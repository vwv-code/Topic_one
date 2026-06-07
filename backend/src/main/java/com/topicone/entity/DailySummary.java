package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日口语总结实体
 */
@Data
@TableName("daily_summary")
public class DailySummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 总结日期 */
    private LocalDate summaryDate;

    /** 当天评测句子数 */
    private Integer evalCount;

    /** 当天综合平均分 */
    private Double avgOverallScore;

    /** 当天准确度平均分 */
    private Double avgAccuracyScore;

    /** 当天流利度平均分 */
    private Double avgFluencyScore;

    /** 当天完整度平均分 */
    private Double avgIntegrityScore;

    /** LLM 生成的总结评语 */
    private String summaryContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
