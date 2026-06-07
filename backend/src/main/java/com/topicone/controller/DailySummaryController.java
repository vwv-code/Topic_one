package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.DailySummaryResponse;
import com.topicone.dto.GrowthRecordResponse;
import com.topicone.service.DailySummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/daily-summary")
@RequiredArgsConstructor
public class DailySummaryController {

    private final DailySummaryService dailySummaryService;

    /**
     * 获取用户当天的口语练习总结
     */
    @GetMapping
    public Result<DailySummaryResponse> getTodaySummary(@RequestParam Long userId) {
        log.info("[每日总结] 查询: userId={}", userId);
        DailySummaryResponse response = dailySummaryService.getTodaySummary(userId);
        return Result.success(response);
    }

    /**
     * 获取用户的成长记录（全部日期的数据，用于可视化）
     */
    @GetMapping("/history")
    public Result<GrowthRecordResponse> getGrowthRecord(@RequestParam Long userId) {
        log.info("[成长记录] 查询: userId={}", userId);
        GrowthRecordResponse response = dailySummaryService.getGrowthRecord(userId);
        return Result.success(response);
    }
}
