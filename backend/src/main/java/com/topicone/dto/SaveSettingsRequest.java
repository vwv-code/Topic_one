package com.topicone.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存用户设置请求 DTO
 */
@Data
public class SaveSettingsRequest {

    /** 用户ID */
    private Long userId;

    /** 当前选中的场景ID */
    private Long currentSceneId;

    /** 难度等级：beginner/intermediate/advanced */
    private String difficulty;

    /** AI语音速度：0.5~2.0 */
    private BigDecimal speechSpeed;

    /** 当前场景ID（用于同步更新场景描述/角色设定） */
    private Long sceneId;

    /** 场景描述（同步更新到场景表） */
    private String description;

    /** 角色设定（同步更新到场景表） */
    private String roleSetting;
}
