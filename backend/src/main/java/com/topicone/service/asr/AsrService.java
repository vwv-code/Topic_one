package com.topicone.service.asr;

/**
 * ASR 语音识别服务接口
 *
 * 支持实时流式语音识别，接收 PCM 音频帧，返回实时/最终识别结果
 */
public interface AsrService {

    /**
     * 开始一次新的识别会话
     *
     * @return 会话ID
     */
    String startSession();

    /**
     * 发送一帧 PCM 音频数据
     *
     * @param sessionId 识别会话ID
     * @param pcmData   PCM 音频字节数据（16kHz, 16bit, mono）
     */
    void sendAudio(String sessionId, byte[] pcmData);

    /**
     * 结束识别会话，获取最终识别结果
     *
     * @param sessionId 识别会话ID
     * @return 最终识别文本
     */
    String stopSession(String sessionId);

    /**
     * 设置识别结果回调
     *
     * @param listener 结果监听器
     */
    void setListener(AsrResultListener listener);

    /**
     * ASR 识别结果监听器
     */
    interface AsrResultListener {

        /**
         * 收到中间识别结果（实时显示用）
         */
        void onIntermediateResult(String sessionId, String text);

        /**
         * 收到最终识别结果（用户说完一句完整的话）
         */
        void onFinalResult(String sessionId, String text);

        /**
         * 识别完成（所有句子处理完毕）
         */
        void onComplete(String sessionId, String finalText);

        /**
         * 识别出错
         */
        void onError(String sessionId, String error);
    }
}
