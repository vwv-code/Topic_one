package com.topicone.service;

/**
 * 提示词构建服务
 * 将场景配置（description、role_setting）与对话上下文合并为完整的 System Prompt
 */
public interface PromptBuilderService {

    /**
     * 构建完整提示词
     *
     * @param conversationId 会话ID
     * @return 构建好的 system prompt
     */
    String buildSystemPrompt(Long conversationId);
}
