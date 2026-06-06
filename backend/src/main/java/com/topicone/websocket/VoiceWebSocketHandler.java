package com.topicone.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicone.dto.ws.WsMessage;
import com.topicone.entity.Message;
import com.topicone.service.MessageService;
import com.topicone.service.PromptBuilderService;
import com.topicone.service.asr.AsrService;
import com.topicone.service.llm.LlmService;
import com.topicone.service.tts.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时语音对话 WebSocket Handler（全自动循环模式）
 *
 * 状态机流转：
 *   IDLE ──start──→ RECORDING ──ASR SentenceEnd──→ PROCESSING(LLM) ──→ SPEAKING(TTS) ──→ RECORDING
 *       ←──stop───←───────────────────────────────────────────────────────┘
 *
 * 核心变化：
 * - 不再需要用户手动点「停止」来触发 LLM
 * - ASR 检测到用户说完一句话(SentenceEnd) → 自动触发 LLM → TTS → 播放
 * - TTS 播放完毕后自动回到录音状态，继续监听下一句话
 * - 用户点停止才真正结束整个会话
 */
@Slf4j
@Component
public class VoiceWebSocketHandler implements WebSocketHandler {

    /** 会话状态枚举 */
    private enum SessionState {
        IDLE,           // 空闲，等待 start
        RECORDING,      // 录音中，ASR 正在识别
        PROCESSING,     // LLM 处理中
        SPEAKING        // TTS 播放中
    }

    @Autowired
    private AsrService asrService;

    @Autowired
    private LlmService llmService;

    @Autowired
    private TtsService ttsService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private PromptBuilderService promptBuilderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 当前活跃的会话 */
    private final ConcurrentHashMap<String, VoiceSession> sessions = new ConcurrentHashMap<>();

