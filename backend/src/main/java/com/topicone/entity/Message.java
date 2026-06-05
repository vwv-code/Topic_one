package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@TableName("user_message")
public class Message {

    /** 消息ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long messageId;

    /** 关联的会话ID */
    private Long conversationId;

    /** 消息角色：user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

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
