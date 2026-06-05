package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.ConversationSceneConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConversationSceneConfigMapper extends BaseMapper<ConversationSceneConfig> {

    /**
     * 按 conversationId 查询会话场景配置
     */
    @Select("SELECT * FROM conversation_scene_config WHERE conversation_id = #{conversationId}")
    ConversationSceneConfig selectByConversationId(Long conversationId);

    /**
     * 更新会话的场景配置（sceneId + 描述 + 角色设定）
     */
    @Update("UPDATE conversation_scene_config SET description = #{description}, role_setting = #{roleSetting}, update_time = NOW() WHERE conversation_id = #{conversationId}")
    int updateByConversationId(@Param("conversationId") Long conversationId,
                               @Param("description") String description,
                               @Param("roleSetting") String roleSetting);
}
