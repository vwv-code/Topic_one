package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.ExpressionCorrection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExpressionCorrectionMapper extends BaseMapper<ExpressionCorrection> {

    /**
     * 查询某用户当天的全部纠错记录
     */
    @Select("SELECT * FROM expression_correction WHERE user_id = #{userId} AND create_time >= #{startOfDay} AND create_time < #{endOfDay} ORDER BY create_time ASC")
    List<ExpressionCorrection> selectTodayByUserId(@Param("userId") Long userId,
                                                    @Param("startOfDay") LocalDateTime startOfDay,
                                                    @Param("endOfDay") LocalDateTime endOfDay);
}
