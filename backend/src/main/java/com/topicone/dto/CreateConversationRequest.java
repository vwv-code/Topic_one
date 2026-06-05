package com.topicone.dto;

import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateConversationRequest {
    /** 用户ID */
    private Long userId;

    /** 场景ID（可选，不传则默认） */
    private Long sceneId;

    /** 对话标题（可选，不传则自动生成） */
    private String title;
}
