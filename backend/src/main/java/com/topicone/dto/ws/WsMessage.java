package com.topicone.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 统一消息格式（服务端 → 前端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {

    /** 消息类型 */
    private String type;

    /** 载荷数据 */
    private Object data;

    // ========== 工厂方法 ==========

    /** ASR 实时转写结果（中间态） */
    public static WsMessage recognitionText(String text) {
        return new WsMessage("recognition_text", text);
    }

    /** ASR 最终识别结果（用户说完一句话） */
    public static WsMessage recognitionFinal(String text) {
        return new WsMessage("recognition_final", text);
    }

    /** AI 回复文本片段（流式） */
    public static WsMessage aiResponseText(String text) {
        return new WsMessage("ai_response_text", text);
    }

    /** AI 回复完成（完整文本） */
    public static WsMessage aiResponseComplete(String fullText) {
        return new WsMessage("ai_response_complete", fullText);
    }

    /** TTS 语音分片（Base64 编码的音频数据） */
    public static WsMessage audioChunk(String base64Audio) {
        return new WsMessage("audio_chunk", base64Audio);
    }

    /** TTS 语音播放完毕 */
    public static WsMessage audioComplete() {
        return new WsMessage("audio_complete", null);
    }

    /** 状态变更通知 */
    public static WsMessage status(String status) {
        return new WsMessage("status", status);
    }

    /** 错误消息 */
    public static WsMessage error(String errorMessage) {
        return new WsMessage("error", errorMessage);
    }

    /** 发音评测结果 */
    public static WsMessage pronunciationResult(Object result) {
        return new WsMessage("pronunciation_result", result);
    }

    /** 发音评测完成（全部句子评测结束） */
    public static WsMessage pronunciationComplete() {
        return new WsMessage("pronunciation_complete", null);
    }
}