    // ========== WebSocket 生命周期 ==========

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 连接建立: sessionId={}", session.getId());
        sendJson(session, WsMessage.status("connected"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (message instanceof BinaryMessage) {
                handleBinaryMessage(session, (BinaryMessage) message);
            } else if (message instanceof TextMessage) {
                handleTextMessage(session, (TextMessage) message);
            }
        } catch (Exception e) {
            log.error("处理消息异常, sessionId={}", session.getId(), e);
            sendJson(session, WsMessage.error("消息处理异常: " + e.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误, sessionId={}", session.getId(), exception);
        cleanupSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
        cleanupSession(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ========== 消息处理 ==========

    /**
     * 处理二进制消息（PCM 音频数据）
     * 仅在 RECORDING 状态下转发给 ASR，其他状态丢弃
     */
    private void handleBinaryMessage(WebSocketSession session, BinaryMessage binaryMsg) throws IOException {
        VoiceSession voiceSession = sessions.get(session.getId());
        if (voiceSession == null || voiceSession.getAsrSessionId() == null) {
            return;
        }

        // 非 RECORDING 状态时忽略音频数据（TTS 播放期间不录音）
        if (voiceSession.getState() != SessionState.RECORDING) {
            return;
        }

        ByteBuffer buffer = binaryMsg.getPayload();
        byte[] pcmData = new byte[buffer.remaining()];
        buffer.get(pcmData);

        asrService.sendAudio(voiceSession.getAsrSessionId(), pcmData);
    }

    /**
     * 处理文本消息（JSON 控制指令）
     *
     * 支持指令：
     * - {"type": "start", "conversationId": 123}  开始对话
     * - {"type": "stop"}                            停止并结束整个会话
     */
    private void handleTextMessage(WebSocketSession session, TextMessage textMsg) throws IOException {
        String payload = textMsg.getPayload();
        Map<String, Object> command;
        try {
            command = objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            sendJson(session, WsMessage.error("无效 JSON"));
            return;
        }

        String type = (String) command.getOrDefault("type", "");
        switch (type) {
            case "start" -> handleStartCommand(session, command);
            case "stop" -> handleStopCommand(session);
            default -> sendJson(session, WsMessage.error("未知指令: " + type));
        }
    }

    /**
     * 开始对话：创建 ASR 会话，进入录音状态
     */
    private void handleStartCommand(WebSocketSession session, Map<String, Object> command) throws IOException {
        Long conversationId = extractConversationId(command);
        if (conversationId == null) {
            sendJson(session, WsMessage.error("缺少 conversationId"));
            return;
        }

        VoiceSession voiceSession = new VoiceSession(session.getId(), conversationId);
        sessions.put(session.getId(), voiceSession);

        String asrSessionId = asrService.startSession();
        voiceSession.setAsrSessionId(asrSessionId);
        voiceSession.setState(SessionState.RECORDING);

        asrService.setListener(new AsrEventListener(session, voiceSession));

        sendJson(session, WsMessage.status("recording"));
        log.info("[语音] 开始: ws={}, conv={}, asr={}", session.getId(), conversationId, asrSessionId);
    }

    /**
     * 停止对话：结束所有服务，清理资源
     */
    private void handleStopCommand(WebSocketSession session) throws IOException {
        VoiceSession voiceSession = sessions.get(session.getId());
        if (voiceSession != null) {
            voiceSession.setState(SessionState.IDLE);
            if (voiceSession.getAsrSessionId() != null) {
                asrService.stopSession(voiceSession.getAsrSessionId());
            }
        }
        cleanupSession(session.getId());
        sendJson(session, WsMessage.status("ready"));
        log.info("[语音] 停止: ws={}", session.getId());
    }

    // ========== 核心：自动循环流水线 ==========

    /**
     * ASR 检测到用户说完一句话 → 自动触发 LLM → TTS 流水线
     *
     * 这是全自动模式的核心：不需要用户手动点停止，
     * ASR 的 SentenceEnd 事件直接驱动后续流程。
     */
    private void onUserSentenceDetected(WebSocketSession session, VoiceSession voiceSession,
                                         String userText) {
        if (userText == null || userText.isBlank()) {
            return;
        }

        log.info("[语音] 用户说完: '{}', 进入 LLM 处理", userText.trim());

        // 1. 切换状态为 PROCESSING，通知前端
        voiceSession.setState(SessionState.PROCESSING);
        try {
            sendJson(session, WsMessage.recognitionFinal(userText));
            sendJson(session, WsMessage.status("processing"));
        } catch (IOException e) {
            log.error("发送状态失败", e);
        }

        // 2. 保存用户消息
        Long conversationId = voiceSession.getConversationId();
        messageService.saveUserMessage(conversationId, userText);

        // 3. 异步执行 LLM → TTS 流水线
        CompletableFuture.runAsync(() -> runLlmTtsPipeline(session, voiceSession, userText));
    }

    /**
     * 执行 LLM 流式调用 → TTS 合成 → 推送音频的完整流水线
     */
    private void runLlmTtsPipeline(WebSocketSession session, VoiceSession voiceSession,
                                    String userText) {
        Long conversationId = voiceSession.getConversationId();

        // 构建上下文
        List<Message> history = messageService.getMessagesByConversationId(conversationId);
        List<Map<String, String>> llmHistory = buildLlmHistory(history);
        String systemPrompt = promptBuilderService.buildSystemPrompt(conversationId);

        try {
            llmService.chatStream(systemPrompt, userText, llmHistory, new LlmService.LlmStreamListener() {
                private final StringBuilder aiFullResponse = new StringBuilder();

                @Override
                public void onChunk(String chunk) {
                    aiFullResponse.append(chunk);
                    safeSendJson(session, WsMessage.aiResponseText(chunk));
                }

                @Override
                public void onComplete(String fullText) {
                    log.info("[语音] LLM 完成, 文本长度={}, 开始 TTS", fullText.length());

                    // 保存 AI 回复
                    messageService.saveAssistantMessage(conversationId, fullText);

                    // 切换到 SPEAKING 状态
                    voiceSession.setState(SessionState.SPEAKING);
                    safeSendJson(session, WsMessage.aiResponseComplete(fullText));
                    safeSendJson(session, WsMessage.status("speaking"));

                    // 开始 TTS 流式合成
                    ttsService.synthesizeStream(fullText, new TtsService.TtsStreamListener() {
                        @Override
                        public void onAudioData(byte[] audioData) {
                            String base64 = Base64.getEncoder().encodeToString(audioData);
                            safeSendJson(session, WsMessage.audioChunk(base64));
                        }

                        @Override
                        public void onComplete() {
                            log.info("[语音] TTS 播放完成, 回到录音状态");
                            voiceSession.setState(SessionState.RECORDING);
                            safeSendJson(session, WsMessage.audioComplete());
                            safeSendJson(session, WsMessage.status("recording"));
                        }

                        @Override
                        public void onError(String error) {
                            log.error("[语音] TTS 错误: {}", error);
                            voiceSession.setState(SessionState.RECORDING);
                            safeSendJson(session, WsMessage.error("TTS 错误: " + error));
                            safeSendJson(session, WsMessage.status("recording"));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    log.error("[语音] LLM 错误: {}", error);
                    voiceSession.setState(SessionState.RECORDING);
                    safeSendJson(session, WsMessage.error("LLM 错误: " + error));
                    safeSendJson(session, WsMessage.status("recording"));
                }
            });
        } catch (Exception e) {
            log.error("[语音] Pipeline 异常", e);
            voiceSession.setState(SessionState.RECORDING);
            safeSendJson(session, WsMessage.error("处理异常: " + e.getMessage()));
            safeSendJson(session, WsMessage.status("recording"));
        }
    }

    // ========== 工具方法 ==========

    private Long extractConversationId(Map<String, Object> command) {
        Object idObj = command.get("conversationId");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        }
        return null;
    }

    /** 构建 LLM 历史消息（排除最后一条刚保存的用户消息） */
    private List<Map<String, String>> buildLlmHistory(List<Message> history) {
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < Math.max(0, history.size() - 1); i++) {
            Message msg = history.get(i);
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            result.add(m);
        }
        return result;
    }

    /** 安全发送 JSON（连接断开时不抛异常） */
    private void safeSendJson(WebSocketSession session, WsMessage msg) {
        try {
            sendJson(session, msg);
        } catch (IOException e) {
            log.warn("发送消息失败（可能已断开）: {}", e.getMessage());
        }
    }

    private void sendJson(WebSocketSession session, WsMessage msg) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void cleanupSession(String sessionId) {
        VoiceSession removed = sessions.remove(sessionId);
        if (removed != null && removed.getAsrSessionId() != null) {
            try {
                asrService.stopSession(removed.getAsrSessionId());
            } catch (Exception e) {
                log.warn("清理 ASR 会话异常", e);
            }
        }
    }

    // ========== 内部类 ==========

    /**
     * 语音会话上下文（含状态机）
     */
    @lombok.Data
    private static class VoiceSession {
        private final String webSocketSessionId;
        private final Long conversationId;
        private String asrSessionId;
        private SessionState state = SessionState.IDLE;

        public VoiceSession(String webSocketSessionId, Long conversationId) {
            this.webSocketSessionId = webSocketSessionId;
            this.conversationId = conversationId;
        }
    }

    /**
     * ASR 结果事件监听器
     *
     * 关键变化：
     * - onFinalResult（SentenceEnd）：自动触发 LLM 流水线（之前是空实现）
     * - 不再依赖手动 stop 来驱动流程
     */
    private class AsrEventListener implements AsrService.AsrResultListener {
        private final WebSocketSession session;
        private final VoiceSession voiceSession;

        AsrEventListener(WebSocketSession session, VoiceSession voiceSession) {
            this.session = session;
            this.voiceSession = voiceSession;
        }

        @Override
        public void onIntermediateResult(String sessionId, String text) {
            safeSendJson(session, WsMessage.recognitionText(text));
        }

        @Override
        public void onFinalResult(String sessionId, String text) {
            // ★ 核心：ASR 检测到用户说完一句完整的话 → 自动触发 LLM
            log.info("[ASR] 句子结束: '{}'", text);
            onUserSentenceDetected(session, voiceSession, text);
        }

        @Override
        public void onComplete(String sessionId, String finalText) {
            log.info("[ASR] 识别完成: {}", finalText);
        }

        @Override
        public void onError(String sessionId, String error) {
            safeSendJson(session, WsMessage.error("ASR 错误: " + error));
        }
    }
}
