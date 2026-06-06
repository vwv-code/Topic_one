package com.topicone.service.llm;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM 服务实现 - 通义千问（DashScope SDK）
 *
 * 使用 DashScope SDK 调用通义千问大模型，支持流式输出。
 * 模型默认使用 qwen-plus，可在 application.yml 中配置。
 */
@Slf4j
@Service
public class QwenLlmService implements LlmService {

    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:qwen-plus}")
    private String model;

    private Generation generation;

    @PostConstruct
    public void init() {
        this.generation = new Generation();
        log.info("QwenLlmService 初始化完成, model={}", model);
    }

    @Override
    public void chatStream(String systemPrompt, String userMessage,
                           List<Map<String, String>> history, LlmStreamListener listener) {
        try {
            // 构建消息列表
            List<Message> messages = new ArrayList<>();

            // System prompt
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Message.builder().role("system").content(systemPrompt).build());
                log.info("[LLM] System Prompt (长度={}):\n{}", systemPrompt.length(), systemPrompt);
            } else {
                log.warn("[LLM] System Prompt 为空！LLM 将不会收到任何角色设定。");
            }

            // 历史对话
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    String role = msg.getOrDefault("role", "user");
                    if ("system".equals(role)) continue;
                    String dashScopeRole = "assistant".equals(role) ? "assistant" : "user";
                    messages.add(Message.builder().role(dashScopeRole).content(msg.get("content")).build());
                }
                log.info("[LLM] 历史消息: {} 条", history.size());
            }

            // 当前用户消息
            messages.add(Message.builder().role("user").content(userMessage).build());
            log.info("[LLM] 用户输入: '{}'", userMessage);

            log.info("[LLM] 共发送 {} 条消息给 {}(第1条role={})",
                    messages.size(), model,
                    messages.isEmpty() ? "N/A" : messages.get(0).getRole());

            // 构建请求参数（多维度约束模型输出）
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(0.3F)        // 低发散度 → 严格遵循 system prompt
                    .maxTokens(150)            // ★ 硬限制：最多 150 tokens（约 300 个英文字母）
                    .seed(42)                  // 固定种子 → 输出可复现、更确定
                    .repetitionPenalty(1.1F)   // 轻度惩罚重复 → 避免车轱辘话
                    .enableSearch(false)       // 禁止联网搜索，只按 system prompt 说话
                    .build();

            log.info("[LLM] 请求参数: model={}, temperature={}, maxTokens={}, seed={}, repetitionPenalty={}, enableSearch={}",
                    param.getModel(), param.getTemperature(), param.getMaxTokens(),
                    param.getSeed(), param.getRepetitionPenalty(), param.getEnableSearch());
            log.info("[LLM] Parameters map: {}", param.getParameters());

            // 流式调用
            Flowable<GenerationResult> flowable = generation.streamCall(param);
            StringBuilder fullText = new StringBuilder();

            flowable.subscribe(
                    result -> {
                        if (result != null && result.getOutput() != null
                                && result.getOutput().getChoices() != null
                                && !result.getOutput().getChoices().isEmpty()) {
                            // ★ DashScope 流式返回的是累积全文，不是增量！
                            //    需要做差量计算：只取新增部分发给前端
                            String accumulatedText = result.getOutput().getChoices().get(0).getMessage().getContent();
                            if (accumulatedText != null && !accumulatedText.isEmpty()) {
                                int previousLen = fullText.length();
                                if (accumulatedText.length() > previousLen) {
                                    String delta = accumulatedText.substring(previousLen);
                                    fullText.append(delta);
                                    listener.onChunk(delta);
                                }
                            }
                        }
                    },
                    error -> {
                        log.error("LLM 流式调用出错", error);
                        listener.onError(error.getMessage());
                    },
                    () -> {
                        String complete = fullText.toString();
                        log.info("[LLM] 流式完成: 共 {} 字符, 完整内容: '{}'",
                                complete.length(), complete);
                        listener.onComplete(complete);
                    }
            );

        } catch (NoApiKeyException e) {
            log.error("DashScope API Key 未配置", e);
            listener.onError("API Key 未配置");
        } catch (InputRequiredException | ApiException e) {
            log.error("LLM 调用异常", e);
            listener.onError(e.getMessage());
        } catch (Exception e) {
            log.error("LLM 调用未知异常", e);
            listener.onError(e.getMessage());
        }
    }
}
