package com.topicone.service.pronunciation;

import com.topicone.dto.pronunciation.PronunciationResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * 发音评测服务接口
 */
public interface PronunciationService {

    /**
     * 评测单句发音
     *
     * @param pcmAudio   PCM 音频数据（16kHz / 16bit / mono）
     * @param refText    参考文本（用户实际说的话）
     * @param onComplete 评测完成回调
     * @param onError    错误回调
     */
    void evaluate(byte[] pcmAudio, String refText,
                  Consumer<PronunciationResult> onComplete,
                  Consumer<String> onError);

    /**
     * 批量评测（顺序执行，汇总结果）
     *
     * @param utterances 用户语音句列表（每句包含音频+文本）
     * @param onComplete 全部完成回调
     * @param onError    错误回调
     */
    void evaluateBatch(List<UserUtterance> utterances,
                       Consumer<List<PronunciationResult>> onComplete,
                       Consumer<String> onError);

    /**
     * 一条用户语音
     */
    record UserUtterance(byte[] pcmAudio, String text) {}
}
