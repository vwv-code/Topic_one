package com.topicone.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicone.dto.DailySummaryResponse;
import com.topicone.entity.DailySummary;
import com.topicone.entity.PronunciationEvaluation;
import com.topicone.mapper.DailySummaryMapper;
import com.topicone.mapper.PronunciationEvaluationMapper;
import com.topicone.service.DailySummaryService;
import com.topicone.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySummaryServiceImpl implements DailySummaryService {

    private final PronunciationEvaluationMapper pronunciationEvaluationMapper;
    private final DailySummaryMapper dailySummaryMapper;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    public DailySummaryResponse getTodaySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 1. 查询当天的评测记录
        List<PronunciationEvaluation> evaluations = pronunciationEvaluationMapper
                .selectTodayByUserId(userId, startOfDay, endOfDay);

        // 2. 查询当天是否已有总结
        DailySummary existingSummary = dailySummaryMapper.selectByUserIdAndDate(userId, today);

        // 3. 计算统计数据
        double avgOverall = 0, avgAccuracy = 0, avgFluency = 0, avgIntegrity = 0;
        int count = evaluations.size();
        if (count > 0) {
            avgOverall = evaluations.stream().mapToDouble(e -> nvl(e.getOverallScore())).average().orElse(0);
            avgAccuracy = evaluations.stream().mapToDouble(e -> nvl(e.getAccuracyScore())).average().orElse(0);
            avgFluency = evaluations.stream().mapToDouble(e -> nvl(e.getFluencyScore())).average().orElse(0);
            avgIntegrity = evaluations.stream().mapToDouble(e -> nvl(e.getIntegrityScore())).average().orElse(0);
        }

        // 4. 构建详情列表
        List<DailySummaryResponse.EvaluationDetail> details = evaluations.stream()
                .map(e -> DailySummaryResponse.EvaluationDetail.builder()
                        .refText(e.getRefText())
                        .overallScore(e.getOverallScore())
                        .accuracyScore(e.getAccuracyScore())
                        .fluencyScore(e.getFluencyScore())
                        .integrityScore(e.getIntegrityScore())
                        .build())
                .collect(Collectors.toList());

        // 5. 如果已有当天总结，直接返回缓存结果
        if (existingSummary != null && existingSummary.getSummaryContent() != null) {
            log.info("[每日总结] 返回缓存: userId={}, date={}", userId, today);
            return DailySummaryResponse.builder()
                    .summaryDate(today.toString())
                    .evalCount(count)
                    .avgOverallScore(round(avgOverall))
                    .avgAccuracyScore(round(avgAccuracy))
                    .avgFluencyScore(round(avgFluency))
                    .avgIntegrityScore(round(avgIntegrity))
                    .summaryContent(existingSummary.getSummaryContent())
                    .details(details)
                    .build();
        }

        // 6. 如果当天没有评测数据
        if (count == 0) {
            return DailySummaryResponse.builder()
                    .summaryDate(today.toString())
                    .evalCount(0)
                    .avgOverallScore(0.0)
                    .avgAccuracyScore(0.0)
                    .avgFluencyScore(0.0)
                    .avgIntegrityScore(0.0)
                    .summaryContent("今天还没有口语练习记录，快去练一练吧！")
                    .details(Collections.emptyList())
                    .build();
        }

        // 7. 构建 LLM 提示词，生成总结
        String summaryContent = generateSummaryWithLlm(userId, evaluations, avgOverall, avgAccuracy, avgFluency, avgIntegrity);

        // 8. 存储到数据库
        DailySummary ds = new DailySummary();
        ds.setUserId(userId);
        ds.setSummaryDate(today);
        ds.setEvalCount(count);
        ds.setAvgOverallScore(round(avgOverall));
        ds.setAvgAccuracyScore(round(avgAccuracy));
        ds.setAvgFluencyScore(round(avgFluency));
        ds.setAvgIntegrityScore(round(avgIntegrity));
        ds.setSummaryContent(summaryContent);
        dailySummaryMapper.insert(ds);

        return DailySummaryResponse.builder()
                .summaryDate(today.toString())
                .evalCount(count)
                .avgOverallScore(round(avgOverall))
                .avgAccuracyScore(round(avgAccuracy))
                .avgFluencyScore(round(avgFluency))
                .avgIntegrityScore(round(avgIntegrity))
                .summaryContent(summaryContent)
                .details(details)
                .build();
    }

    /**
     * 调用 LLM 生成每日口语总结评语
     */
    private String generateSummaryWithLlm(Long userId, List<PronunciationEvaluation> evaluations,
                                           double avgOverall, double avgAccuracy,
                                           double avgFluency, double avgIntegrity) {
        // 构建评分数据文本
        StringBuilder scoreData = new StringBuilder();
        scoreData.append("今天用户练习了 ").append(evaluations.size()).append(" 句口语，以下是评测数据：\n\n");
        scoreData.append("【综合统计】\n");
        scoreData.append("- 综合平均分: ").append(String.format("%.1f", avgOverall)).append("/100\n");
        scoreData.append("- 发音准确度: ").append(String.format("%.1f", avgAccuracy)).append("/100\n");
        scoreData.append("- 流利度: ").append(String.format("%.1f", avgFluency)).append("/100\n");
        scoreData.append("- 完整度: ").append(String.format("%.1f", avgIntegrity)).append("/100\n\n");
        scoreData.append("【逐句详情】\n");
        for (int i = 0; i < evaluations.size(); i++) {
            PronunciationEvaluation e = evaluations.get(i);
            scoreData.append("第").append(i + 1).append("句: \"").append(e.getRefText()).append("\"\n");
            scoreData.append("  综合: ").append(e.getOverallScore())
                    .append(" | 准确度: ").append(e.getAccuracyScore())
                    .append(" | 流利度: ").append(e.getFluencyScore())
                    .append(" | 完整度: ").append(e.getIntegrityScore()).append("\n");
        }

        String systemPrompt = "你是一位专业的英语口语评测老师，你的任务是根据用户今天的口语练习数据生成一段总结评语。\n\n"
                + "# 要求\n"
                + "1. 用中文回复，语气亲切鼓励\n"
                + "2. 简要概括今天的整体表现\n"
                + "3. 指出1-2个优点和1-2个需要改进的地方\n"
                + "4. 给出具体的练习建议\n"
                + "5. 控制在150字以内\n"
                + "6. 回复格式要适合直接展示给用户阅读";

        String userMessage = "请根据以下数据生成今天的口语练习总结：\n\n" + scoreData.toString();

        try {
            // 同步调用 LLM
            StringBuilder result = new StringBuilder();
            final Object lock = new Object();
            llmService.chatStream(systemPrompt, userMessage, Collections.emptyList(),
                    new LlmService.LlmStreamListener() {
                        @Override
                        public void onChunk(String chunk) {
                            result.append(chunk);
                        }

                        @Override
                        public void onComplete(String fullText) {
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            log.error("[每日总结] LLM 调用失败: {}", error);
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                        }
                    });

            // 等待 LLM 完成（最多等待 30 秒）
            synchronized (lock) {
                lock.wait(30000);
            }

            String content = result.toString().trim();
            if (content.isEmpty()) {
                return generateFallbackSummary(avgOverall, avgAccuracy, avgFluency, avgIntegrity, evaluations.size());
            }
            return content;

        } catch (Exception e) {
            log.error("[每日总结] LLM 调用异常", e);
            return generateFallbackSummary(avgOverall, avgAccuracy, avgFluency, avgIntegrity, evaluations.size());
        }
    }

    /**
     * LLM 失败时的兜底总结
     */
    private String generateFallbackSummary(double avgOverall, double avgAccuracy,
                                            double avgFluency, double avgIntegrity, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("今天你一共练习了 ").append(count).append(" 句口语，");

        if (avgOverall >= 80) {
            sb.append("表现非常出色！发音准确度和流利度都很好，继续保持这个状态。");
        } else if (avgOverall >= 60) {
            sb.append("表现不错！建议多注意发音的准确性，可以尝试放慢语速来提升清晰度。");
        } else {
            sb.append("还有进步空间哦。建议从简单的句子开始，重点练习单词的发音，多加练习会越来越好的！");
        }

        sb.append("\n\n【得分详情】");
        sb.append("\n- 综合: ").append(String.format("%.1f", avgOverall)).append("分");
        sb.append("\n- 准确度: ").append(String.format("%.1f", avgAccuracy)).append("分");
        sb.append("\n- 流利度: ").append(String.format("%.1f", avgFluency)).append("分");
        sb.append("\n- 完整度: ").append(String.format("%.1f", avgIntegrity)).append("分");

        return sb.toString();
    }

    private double nvl(Double val) {
        return val == null ? 0 : val;
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
