package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对话会话实体
 */
@Data
@TableName("user_conversation")
public class Conversation {

    /** 会话ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long conversationId;

    /** 用户ID */
    private Long userId;

    /** 场景ID */
    private Long sceneId;

    /** 对话标题 */
    private String title;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
