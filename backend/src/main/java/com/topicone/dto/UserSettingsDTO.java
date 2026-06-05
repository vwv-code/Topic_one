package com.topicone.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户设置返回 DTO
 */
@Data
public class UserSettingsDTO {

    /** 当前选中的场景ID */
    private Long currentSceneId;

    /** 难度等级：beginner/intermediate/advanced */
    private String difficulty;

    BigDecimal speechSpeed;
}
