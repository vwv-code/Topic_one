package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.config.AuthInterceptor;
import com.topicone.dto.BackgroundResponse;
import com.topicone.service.BackgroundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/background")
@RequiredArgsConstructor
public class BackgroundController {

    private final BackgroundService backgroundService;

    /**
     * 获取或生成会话背景图
     */
    @GetMapping
    public Result<BackgroundResponse> getBackground(@RequestParam Long conversationId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        log.info("[背景图] 请求: userId={}, conversationId={}", userId, conversationId);
        BackgroundResponse response = backgroundService.getOrGenerateBackground(userId, conversationId);
        return Result.success(response);
    }
}
