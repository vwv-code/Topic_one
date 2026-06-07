package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_background")
public class ConversationBackground {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    private String sceneDescription;

    private String prompt;

    private String imageUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
