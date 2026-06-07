package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.PronunciationEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PronunciationEvaluationMapper extends BaseMapper<PronunciationEvaluation> {

    /**
     * 查询某用户当天的全部评测记录
     */
    @Select("SELECT * FROM pronunciation_evaluation WHERE user_id = #{userId} AND create_time >= #{startOfDay} AND create_time < #{endOfDay} ORDER BY create_time ASC")
    List<PronunciationEvaluation> selectTodayByUserId(@Param("userId") Long userId,
                                                       @Param("startOfDay") LocalDateTime startOfDay,
                                                       @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 查询某用户当天的评测统计（平均值）
     */
    @Select("SELECT COUNT(*) AS evalCount, " +
            "COALESCE(AVG(overall_score), 0) AS avgOverallScore, " +
            "COALESCE(AVG(accuracy_score), 0) AS avgAccuracyScore, " +
            "COALESCE(AVG(fluency_score), 0) AS avgFluencyScore, " +
            "COALESCE(AVG(integrity_score), 0) AS avgIntegrityScore " +
            "FROM pronunciation_evaluation " +
            "WHERE user_id = #{userId} AND create_time >= #{startOfDay} AND create_time < #{endOfDay}")
    TodayStats selectTodayStats(@Param("userId") Long userId,
                                @Param("startOfDay") LocalDateTime startOfDay,
                                @Param("endOfDay") LocalDateTime endOfDay);

    /** 聚合查询结果 */
    class TodayStats {
        private Integer evalCount;
        private Double avgOverallScore;
        private Double avgAccuracyScore;
        private Double avgFluencyScore;
        private Double avgIntegrityScore;

        public Integer getEvalCount() { return evalCount; }
        public void setEvalCount(Integer evalCount) { this.evalCount = evalCount; }
        public Double getAvgOverallScore() { return avgOverallScore; }
        public void setAvgOverallScore(Double avgOverallScore) { this.avgOverallScore = avgOverallScore; }
        public Double getAvgAccuracyScore() { return avgAccuracyScore; }
        public void setAvgAccuracyScore(Double avgAccuracyScore) { this.avgAccuracyScore = avgAccuracyScore; }
        public Double getAvgFluencyScore() { return avgFluencyScore; }
        public void setAvgFluencyScore(Double avgFluencyScore) { this.avgFluencyScore = avgFluencyScore; }
        public Double getAvgIntegrityScore() { return avgIntegrityScore; }
        public void setAvgIntegrityScore(Double avgIntegrityScore) { this.avgIntegrityScore = avgIntegrityScore; }
    }
}
