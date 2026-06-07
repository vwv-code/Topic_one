package com.topicone.dto.pronunciation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表达纠错结果（WebSocket 推送用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionCorrectionResult {

    /** 句子序号（本会话内，从0开始） */
    private Integer sentenceIndex;

    /** 用户原始英文句子 */
    private String originalText;

    /** LLM 纠错后的句子 */
    private String correctedText;

    /** LLM 纠错建议/说明（中文） */
    private String suggestion;
}
