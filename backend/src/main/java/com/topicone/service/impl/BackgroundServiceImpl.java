package com.topicone.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicone.dto.BackgroundResponse;
import com.topicone.entity.Conversation;
import com.topicone.entity.ConversationBackground;
import com.topicone.entity.ConversationSceneConfig;
import com.topicone.mapper.ConversationBackgroundMapper;
import com.topicone.mapper.ConversationMapper;
import com.topicone.service.BackgroundService;
import com.topicone.service.ConversationSceneConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BackgroundServiceImpl implements BackgroundService {

    private final ConversationBackgroundMapper backgroundMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationSceneConfigService configService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String model;

    public BackgroundServiceImpl(ConversationBackgroundMapper backgroundMapper,
                                  ConversationMapper conversationMapper,
                                  ConversationSceneConfigService configService,
                                  ObjectMapper objectMapper,
                                  @Value("${ai.dashscope.api-key:}") String apiKey,
                                  @Value("${ai.image.model:qwen-image-plus}") String model) {
        this.backgroundMapper = backgroundMapper;
        this.conversationMapper = conversationMapper;
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public BackgroundResponse getOrGenerateBackground(Long userId, Long conversationId) {
        // 1. 查数据库缓存
        ConversationBackground existing = backgroundMapper.selectByConversationId(conversationId);
        if (existing != null) {
            log.info("[背景图] 命中缓存: conversationId={}", conversationId);
            return BackgroundResponse.builder()
                    .hasImage(true)
                    .imageUrl(existing.getImageUrl())
                    .generating(false)
                    .build();
        }

        // 2. 获取会话的场景描述和标题
        Conversation conv = conversationMapper.selectByConversationId(conversationId);
        if (conv == null) {
            return BackgroundResponse.builder()
                    .hasImage(false)
                    .imageUrl(null)
                    .generating(false)
                    .build();
        }

        ConversationSceneConfig config = configService.getConfig(conversationId);
        String sceneDescription = (config != null && config.getDescription() != null)
                ? config.getDescription() : "英语口语练习场景";
        String title = (conv.getTitle() != null) ? conv.getTitle() : "英语对话";

        // 3. 构建文生图提示词
        String prompt = buildImagePrompt(title, sceneDescription);

        // 4. 调用通义文生图 API
        log.info("[背景图] 开始生成: conversationId={}, title={}", conversationId, title);
        String imageUrl = callImageGenerationApi(prompt);

        if (imageUrl == null) {
            return BackgroundResponse.builder()
                    .hasImage(false)
                    .imageUrl(null)
                    .generating(false)
                    .build();
        }

        // 5. 存数据库
        ConversationBackground bg = new ConversationBackground();
        bg.setUserId(userId);
        bg.setConversationId(conversationId);
        bg.setSceneDescription(sceneDescription);
        bg.setPrompt(prompt);
        bg.setImageUrl(imageUrl);
        bg.setCreateTime(LocalDateTime.now());
        backgroundMapper.insert(bg);

        log.info("[背景图] 生成并保存成功: conversationId={}", conversationId);
        return BackgroundResponse.builder()
                .hasImage(true)
                .imageUrl(imageUrl)
                .generating(false)
                .build();
    }

    /** 构建文生图提示词 */
    private String buildImagePrompt(String title, String description) {
        return String.format(
                "A beautiful, atmospheric digital illustration for an English conversation practice scene: %s. %s. "
                + "Soft lighting, cinematic composition, warm and inviting mood, "
                + "clean and minimalist style, suitable for a language learning app background. "
                + "No text, no words, no letters in the image.",
                title, description);
    }

    /** 调用通义文生图 API */
    private String callImageGenerationApi(String prompt) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);

            Map<String, Object> input = new LinkedHashMap<>();
            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("text", prompt);
            userMsg.put("content", List.of(textContent));
            input.put("messages", List.of(userMsg));
            body.put("input", input);

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("size", "1664*928");
            parameters.put("n", 1);
            parameters.put("watermark", false);
            parameters.put("prompt_extend", true);
            parameters.put("negative_prompt", "low quality, blurry, text, watermark, signature, logo");
            body.put("parameters", parameters);

            Request request = new Request.Builder()
                    .url("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(body),
                            MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("[背景图] API 返回错误: {} {}", response.code(), responseBody);
                    // 尝试下载图片（生成的 URL 直接返回了图片二进制）
                    // 检查 content-type
                    String contentType = response.header("Content-Type", "");
                    if (contentType.startsWith("image/")) {
                        // 同步接口有时直接返回图片而不是 JSON
                        // 这种情况下我们重新发起请求来获取 JSON
                        return null;
                    }
                    return null;
                }

                JsonNode json = objectMapper.readTree(responseBody);
                JsonNode choices = json.path("output").path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode content = choices.get(0).path("message").path("content");
                    if (content.isArray() && content.size() > 0) {
                        String imageUrl = content.get(0).path("image").asText();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            return imageUrl;
                        }
                    }
                }

                log.warn("[背景图] 未能从响应中提取图片URL: {}", responseBody.substring(0, Math.min(300, responseBody.length())));
                return null;
            }
        } catch (Exception e) {
            log.error("[背景图] 调用文生图 API 异常", e);
            return null;
        }
    }
}
