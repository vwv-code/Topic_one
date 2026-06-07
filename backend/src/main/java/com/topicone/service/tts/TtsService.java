package com.topicone.service.tts;

/**
 * TTS 文本转语音服务接口
 *
 * 支持流式合成，将文本分段转换为语音并回调返回音频分片
 */
public interface TtsService {

    /**
     * 流式文本转语音
     *
     * @param text       要合成的文本
     * @param speechRate 语速（NLS speech_rate，范围 -500~500，0=正常）
     * @param listener   音频分片监听器
     */
    void synthesizeStream(String text, int speechRate, TtsStreamListener listener);

    /**
     * TTS 流式响应监听器
     */
    interface TtsStreamListener {

        /** 收到一段音频数据（PCM 格式） */
        void onAudioData(byte[] audioData);

        /** 合成完成 */
        void onComplete();

        /** 出错 */
        void onError(String error);
    }
}
