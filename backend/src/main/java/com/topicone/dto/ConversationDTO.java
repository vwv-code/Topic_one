package com.topicone.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话 DTO（返回给前端）
 */
@Data
public class ConversationDTO {

    private Long conversationId;
    private Long userId;
    private Long sceneId;
    private String title;
    private LocalDateTime createTime;
}
