package com.topicone.service.impl;

import com.topicone.entity.Conversation;
import com.topicone.entity.ConversationSceneConfig;
import com.topicone.entity.Scene;
import com.topicone.entity.UserSetting;
import com.topicone.mapper.ConversationMapper;
import com.topicone.mapper.SceneMapper;
import com.topicone.mapper.UserSettingMapper;
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
    private final UserSettingMapper userSettingMapper;

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

        // 2.5 添加难度级别约束
        appendDifficultyRule(prompt, conversationId);

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

    /**
     * 解析用户等级，优先用场景级别，其次用用户设置
     * @return "beginner" | "intermediate" | "advanced" | null
     */
    private String resolveDifficultyLevel(Long conversationId) {
        try {
            Conversation conversation = conversationMapper.selectByConversationId(conversationId);
            if (conversation == null) return null;

            // 优先用场景的 difficulty（1=初级 2=中级 3=高级）
            if (conversation.getSceneId() != null) {
                Scene scene = sceneMapper.selectBySceneId(conversation.getSceneId());
                if (scene != null && scene.getDifficulty() != null) {
                    return mapSceneDifficulty(scene.getDifficulty());
                }
            }

            // 回退到用户设置
            UserSetting setting = userSettingMapper.selectById(conversation.getUserId());
            if (setting != null && setting.getDifficulty() != null) {
                return setting.getDifficulty(); // "beginner" / "intermediate" / "advanced"
            }
        } catch (Exception e) {
            log.warn("解析难度等级失败, conversationId={}", conversationId, e);
        }
        return null;
    }

    private String mapSceneDifficulty(Integer d) {
        return switch (d) {
            case 1 -> "beginner";
            case 3 -> "advanced";
            default -> "intermediate";
        };
    }

    /**
     * 根据难度级别添加约束规则
     */
    private void appendDifficultyRule(StringBuilder prompt, Long conversationId) {
        String level = resolveDifficultyLevel(conversationId);
        if (level == null) {
            log.warn("无法解析难度等级, conversationId={}, 使用默认 intermediate", conversationId);
            level = "intermediate";
        }

        prompt.append("# 对话难度\n");
        prompt.append("当前难度等级: ").append(levelToChinese(level)).append("\n");

        switch (level) {
            case "beginner" -> prompt.append(
                    "- 使用最基础的英语单词和简单句型（如一般现在时、简单陈述句）。\n"
                  + "- 每句话不超过10个单词。\n"
                  + "- 语速要慢，词汇量控制在500词以内。\n"
                  + "- 遇到用户听不懂时，用更简单的词重新解释。\n"
                  + "- 多使用鼓励性语言，建立用户信心。\n"
            );
            case "intermediate" -> prompt.append(
                    "- 使用中等难度的词汇和句型，适当引入复合句。\n"
                  + "- 可以使用一些常用习语和地道表达。\n"
                  + "- 每句话控制在15个单词以内。\n"
                  + "- 可以适度引入新词汇，但要在语境中让用户理解。\n"
            );
            case "advanced" -> prompt.append(
                    "- 使用地道、自然的英语表达，可以包含复杂句型和高阶词汇。\n"
                  + "- 可以使用俚语、习语、隐喻等高级表达方式。\n"
                  + "- 语速可以接近母语者正常语速。\n"
                  + "- 可以就话题进行深度讨论，引导用户表达观点。\n"
                  + "- 适度指出用户的高级语法错误或表达不地道之处。\n"
            );
        }
        prompt.append("\n");
        log.info("为会话 {} 设置难度: {}", conversationId, level);
    }

    private String levelToChinese(String level) {
        return switch (level) {
            case "beginner" -> "初级";
            case "advanced" -> "高级";
            default -> "中级";
        };
    }
}
