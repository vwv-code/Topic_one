package com.topicone.service;

import com.topicone.entity.Message;

import java.util.List;

public interface MessageService {

    /**
     * 查询某个会话的全部消息（按时间正序）
     */
    List<Message> getMessagesByConversationId(Long conversationId);

    /**
     * 保存一条消息
     */
    Message saveMessage(Message message);

    /**
     * 保存用户消息
     */
    Message saveUserMessage(Long conversationId, String content);

    /**
     * 保存 AI 回复消息
     */
    Message saveAssistantMessage(Long conversationId, String content);
}
