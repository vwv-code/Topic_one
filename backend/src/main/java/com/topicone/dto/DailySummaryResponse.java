package com.topicone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 每日口语总结响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryResponse {

    /** 总结日期 */
    private String summaryDate;

    /** 当天评测句子总数 */
    private Integer evalCount;

    /** 综合平均分 */
    private Double avgOverallScore;

    /** 准确度平均分 */
    private Double avgAccuracyScore;

    /** 流利度平均分 */
    private Double avgFluencyScore;

    /** 完整度平均分 */
    private Double avgIntegrityScore;

    /** LLM 生成的总结评语 */
    private String summaryContent;

    /** 逐句评测详情（发音评测） */
    private List<EvaluationDetail> details;

    /** 逐句纠错详情（表达纠错） */
    private List<CorrectionDetail> correctionDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationDetail {
        private String refText;
        private Double overallScore;
        private Double accuracyScore;
        private Double fluencyScore;
        private Double integrityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrectionDetail {
        /** 用户原始英文句子 */
        private String originalText;
        /** LLM 纠错后的句子 */
        private String correctedText;
        /** 纠错建议（中文） */
        private String suggestion;
    }
}
