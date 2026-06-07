package com.topicone.service;

import com.topicone.dto.BackgroundResponse;

/**
 * 会话背景图服务（沉浸式体验）
 */
public interface BackgroundService {

    /**
     * 获取或生成会话背景图
     * 先查数据库缓存，没有再调用文生图模型生成
     */
    BackgroundResponse getOrGenerateBackground(Long userId, Long conversationId);
}
