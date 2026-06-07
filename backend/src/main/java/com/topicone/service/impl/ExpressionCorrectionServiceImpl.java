package com.topicone.service.impl;

import com.topicone.dto.pronunciation.ExpressionCorrectionResult;
import com.topicone.entity.ExpressionCorrection;
import com.topicone.mapper.ExpressionCorrectionMapper;
import com.topicone.service.ExpressionCorrectionService;
import com.topicone.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionCorrectionServiceImpl implements ExpressionCorrectionService {

    private final LlmService llmService;
    private final ExpressionCorrectionMapper expressionCorrectionMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final String SYSTEM_PROMPT = """
            你是一位专业的英语语法和表达老师。你的任务是检查非英语母语者的英文句子，给出纠错和润色建议。

            # 要求
            1. 如果句子没有语法或表达错误，corrected_text 可以与 original_text 相同，suggestion 可以简短肯定
            2. 如果句子有错误，请指出问题并给出正确的表达方式
            3. suggestion 必须用中文写，简洁明了地说明问题所在
            4. corrected_text 必须是正确的英文句子
            5. 回复格式必须是严格的 JSON，不要包含任何其他内容：
            {
              "corrected_text": "正确的英文句子",
              "suggestion": "中文纠错说明"
            }
            
            # 示例
            用户输入: "I go to store yesterday"
            回复:
            {
              "corrected_text": "I went to the store yesterday",
              "suggestion": "时间状语 yesterday 表示过去，动词 go 应改为过去式 went；store 前需加定冠词 the"
            }
            
            用户输入: "The weather is nice today"
            回复:
            {
              "corrected_text": "The weather is nice today",
              "suggestion": "句子表达正确，没有问题"
            }
            """;

    @Override
    public void correctBatch(Long userId, Long conversationId,
                             List<String> sentences,
                             Consumer<ExpressionCorrectionResult> onResult,
                             Consumer<List<ExpressionCorrectionResult>> onComplete,
                             Consumer<String> onError) {
        if (sentences == null || sentences.isEmpty()) {
            onComplete.accept(Collections.emptyList());
            return;
        }

        List<ExpressionCorrectionResult> results = new ArrayList<>();

        // 逐句处理（顺序执行，保持结果顺序）
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < sentences.size(); i++) {
                String sentence = sentences.get(i);
                int sentenceIndex = i;
                try {
                    ExpressionCorrectionResult result = correctOne(
                            userId, conversationId, sentenceIndex, sentence);
                    results.add(result);
                    onResult.accept(result);
                } catch (Exception e) {
                    log.error("[表达纠错] 第{}句纠错失败: '{}'", i, sentence, e);
                    // 失败时返回原文 + 错误说明
                    ExpressionCorrectionResult errorResult = ExpressionCorrectionResult.builder()
                            .sentenceIndex(sentenceIndex)
                            .originalText(sentence)
                            .correctedText(sentence)
                            .suggestion("纠错服务暂时不可用")
                            .build();
                    results.add(errorResult);
                    onResult.accept(errorResult);
                }
            }
            onComplete.accept(results);
            log.info("[表达纠错] 全部完成, 共{}句", results.size());
        }, executor).exceptionally(ex -> {
            log.error("[表达纠错] 批量纠错异常", ex);
            onError.accept(ex.getMessage());
            return null;
        });
    }

    /**
     * 对单个句子调用 LLM 进行表达纠错
     */
    private ExpressionCorrectionResult correctOne(Long userId, Long conversationId,
                                                    int sentenceIndex, String originalText) throws Exception {
        CompletableFuture<ExpressionCorrectionResult> future = new CompletableFuture<>();

        llmService.chatStream(SYSTEM_PROMPT, originalText,
                Collections.emptyList(), new LlmService.LlmStreamListener() {
                    private final StringBuilder fullResponse = new StringBuilder();

                    @Override
                    public void onChunk(String chunk) {
                        fullResponse.append(chunk);
                    }

                    @Override
                    public void onComplete(String fullText) {
                        try {
                            ExpressionCorrectionResult result = parseLlmResponse(
                                    sentenceIndex, originalText, fullText);
                            // 存入数据库
                            saveToDb(userId, conversationId, result);
                            future.complete(result);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        future.completeExceptionally(new RuntimeException(error));
                    }
                });

        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * 解析 LLM 返回的 JSON
     */
    private ExpressionCorrectionResult parseLlmResponse(int sentenceIndex, String originalText,
                                                          String llmResponse) {
        String correctedText = originalText;
        String suggestion = "";

        try {
            // 尝试提取 JSON（LLM 可能包裹在 markdown 代码块中）
            String json = llmResponse.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                }
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);

            if (node.has("corrected_text")) {
                correctedText = node.get("corrected_text").asText();
            }
            if (node.has("suggestion")) {
                suggestion = node.get("suggestion").asText();
            }
        } catch (Exception e) {
            log.warn("[表达纠错] LLM 返回格式解析失败，使用原文: '{}'", llmResponse);
            suggestion = "LLM 返回格式异常，未能生成纠错建议";
        }

        return ExpressionCorrectionResult.builder()
                .sentenceIndex(sentenceIndex)
                .originalText(originalText)
                .correctedText(correctedText)
                .suggestion(suggestion)
                .build();
    }

    /**
     * 存储纠错记录到数据库
     */
    private void saveToDb(Long userId, Long conversationId, ExpressionCorrectionResult result) {
        try {
            ExpressionCorrection ec = new ExpressionCorrection();
            ec.setUserId(userId);
            ec.setConversationId(conversationId);
            ec.setSentenceIndex(result.getSentenceIndex());
            ec.setOriginalText(result.getOriginalText());
            ec.setCorrectedText(result.getCorrectedText());
            ec.setSuggestion(result.getSuggestion());
            ec.setCreateTime(LocalDateTime.now());
            expressionCorrectionMapper.insert(ec);
        } catch (Exception e) {
            log.error("[表达纠错] 存储失败: conversationId={}", conversationId, e);
        }
    }
}
