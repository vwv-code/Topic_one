package com.topicone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成长记录响应（全部日期的每日总结数据，用于可视化展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthRecordResponse {

    /** 用户总练习天数 */
    private Integer totalDays;

    /** 用户总练习句数 */
    private Integer totalSentences;

    /** 每日数据点列表 */
    private List<DataPoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /** 日期 */
        private String date;
        /** 当天练习句数 */
        private Integer evalCount;
        /** 综合平均分 */
        private Double avgOverallScore;
        /** 准确度平均分 */
        private Double avgAccuracyScore;
        /** 流利度平均分 */
        private Double avgFluencyScore;
        /** 完整度平均分 */
        private Double avgIntegrityScore;
    }
}
