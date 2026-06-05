package com.topicone.service.impl;

import com.topicone.entity.ConversationSceneConfig;
import com.topicone.entity.Scene;
import com.topicone.mapper.ConversationSceneConfigMapper;
import com.topicone.mapper.SceneMapper;
import com.topicone.service.ConversationSceneConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationSceneConfigServiceImpl implements ConversationSceneConfigService {

    private final ConversationSceneConfigMapper configMapper;
    private final SceneMapper sceneMapper;

    @Override
    public ConversationSceneConfig getConfig(Long conversationId) {
        return configMapper.selectByConversationId(conversationId);
    }

    @Override
    public void initConfig(Long conversationId, Long sceneId) {
        // 从 scenes 表读取默认值作为模板
        Scene scene = sceneMapper.selectBySceneId(sceneId);

        ConversationSceneConfig config = new ConversationSceneConfig();
        config.setConversationId(conversationId);
        config.setDescription(scene != null ? scene.getDescription() : "");
        config.setRoleSetting(scene != null ? scene.getRoleSetting() : "");

        configMapper.insert(config);
    }

    @Override
    public void updateConfig(Long conversationId, String description, String roleSetting) {
        int rows = configMapper.updateByConversationId(conversationId, description, roleSetting);
        if (rows == 0) {
            // 记录不存在则插入（兜底）
            ConversationSceneConfig config = new ConversationSceneConfig();
            config.setConversationId(conversationId);
            config.setDescription(description);
            config.setRoleSetting(roleSetting);
            configMapper.insert(config);
        }
    }
}
