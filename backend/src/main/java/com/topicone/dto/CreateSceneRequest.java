package com.topicone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建自定义场景请求 DTO
 */
@Data
public class CreateSceneRequest {

    /** 用户ID */
    private Long userId;

    @NotBlank(message = "场景名称不能为空")
    private String sceneName;

    @NotBlank(message = "场景描述不能为空")
    private String description;

    /** 角色设定要求 */
    private String roleSetting;

    /** 难度等级：1-初级 2-中级 3-高级 */
    private Integer difficulty = 1;
}
