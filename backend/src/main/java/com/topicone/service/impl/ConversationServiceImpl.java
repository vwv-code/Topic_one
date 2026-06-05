package com.topicone.service.impl;

import com.topicone.dto.ConversationDTO;
import com.topicone.dto.CreateConversationRequest;
import com.topicone.entity.Conversation;
import com.topicone.mapper.ConversationMapper;
import com.topicone.service.ConversationSceneConfigService;
import com.topicone.service.ConversationService;
import com.topicone.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final UserSettingService userSettingService;
    private final ConversationSceneConfigService configService;

    @Override
    public List<ConversationDTO> getConversationList(Long userId) {
        List<Conversation> list = conversationMapper.selectByUserId(userId);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ConversationDTO createConversation(CreateConversationRequest request) {
        Conversation conv = new Conversation();
        conv.setUserId(request.getUserId());
        // sceneId：传了就用，没传则从用户当前设置中取
        Long sceneId = request.getSceneId();
        if (sceneId == null) {
            sceneId = userSettingService.getSettings(request.getUserId()).getCurrentSceneId();
        }
        conv.setSceneId(sceneId);

        // 标题：传了就用，没传就自动生成
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            conv.setTitle(request.getTitle());
        } else {
            conv.setTitle("新对话 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
        }

        conv.setCreateTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conv);

        // 从 scenes 表拷贝默认描述和角色设定到新表
        configService.initConfig(conv.getConversationId(), sceneId);

        return toDTO(conv);
    }

    @Override
    public void deleteConversation(Long conversationId) {
        int rows = conversationMapper.deleteById(conversationId);
        if (rows == 0) {
            throw new com.topicone.common.exception.BusinessException("会话不存在或已被删除");
        }
    }

    @Override
    public ConversationDTO getConversationById(Long conversationId) {
        Conversation conv = conversationMapper.selectByConversationId(conversationId);
        return conv != null ? toDTO(conv) : null;
    }

    @Override
    public void updateTitle(Long conversationId, String title) {
        int rows = conversationMapper.updateTitle(conversationId, title);
        if (rows == 0) {
            throw new com.topicone.common.exception.BusinessException("会话不存在或已被删除");
        }
    }

    private ConversationDTO toDTO(Conversation entity) {
        ConversationDTO dto = new ConversationDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
