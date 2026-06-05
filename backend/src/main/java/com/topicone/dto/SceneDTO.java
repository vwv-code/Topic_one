package com.topicone.dto;

import lombok.Data;

/**
 * 场景列表返回 DTO
 */
@Data
public class SceneDTO {

    private Long sceneId;
    private String sceneName;
    private String description;
    private String roleSetting;
    private Integer difficulty;
    private String vocabulary;   // JSON 字符串
    private String sentences;    // JSON 字符串
    private Boolean isBuiltin;
    private String icon;
    private Integer sortOrder;
}
