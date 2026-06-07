package com.topicone.dto.pronunciation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发音评测结果（数值评分）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationResult {

    /** 参考文本 */
    private String refText;
    /** 综合得分 (0-100) */
    private Double overallScore;
    /** 发音准确度得分 */
    private Double accuracyScore;
    /** 流利度得分 */
    private Double fluencyScore;
    /** 完整度得分 */
    private Double integrityScore;
    /** 用户录音时长（毫秒） */
    private Integer audioDuration;
    /** 句子级别详情 */
    private SentenceDetail sentenceDetail;
    /** 单词级别详情 */
    private List<WordDetail> wordDetails;

    // ========== 内部类 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentenceDetail {
        private Double score;
        private Integer stressScore;
        private Integer toneScore;
        private Integer senseScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordDetail {
        private String word;
        private Double score;
        private Integer startMs;
        private Integer endMs;
        private List<PhonemeDetail> phonemes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhonemeDetail {
        private String phoneme;
        private Double score;
        private Boolean hasError;
    }
}
