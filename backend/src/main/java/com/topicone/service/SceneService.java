package com.topicone.service;

import com.topicone.dto.CreateSceneRequest;
import com.topicone.dto.SceneDTO;

import java.util.List;

public interface SceneService {

    /**
     * 获取用户全部场景列表
     */
    List<SceneDTO> getSceneList(Long userId);

    /**
     * 创建自定义场景
     */
    SceneDTO createCustomScene(CreateSceneRequest request);

    /**
     * 删除自定义场景（逻辑删除）
     */
    void deleteScene(Long sceneId);
}
