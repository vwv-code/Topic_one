package com.topicone.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicone.dto.pronunciation.ExpressionCorrectionResult;
import com.topicone.dto.pronunciation.PronunciationResult;
import com.topicone.dto.ws.WsMessage;
import com.topicone.entity.Conversation;
import com.topicone.entity.Message;
import com.topicone.entity.PronunciationEvaluation;
import com.topicone.entity.UserSetting;
import com.topicone.mapper.ConversationMapper;
import com.topicone.mapper.PronunciationEvaluationMapper;
import com.topicone.mapper.UserSettingMapper;
import com.topicone.service.ExpressionCorrectionService;
import com.topicone.service.MessageService;
import com.topicone.service.PromptBuilderService;
import com.topicone.service.asr.AsrService;
import com.topicone.service.llm.LlmService;
import com.topicone.service.tts.TtsService;
import com.topicone.service.pronunciation.PronunciationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
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

    @Autowired
    private PronunciationService pronunciationService;

    @Autowired
    private ExpressionCorrectionService expressionCorrectionService;

    @Autowired
    private UserSettingMapper userSettingMapper;

    @Autowired
    private PronunciationEvaluationMapper pronunciationEvaluationMapper;

    @Autowired
    private ConversationMapper conversationMapper;

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

        // 缓冲用户音频（用于后续发音评测）
        voiceSession.appendAudioData(pcmData);

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
     * 停止对话：结束所有服务，触发发音评测，清理资源
     */
    private void handleStopCommand(WebSocketSession session) throws IOException {
        VoiceSession voiceSession = sessions.get(session.getId());
        if (voiceSession != null) {
            voiceSession.setState(SessionState.IDLE);
            if (voiceSession.getAsrSessionId() != null) {
                asrService.stopSession(voiceSession.getAsrSessionId());
            }

            // ★ 触发发音评测：使用对话中收集的全部用户语音
            List<PronunciationService.UserUtterance> utterances = voiceSession.getUtterances();
            if (!utterances.isEmpty()) {
                log.info("[语音] 开始发音评测, 共{}句", utterances.size());
                pronunciationService.evaluateBatch(utterances,
                        results -> {
                            // 逐条发送评测结果
                            for (PronunciationResult r : results) {
                                safeSendJson(session, WsMessage.pronunciationResult(r));
                            }
                            safeSendJson(session, WsMessage.pronunciationComplete());
                            log.info("[语音] 发音评测完成, 共{}条结果", results.size());

                            // ★ 存储评测结果到数据库
                            savePronunciationResults(voiceSession.getConversationId(), results);
                        },
                        error -> {
                            log.error("[语音] 发音评测失败: {}", error);
                            safeSendJson(session, WsMessage.error("发音评测失败: " + error));
                        });

                // ★ 触发表达纠错：提取所有用户说的句子文本，发给 LLM 纠错
                List<String> sentences = utterances.stream()
                        .map(PronunciationService.UserUtterance::text)
                        .toList();
                log.info("[语音] 开始表达纠错, 共{}句", sentences.size());
                Conversation conv = conversationMapper.selectByConversationId(voiceSession.getConversationId());
                Long userId = conv != null ? conv.getUserId() : 0L;
                expressionCorrectionService.correctBatch(
                        userId,
                        voiceSession.getConversationId(),
                        sentences,
                        result -> {
                            // 逐条发送纠错结果
                            safeSendJson(session, WsMessage.expressionCorrectionResult(result));
                        },
                        allResults -> {
                            safeSendJson(session, WsMessage.expressionCorrectionComplete());
                            log.info("[语音] 表达纠错完成, 共{}条结果", allResults.size());
                        },
                        error -> {
                            log.error("[语音] 表达纠错失败: {}", error);
                            safeSendJson(session, WsMessage.error("表达纠错失败: " + error));
                        });
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
            // 空句子：丢弃已缓存的音频
            voiceSession.clearCurrentAudio();
            return;
        }

        log.info("[语音] 用户说完: '{}', 进入 LLM 处理", userText.trim());

        // ★ 保存当前句子的音频数据（用于发音评测）
        byte[] sentenceAudio = voiceSession.flushCurrentAudio();
        // 记录语音句子（音频+文本），供停止时批量评测
        voiceSession.addUtterance(new PronunciationService.UserUtterance(sentenceAudio, userText));

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
                    int speechRate = getSpeechRateForConversation(conversationId);
                    ttsService.synthesizeStream(fullText, speechRate, new TtsService.TtsStreamListener() {
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

    /** 读取用户设置的语速并转换为 NLS speech_rate (-500~500) */
    private int getSpeechRateForConversation(Long conversationId) {
        try {
            Conversation conv = conversationMapper.selectByConversationId(conversationId);
            if (conv == null) return 0;
            UserSetting setting = userSettingMapper.selectById(conv.getUserId());
            if (setting == null || setting.getSpeechSpeed() == null) return 0;
            BigDecimal speed = setting.getSpeechSpeed(); // 0.5 ~ 2.0
            int rate = (int) ((speed.doubleValue() - 1.0) * 500);
            return Math.max(-500, Math.min(500, rate));
        } catch (Exception e) {
            return 0;
        }
    }

    /** 将发音评测结果保存到数据库 */
    private void savePronunciationResults(Long conversationId, List<PronunciationResult> results) {
        try {
            Conversation conv = conversationMapper.selectByConversationId(conversationId);
            if (conv == null) {
                log.warn("[语音] 未找到会话, conversationId={}, 跳过评测存储", conversationId);
                return;
            }
            Long userId = conv.getUserId();

            for (PronunciationResult r : results) {
                PronunciationEvaluation pe = new PronunciationEvaluation();
                pe.setUserId(userId);
                pe.setConversationId(conversationId);
                pe.setRefText(r.getRefText());
                pe.setOverallScore(r.getOverallScore());
                pe.setAccuracyScore(r.getAccuracyScore());
                pe.setFluencyScore(r.getFluencyScore());
                pe.setIntegrityScore(r.getIntegrityScore());
                pe.setAudioDuration(r.getAudioDuration());
                pe.setCreateTime(LocalDateTime.now());

                // 序列化单词详情的 JSON
                if (r.getWordDetails() != null && !r.getWordDetails().isEmpty()) {
                    try {
                        pe.setWordDetails(objectMapper.writeValueAsString(r.getWordDetails()));
                    } catch (JsonProcessingException e) {
                        log.warn("[语音] 序列化单词详情失败", e);
                    }
                }

                pronunciationEvaluationMapper.insert(pe);
            }
            log.info("[语音] 已存储 {} 条发音评测记录, userId={}, conversationId={}",
                    results.size(), userId, conversationId);
        } catch (Exception e) {
            log.error("[语音] 存储发音评测失败", e);
        }
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
        /** 当前句子的音频缓冲（RECORDING 期间持续追加） */
        private final List<byte[]> currentAudioChunks = new ArrayList<>();
        /** 对话中收集的全部用户语音句子（音频+文本），停止时用于发音评测 */
        private final List<PronunciationService.UserUtterance> utterances = new ArrayList<>();

        public VoiceSession(String webSocketSessionId, Long conversationId) {
            this.webSocketSessionId = webSocketSessionId;
            this.conversationId = conversationId;
        }

        /** 追加音频数据（用户录音期间持续调用） */
        void appendAudioData(byte[] pcmData) {
            currentAudioChunks.add(pcmData);
        }

        /** 取出并清空当前句子的全部音频缓冲，合并为单段 PCM */
        byte[] flushCurrentAudio() {
            if (currentAudioChunks.isEmpty()) return new byte[0];
            int totalLen = currentAudioChunks.stream().mapToInt(b -> b.length).sum();
            byte[] merged = new byte[totalLen];
            int pos = 0;
            for (byte[] chunk : currentAudioChunks) {
                System.arraycopy(chunk, 0, merged, pos, chunk.length);
                pos += chunk.length;
            }
            currentAudioChunks.clear();
            return merged;
        }

        /** 清空当前句子音频缓冲 */
        void clearCurrentAudio() {
            currentAudioChunks.clear();
        }

        /** 添加一个用户语音句子 */
        void addUtterance(PronunciationService.UserUtterance utterance) {
            utterances.add(utterance);
        }

        /** 获取全部用户语音句子 */
        List<PronunciationService.UserUtterance> getUtterances() {
            return new ArrayList<>(utterances);
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
