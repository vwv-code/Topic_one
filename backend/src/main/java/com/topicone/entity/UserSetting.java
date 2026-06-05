package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户设置实体（每个用户一行）
 */
@Data
@TableName("user_settings")
public class UserSetting {

    /** 用户ID（主键） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 当前选中的场景ID */
    private Long currentSceneId;

    /** 难度等级：beginner/intermediate/advanced */
    private String difficulty;

    /** AI语音速度：0.5~2.0 */
    private BigDecimal speechSpeed;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
