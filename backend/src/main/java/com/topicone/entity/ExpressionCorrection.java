package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表达纠错记录实体（LLM 对每句用户表达进行纠错/润色）
 */
@Data
@TableName("expression_correction")
public class ExpressionCorrection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    /** 句子序号（本会话内，从0开始） */
    private Integer sentenceIndex;

    /** 用户原始英文句子 */
    private String originalText;

    /** LLM 纠错后的句子 */
    private String correctedText;

    /** LLM 纠错建议/说明（中文） */
    private String suggestion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
