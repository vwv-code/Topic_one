package com.topicone.dto;

import lombok.Data;

/**
 * 会话场景配置 DTO（返回给前端）
 */
@Data
public class ConversationSceneConfigDTO {

    /** 会话ID */
    private Long conversationId;

    /** 场景ID */
    private Long sceneId;

    /** 本次对话的场景描述 */
    private String description;

    /** 本次对话的角色设定 */
    private String roleSetting;
}
