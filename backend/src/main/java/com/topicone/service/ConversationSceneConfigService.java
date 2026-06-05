package com.topicone.service;

import com.topicone.entity.ConversationSceneConfig;

public interface ConversationSceneConfigService {

    /**
     * 获取会话的场景配置
     */
    ConversationSceneConfig getConfig(Long conversationId);

    /**
     * 创建会话时初始化配置（从 scenes 表拷贝默认值）
     */
    void initConfig(Long conversationId, Long sceneId);

    /**
     * 更新会话的场景配置（描述 + 角色设定）
     */
    void updateConfig(Long conversationId, String description, String roleSetting);
}
