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
}
