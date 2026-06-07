package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.ConversationBackground;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationBackgroundMapper extends BaseMapper<ConversationBackground> {

    @Select("SELECT * FROM conversation_background WHERE conversation_id = #{conversationId}")
    ConversationBackground selectByConversationId(@Param("conversationId") Long conversationId);
}
