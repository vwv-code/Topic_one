package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.SaveSettingsRequest;
import com.topicone.dto.UserSettingsDTO;
import com.topicone.service.UserSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserSettingService userSettingService;

    /**
     * 获取用户设置
     * GET /api/settings?userId=1
     */
    @GetMapping
    public Result<UserSettingsDTO> getSettings(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(userSettingService.getSettings(userId));
    }

    /**
     * 保存用户设置（场景、难度、语速）
     * POST /api/settings
     */
    @PostMapping
    public Result<Void> saveSettings(@Valid @RequestBody SaveSettingsRequest request) {
        userSettingService.saveSettings(request);
        return Result.success(null);
    }
}
