package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.ConversationDTO;
import com.topicone.dto.ConversationSceneConfigDTO;
import com.topicone.dto.CreateConversationRequest;
import com.topicone.service.ConversationSceneConfigService;
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
    private final ConversationSceneConfigService configService;

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

    /**
     * 更新会话标题
     * PUT /api/conversations/title?conversationId=xxx&title=xxx
     */
    @PutMapping("/title")
    public Result<Void> updateTitle(@RequestParam Long conversationId, @RequestParam String title) {
        conversationService.updateTitle(conversationId, title);
        return Result.success(null);
    }

    /**
     * 获取会话的场景配置（场景ID + 描述 + 角色设定）
     * GET /api/conversations/config?conversationId=xxx
     */
    @GetMapping("/config")
    public Result<ConversationSceneConfigDTO> getConversationConfig(@RequestParam Long conversationId) {
        var config = configService.getConfig(conversationId);
        if (config == null) {
            return Result.success(null);
        }
        ConversationSceneConfigDTO dto = new ConversationSceneConfigDTO();
        dto.setConversationId(config.getConversationId());
        // 需要从 user_conversation 表取 sceneId
        var conv = conversationService.getConversationById(conversationId);
        dto.setSceneId(conv != null ? conv.getSceneId() : null);
        dto.setDescription(config.getDescription());
        dto.setRoleSetting(config.getRoleSetting());
        return Result.success(dto);
    }
}
