package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.CreateSceneRequest;
import com.topicone.dto.SceneDTO;
import com.topicone.service.SceneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    /**
     * 获取用户全部场景列表
     * GET /api/scenes?userId=1
     */
    @GetMapping
    public Result<List<SceneDTO>> listScenes(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(sceneService.getSceneList(userId));
    }

    /**
     * 创建自定义场景
     * POST /api/scenes
     */
    @PostMapping
    public Result<SceneDTO> createScene(@Valid @RequestBody CreateSceneRequest request) {
        return Result.success(sceneService.createCustomScene(request));
    }

    /**
     * 删除自定义场景（逻辑删除）
     * DELETE /api/scenes?sceneId=xxx
     */
    @DeleteMapping
    public Result<Void> deleteScene(@RequestParam Long sceneId) {
        sceneService.deleteScene(sceneId);
        return Result.success(null);
    }
}
