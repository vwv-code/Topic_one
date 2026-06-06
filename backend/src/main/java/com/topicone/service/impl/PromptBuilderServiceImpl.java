package com.topicone.service.impl;

import com.topicone.entity.Conversation;
import com.topicone.entity.ConversationSceneConfig;
import com.topicone.entity.Scene;
import com.topicone.mapper.ConversationMapper;
import com.topicone.mapper.SceneMapper;
import com.topicone.service.ConversationSceneConfigService;
import com.topicone.service.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 提示词构建实现
 *
 * 组成结构：
 * 1. 当前场景名称（从 scenes 表读取）
 * 2. 角色设定（来自 conversation_scene_config.role_setting）
 * 3. 场景描述（来自 conversation_scene_config.description）
 * 4. 交互规则（硬编码通用规则）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptBuilderServiceImpl implements PromptBuilderService {

    private final ConversationSceneConfigService configService;
    private final ConversationMapper conversationMapper;
    private final SceneMapper sceneMapper;

    @Override
    public String buildSystemPrompt(Long conversationId) {
        StringBuilder prompt = new StringBuilder();

        // 1. 当前场景名称（根据 conversationId → sceneId → scenes.scene_name）
        String sceneName = resolveSceneName(conversationId);
        if (sceneName != null && !sceneName.isBlank()) {
            prompt.append("当前场景是").append(sceneName).append("\n\n");
        }

        // 2. 读取会话级场景配置
        ConversationSceneConfig config = configService.getConfig(conversationId);

        if (config != null) {
            // 角色设定
            if (config.getRoleSetting() != null && !config.getRoleSetting().isBlank()) {
                prompt.append("# 你的角色\n").append(config.getRoleSetting()).append("\n\n");
            }
            // 场景描述
            if (config.getDescription() != null && !config.getDescription().isBlank()) {
                prompt.append("# 对话场景\n").append(config.getDescription()).append("\n\n");
            }
        }

        // 3. 添加交互规则
        prompt.append("# 交互规则\n")
              .append("- 你是一位专业的英语口语陪练伙伴。\n")
              .append("- 始终用英语回复，语言自然、地道。\n")
              .append("- 根据用户的英语水平调整回复的复杂度。\n")
              .append("- 如果用户表达有语法或用词问题，可以温和地给出建议。\n")
              .append("- 保持对话流畅，主动引导话题延伸。\n")
              .append("- 每次回复控制在 2-4 句话，适合口语练习节奏。\n")
              .append("- 回答的英文单词的字母总数不能超过300个。\n")
              .append("- 回复中只能包含英文单词和标点符号，不得出现任何特殊字符、表情符号、代码块或其他非文本内容。\n\n");

        log.info("为会话 {} 构建提示词完成，场景: {}, 长度: {}", conversationId, sceneName, prompt.length());
        return prompt.toString();
    }

    /**
     * 根据 conversationId 解析场景名称
     * conversationId → user_conversation.scene_id → scenes.scene_name
     */
    private String resolveSceneName(Long conversationId) {
        try {
            Conversation conversation = conversationMapper.selectByConversationId(conversationId);
            if (conversation != null && conversation.getSceneId() != null) {
                Scene scene = sceneMapper.selectBySceneId(conversation.getSceneId());
                if (scene != null && scene.getSceneName() != null) {
                    return scene.getSceneName();
                }
            }
        } catch (Exception e) {
            log.warn("解析场景名称失败, conversationId={}", conversationId, e);
        }
        return null;
    }
}
