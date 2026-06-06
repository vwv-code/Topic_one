package com.topicone.service.impl;

import com.topicone.entity.Message;
import com.topicone.mapper.MessageMapper;
import com.topicone.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messageMapper.selectByConversationId(conversationId);
    }

    @Override
    public Message saveMessage(Message message) {
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    @Override
    public Message saveUserMessage(Long conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        return saveMessage(message);
    }

    @Override
    public Message saveAssistantMessage(Long conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(content);
        return saveMessage(message);
    }
}
