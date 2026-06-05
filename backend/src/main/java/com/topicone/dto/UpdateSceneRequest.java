package com.topicone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新场景详情请求 DTO
 */
@Data
public class UpdateSceneRequest {

    @NotBlank(message = "场景ID不能为空")
    private Long sceneId;

    /** 场景描述 */
    private String description;

    /** 角色设定 */
    private String roleSetting;
}
