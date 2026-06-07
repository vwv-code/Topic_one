package com.topicone.service;

import com.topicone.dto.pronunciation.ExpressionCorrectionResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * 表达纠错服务接口
 *
 * 在麦克风停止后，将本次会话中用户说的所有英文句子逐句发送给 LLM 进行表达纠错。
 */
public interface ExpressionCorrectionService {

    /**
     * 批量表达纠错（逐句发送给 LLM，汇总结果）
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @param sentences      用户说的英文句子列表（按时间顺序）
     * @param onResult       每完成一句的回调（用于逐条 WebSocket 推送）
     * @param onComplete     全部完成回调
     * @param onError        错误回调
     */
    void correctBatch(Long userId, Long conversationId,
                      List<String> sentences,
                      Consumer<ExpressionCorrectionResult> onResult,
                      Consumer<List<ExpressionCorrectionResult>> onComplete,
                      Consumer<String> onError);
}
