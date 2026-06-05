package com.topicone.service;

import com.topicone.dto.ConversationDTO;
import com.topicone.dto.CreateConversationRequest;

import java.util.List;

public interface ConversationService {

    /**
     * 获取用户的会话列表
     */
    List<ConversationDTO> getConversationList(Long userId);

    /**
     * 创建新会话
     */
    ConversationDTO createConversation(CreateConversationRequest request);

    /**
     * 删除会话（逻辑删除）
     */
    void deleteConversation(Long conversationId);

    /**
     * 按 ID 查询单个会话
     */
    ConversationDTO getConversationById(Long conversationId);

    /**
     * 更新会话标题
     */
    void updateTitle(Long conversationId, String title);
}
