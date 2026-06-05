package com.topicone.service.impl;

import com.topicone.dto.SaveSettingsRequest;
import com.topicone.dto.UserSettingsDTO;
import com.topicone.entity.UserSetting;
import com.topicone.mapper.ConversationMapper;
import com.topicone.mapper.SceneMapper;
import com.topicone.mapper.UserSettingMapper;
import com.topicone.service.ConversationSceneConfigService;
import com.topicone.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingMapper userSettingMapper;
    private final SceneMapper sceneMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationSceneConfigService configService;

    @Override
    public UserSettingsDTO getSettings(Long userId) {
        UserSetting setting = userSettingMapper.selectById(userId);
        if (setting == null) {
            // 返回默认值
            UserSettingsDTO dto = new UserSettingsDTO();
            dto.setDifficulty("intermediate");
            dto.setSpeechSpeed(java.math.BigDecimal.valueOf(1.0));
            return dto;
        }
        UserSettingsDTO dto = new UserSettingsDTO();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    @Override
    public void saveSettings(SaveSettingsRequest request) {
        // 1. 保存用户设置
        UserSetting existing = userSettingMapper.selectById(request.getUserId());
        if (existing == null) {
            existing = new UserSetting();
            existing.setId(request.getUserId());
        }

        existing.setCurrentSceneId(request.getCurrentSceneId());
        existing.setDifficulty(request.getDifficulty());
        existing.setSpeechSpeed(request.getSpeechSpeed());

        if (existing.getCreateTime() == null) {
            userSettingMapper.insert(existing);
        } else {
            userSettingMapper.updateById(existing);
        }

        // 2. 场景相关配置
        if (request.getConversationId() != null) {
            // 有激活会话
            // 2a. 更新 user_conversation.scene_id
            if (request.getSceneId() != null) {
                conversationMapper.updateSceneId(request.getConversationId(), request.getSceneId());
            }
            // 2b. 更新 conversation_scene_config 的描述和角色设定
            String desc = request.getDescription() != null ? request.getDescription() : "";
            String role = request.getRoleSetting() != null ? request.getRoleSetting() : "";
            configService.updateConfig(request.getConversationId(), desc, role);
        } else {
            // 无激活会话 → 更新 scenes 表作为默认模板
            String desc = request.getDescription() != null ? request.getDescription() : "";
            String role = request.getRoleSetting() != null ? request.getRoleSetting() : "";
            sceneMapper.updateBySceneId(request.getSceneId(), desc, role);
        }
    }
}
