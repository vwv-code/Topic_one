package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.DailySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailySummaryMapper extends BaseMapper<DailySummary> {

    /**
     * 查询用户指定日期的总结
     */
    @Select("SELECT * FROM daily_summary WHERE user_id = #{userId} AND summary_date = #{date}")
    DailySummary selectByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询用户全部日期的总结（按日期升序）
     */
    @Select("SELECT * FROM daily_summary WHERE user_id = #{userId} ORDER BY summary_date ASC")
    List<DailySummary> selectAllByUserId(@Param("userId") Long userId);
}
