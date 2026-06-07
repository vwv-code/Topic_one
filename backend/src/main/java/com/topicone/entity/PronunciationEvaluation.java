package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发音评测记录实体（每句用户语音的评测结果）
 */
@Data
@TableName("pronunciation_evaluation")
public class PronunciationEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    /** 参考文本（用户说的话） */
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

    /** 单词级别详情（JSON 字符串） */
    private String wordDetails;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
