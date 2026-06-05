package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.Scene;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SceneMapper extends BaseMapper<Scene> {

    /**
     * 查询某用户的全部场景（含预定义+自定义）
     */
    @Select("SELECT * FROM scenes WHERE id = #{userId} AND deleted = 0 ORDER BY sort_order ASC, create_time ASC")
    List<Scene> selectByUserId(Long userId);

    /**
     * 按 sceneId 查询单个场景
     */
    @Select("SELECT * FROM scenes WHERE scene_id = #{sceneId}")
    Scene selectBySceneId(Long sceneId);

    /**
     * 按 sceneId 更新场景描述和角色设定
     */
    @Update("UPDATE scenes SET description = #{description}, role_setting = #{roleSetting}, update_time = NOW() WHERE scene_id = #{sceneId}")
    int updateBySceneId(@Param("sceneId") Long sceneId, @Param("description") String description, @Param("roleSetting") String roleSetting);

    /**
     * 按 sceneId 逻辑删除场景
     */
    @Delete("UPDATE scenes SET deleted = 1 WHERE scene_id = #{sceneId}")
    int deleteBySceneId(@Param("sceneId") Long sceneId);
}
