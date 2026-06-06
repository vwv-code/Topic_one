package com.topicone.service.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM 大语言模型服务接口
 *
 * 支持流式调用，逐 token 返回 AI 回复
 */
public interface LlmService {

    /**
     * 流式调用 LLM
     *
     * @param systemPrompt 系统 Prompt（角色设定+场景描述+上下文）
     * @param userMessage   用户当前输入
     * @param history       历史对话消息 [{"role": "user/assistant", "content": "..."}]
     * @param listener      流式结果监听器
     */
    void chatStream(String systemPrompt, String userMessage,
                    List<Map<String, String>> history, LlmStreamListener listener);

    /**
     * LLM 流式响应监听器
     */
    interface LlmStreamListener {

        /** 收到一个文本片段（token/chunk） */
        void onChunk(String chunk);

        /** 流式输出完成，返回完整文本 */
        void onComplete(String fullText);

        /** 出错 */
        void onError(String error);
    }
}
