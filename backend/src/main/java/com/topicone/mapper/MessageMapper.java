package com.topicone.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topicone.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询某个会话的全部消息（按时间正序）
     */
    @Select("SELECT * FROM user_message WHERE conversation_id = #{conversationId} AND deleted = 0 ORDER BY create_time ASC")
    List<Message> selectByConversationId(Long conversationId);
}
