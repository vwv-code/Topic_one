package com.topicone.service;

import com.topicone.dto.DailySummaryResponse;

/**
 * 每日口语总结服务
 */
public interface DailySummaryService {

    /**
     * 获取用户当天的口语总结
     * 如果当天已有总结则直接返回，否则收集数据 → LLM 生成 → 存储 → 返回
     *
     * @param userId 用户ID
     * @return 每日总结（含评分统计 + LLM 评语）
     */
    DailySummaryResponse getTodaySummary(Long userId);
}
