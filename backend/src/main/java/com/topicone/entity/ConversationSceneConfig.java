package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话场景配置实体（每个会话独立的描述和角色设定）
 */
@Data
@TableName("conversation_scene_config")
public class ConversationSceneConfig {

    /** 会话ID（主键，关联user_conversation） */
    @TableId(type = IdType.INPUT)
    private Long conversationId;

    /** 本次对话的场景描述 */
    private String description;

    /** 本次对话的角色设定 */
    private String roleSetting;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
