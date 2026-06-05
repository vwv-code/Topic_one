package com.topicone.service.impl;

import cn.hutool.core.util.IdUtil;
import com.topicone.dto.CreateSceneRequest;
import com.topicone.dto.SceneDTO;
import com.topicone.entity.Scene;
import com.topicone.mapper.SceneMapper;
import com.topicone.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SceneServiceImpl implements SceneService {

    private final SceneMapper sceneMapper;

    @Override
    public List<SceneDTO> getSceneList(Long userId) {
        List<Scene> scenes = sceneMapper.selectByUserId(userId);
        return scenes.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SceneDTO createCustomScene(CreateSceneRequest request) {
        // 生成雪花算法场景ID（确保全局唯一）
        long sceneId = IdUtil.getSnowflakeNextId();

        // 查询当前用户已有场景的最大排序值
        int maxSort = 0;
        List<Scene> existing = sceneMapper.selectByUserId(request.getUserId());
        if (!existing.isEmpty()) {
            maxSort = existing.stream()
                    .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                    .max().orElse(0);
        }

        // 构建首字母图标：取名称首字 + 颜色池轮换
        String firstChar = request.getSceneName().substring(0, 1);
        int colorIndex = (existing.size() % 8); // 8色轮换
        String[] colorPool = {"#6366f1", "#8b5cf6", "#06b6d4", "#10b981",
                              "#f59e0b", "#ef4444", "#ec4899", "#3b82f6"};
        String icon = firstChar + "|" + colorPool[colorIndex];

        Scene scene = new Scene();
        scene.setId(request.getUserId());
        scene.setSceneId(sceneId);
        scene.setSceneName(request.getSceneName());
        scene.setDescription(request.getDescription());
        scene.setRoleSetting(request.getRoleSetting() != null ? request.getRoleSetting() : "");
        scene.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : 1);
        scene.setVocabulary("[]");
        scene.setSentences("[]");
        scene.setIsBuiltin(0);
        scene.setIcon(icon);
        scene.setSortOrder(maxSort + 1);

        sceneMapper.insert(scene);

        return toDTO(scene);
    }

    @Override
    public void deleteScene(Long sceneId) {
        int rows = sceneMapper.deleteBySceneId(sceneId);
        if (rows == 0) {
            throw new com.topicone.common.exception.BusinessException("场景不存在或已被删除");
        }
    }

    /**
     * Entity → DTO 转换
     */
    private SceneDTO toDTO(Scene scene) {
        SceneDTO dto = new SceneDTO();
        BeanUtils.copyProperties(scene, dto);
        dto.setIsBuiltin(scene.getIsBuiltin() == 1);
        return dto;
    }
}
