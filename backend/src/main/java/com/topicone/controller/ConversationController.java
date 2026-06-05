package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.ConversationDTO;
import com.topicone.dto.CreateConversationRequest;
import com.topicone.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 获取用户会话列表
     * GET /api/conversations?userId=1
     */
    @GetMapping
    public Result<List<ConversationDTO>> listConversations(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(conversationService.getConversationList(userId));
    }

    /**
     * 创建新会话
     * POST /api/conversations
     */
    @PostMapping
    public Result<ConversationDTO> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return Result.success(conversationService.createConversation(request));
    }

    /**
     * 删除会话（逻辑删除）
     * DELETE /api/conversations?id=xxx
     */
    @DeleteMapping
    public Result<Void> deleteConversation(@RequestParam Long id) {
        conversationService.deleteConversation(id);
        return Result.success(null);
    }
}
