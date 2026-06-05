package com.topicone.service;

import com.topicone.dto.SaveSettingsRequest;
import com.topicone.dto.UserSettingsDTO;

public interface UserSettingService {

    /**
     * 获取用户设置
     */
    UserSettingsDTO getSettings(Long userId);

    /**
     * 保存用户设置（场景、难度、语速）
     */
    void saveSettings(SaveSettingsRequest request);
}
