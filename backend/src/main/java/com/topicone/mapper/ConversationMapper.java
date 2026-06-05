package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.Conversation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 查询用户的全部会话列表（按创建时间倒序）
     */
    @Select("SELECT * FROM user_conversation WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Conversation> selectByUserId(Long userId);

    /**
     * 按 conversationId 查询
     */
    @Select("SELECT * FROM user_conversation WHERE conversation_id = #{conversationId}")
    Conversation selectByConversationId(Long conversationId);

    /**
     * 按 conversationId 逻辑删除会话
     */
    @Delete("UPDATE user_conversation SET deleted = 1 WHERE conversation_id = #{conversationId}")
    int deleteById(@Param("conversationId") Long conversationId);
}
