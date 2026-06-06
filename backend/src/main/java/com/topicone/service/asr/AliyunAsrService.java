package com.topicone.service.asr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * ASR 服务实现 - 阿里云 Paraformer 实时转写（完整实现）
 *
 * 通过 OkHttp WebSocket 连接阿里云智能语音交互(NLS)服务，
 * 使用 Paraformer 模型进行实时语音识别。
 *
 * 鉴权流程（阿里云官方文档要求）:
 *   Step 1: 调用 CreateToken POP API → 获取临时 Token（24h 有效）
 *   Step 2: WebSocket 连接时携带 Token 参数
 *
 * 数据流:
 *   StartTranscription → PCM 二进制帧 → SentenceEnd → StopTranscription
 */
@Slf4j
@Service
public class AliyunAsrService implements AsrService {

    @Value("${asr.access-key-id:}")
    private String accessKeyId;

    @Value("${asr.access-key-secret:}")
    private String accessKeySecret;

    @Value("${asr.app-key:}")
    private String appKey;

    /** NLS 网关地址 */
    private static final String NLS_GATEWAY = "nls-gateway-cn-beijing.aliyuncs.com";
    private static final String NLS_PATH = "/ws/v1";

    /** Token 服务地址 */
    private static final String TOKEN_DOMAIN = "nls-meta.cn-shanghai.aliyuncs.com";
    private static final String TOKEN_ACTION = "CreateToken";
    private static final String TOKEN_VERSION = "2019-02-28";
    private static final String TOKEN_REGION_ID = "cn-shanghai";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private AsrResultListener listener;
    private final ConcurrentHashMap<String, AsrSession> sessions = new ConcurrentHashMap<>();

    /** 缓存的 Token 及其过期时间 */
    private volatile String cachedToken;
    private volatile long tokenExpireTime; // 秒级时间戳

    public AliyunAsrService() {
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("AliyunAsrService 初始化完成, appKey={}", appKey);
        if (accessKeyId == null || accessKeyId.isBlank() || "your-access-key-id".equals(accessKeyId)) {
            log.warn("阿里云 NLS AccessKey 未配置！ASR 功能将无法使用。请在 application.yml 中配置 asr.*");
        }
    }

    // ========== AsrService 接口实现 ==========

    @Override
    public String startSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String taskId = UUID.randomUUID().toString().replace("-", "");

        try {
            // Step 1: 获取或刷新 Token
            String token = getToken();

            // Step 2: 构建 NLS WebSocket URL（带 Token 参数）
            String url = String.format("wss://%s%s?token=%s", NLS_GATEWAY, NLS_PATH, token);

            // 创建会话对象
            AsrSession session = new AsrSession(sessionId, taskId);
            sessions.put(sessionId, session);

            // 建立 NLS WebSocket 连接
            connectNlsWebSocket(session, url);

            log.info("ASR 会话创建: sessionId={}, taskId={}", sessionId, taskId);
            return sessionId;
        } catch (Exception e) {
            log.error("ASR 会话创建失败", e);
            return null;
        }
    }

    @Override
    public void sendAudio(String sessionId, byte[] pcmData) {
        AsrSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("ASR 会话不存在: {}", sessionId);
            return;
        }

        if (!session.isReady()) {
            session.bufferPcm(pcmData);
            return;
        }

        sendBinaryToNls(session, pcmData);
    }

    @Override
    public String stopSession(String sessionId) {
        AsrSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("ASR 会话不存在或已关闭: {}", sessionId);
            return "";
        }

        try {
            sendStopCommand(session);

            // 等待最终结果（最多等待 5 秒）
            long deadline = System.currentTimeMillis() + 5000;
            while (!session.isCompleted() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }

            String finalText = session.getFinalText();
            log.info("ASR 会话结束: sessionId={}, 最终结果='{}', 长度={}", sessionId, finalText, finalText.length());

            closeNlsConnection(session);
            sessions.remove(sessionId);

            return finalText;
        } catch (Exception e) {
            log.error("ASR 停止会话异常: sessionId={}", sessionId, e);
            sessions.remove(sessionId);
            return "";
        }
    }

    @Override
    public void setListener(AsrResultListener listener) {
        this.listener = listener;
    }

    // ========== Token 管理（核心修复）==========

    /**
     * 获取有效的 NLS Token（带缓存和自动刷新）
     *
     * 阿里云 NLS 要求通过 CreateToken POP API 获取临时 Token，
     * 不能直接用 AK/SK 做 URL 签名。
     */
    private synchronized String getToken() throws Exception {
        long now = System.currentTimeMillis() / 1000;

        // 如果缓存有效且未过期（提前 5 分钟刷新）
        if (cachedToken != null && now < tokenExpireTime - 300) {
            log.debug("[Token] 使用缓存的 Token");
            return cachedToken;
        }

        log.info("[Token] 正在获取新的 NLS Token...");
        String token = fetchTokenFromApi();
        cachedToken = token;
        log.info("[Token] Token 获取成功");
        return token;
    }

    /**
     * 调用阿里云 CreateToken POP API 获取临时 Token
     *
     * API 文档：https://help.aliyun.com/zh/isi/getting-started/use-http-or-https-to-obtain-an-access-token
     *
     * 请求方式: POST https://nls-meta.cn-shanghai.aliyuncs.com/
     * 签名算法: 阿里云 POP 协议（HMAC-SHA1）
     */
    private String fetchTokenFromApi() throws IOException {
        // 1. 构建公共参数
        String timestamp = formatUtcTimestamp();
        String nonce = UUID.randomUUID().toString();

        Map<String, String> params = new TreeMap<>(); // TreeMap 保证字典序排序
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", TOKEN_ACTION);
        params.put("Format", "JSON");
        params.put("RegionId", TOKEN_REGION_ID);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", nonce);
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", timestamp);
        params.put("Version", TOKEN_VERSION);

        // 2. 计算签名（阿里云 POP 签名规范）
        String signature = computePopSignature(params, "POST");

        // 3. 构建请求体
        StringBuilder bodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (bodyBuilder.length() > 0) bodyBuilder.append("&");
            bodyBuilder.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
        }
        bodyBuilder.append("&Signature=").append(urlEncode(signature));

        // 4. 发送 HTTP POST 请求
        RequestBody requestBody = RequestBody.create(bodyBuilder.toString(), MediaType.parse("application/x-www-form-urlencoded"));
        Request request = new Request.Builder()
                .url("https://" + TOKEN_DOMAIN + "/")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CreateToken HTTP 失败: " + response.code() + " " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);

            // 解析响应：{"Token":{"Id":"xxx","ExpireTime":1234567890}}
            JsonNode tokenObj = json.path("Token");
            if (tokenObj.isMissingNode()) {
                throw new IOException("CreateToken 响应格式异常: " + responseBody);
            }

            String tokenId = tokenObj.path("Id").asText("");
            long expireTime = tokenObj.path("ExpireTime").asLong(0);

            if (tokenId.isEmpty()) {
                throw new IOException("CreateToken 返回空 Token: " + responseBody);
            }

            tokenExpireTime = expireTime;
            log.info("[Token] 新 Token 已获取, expireTime={}", new Date(expireTime * 1000L));
            return tokenId;
        }
    }

    /**
     * 计算阿里云 POP 签名
     *
     * 算法（来自官方文档）:
     *   1. 所有参数按 key 字典序排列
     *   2. 拼接为 query string（key=value&...）
     *   3. StringToSign = HttpMethod + "&" + percentEncode("/") + "&" + percentEncode(queryString)
     *   4. Signature = Base64(HmacSHA1(AK_Secret, StringToSign))
     */
    private String computePopSignature(Map<String, String> params, String httpMethod) throws IOException {
        try {
            // 按字典序拼接参数
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > 0) queryString.append("&");
                queryString.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            }

            // 构建 StringToSign
            String stringToSign = httpMethod +
                    "&" + urlEncode("/") +
                    "&" + urlEncode(queryString.toString());

            // HMAC-SHA1 签名
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IOException("POP 签名计算失败", e);
        }
    }

    /**
     * 格式化 UTC 时间戳（ISO 8601，阿里云要求的格式）
     * 示例：2026-06-06T08:15:03Z
     */
    private String formatUtcTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return sdf.format(new Date());
    }

    /**
     * URL 编码（符合 RFC 3986 / 阿里云要求）
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }

    // ========== NLS WebSocket 通信 ==========

    private void connectNlsWebSocket(AsrSession session, String url) {
        Request request = new Request.Builder().url(url).build();

        okhttp3.WebSocketListener wsListener = new okhttp3.WebSocketListener() {
            @Override
            public void onOpen(okhttp3.WebSocket webSocket, Response response) {
                log.info("[NLS] WebSocket 已连接, sessionId={}", session.getSessionId());
                session.setNlsWebSocket(webSocket);
                sendStartCommand(session);
            }

            @Override
            public void onMessage(okhttp3.WebSocket webSocket, String text) {
                handleNlsMessage(session, text);
            }

            @Override
            public void onMessage(okhttp3.WebSocket webSocket, okio.ByteString bytes) { }

            @Override
            public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
                log.error("[NLS] WebSocket 异常, sessionId={}, responseCode={}",
                        session.getSessionId(), response != null ? response.code() : "null", t);
                session.setError(t.getMessage());
                notifyError(session, "NLS 连接失败: " + t.getMessage());
            }

            @Override
            public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                log.info("[NLS] WebSocket 已关闭, sessionId={}, code={}, reason={}",
                        session.getSessionId(), code, reason);
                session.setClosed(true);
            }
        };

        okhttp3.WebSocket ws = httpClient.newWebSocket(request, wsListener);
        session.setNlsWebSocket(ws);
    }

    private void sendStartCommand(AsrSession session) {
        try {
            // 使用 LinkedHashMap 保证 JSON 字段顺序（NLS 对字段顺序敏感）
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("message_id", UUID.randomUUID().toString().replace("-", ""));
            header.put("task_id", session.getTaskId());
            header.put("namespace", "SpeechTranscriber");
            header.put("name", "StartTranscription");
            header.put("appkey", appKey);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("format", "pcm");
            payload.put("sample_rate", 16000);
            payload.put("enable_inverse_text_normalization", true);
            payload.put("enable_punctuation_prediction", true);
            payload.put("enable_words", false);
            payload.put("max_sentence_silence", 1000);  // 静音 1 秒判定为断句（默认 800ms）

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("code", 20000000);
            status.put("message", "SUCCESS");

            Map<String, Object> command = new LinkedHashMap<>();
            command.put("header", header);
            command.put("payload", payload);
            command.put("status", status);

            String json = objectMapper.writeValueAsString(command);
            log.info("[NLS] 发送 StartTranscription:\n{}", json);
            session.getNlsWebSocket().send(json);
        } catch (Exception e) {
            log.error("[NLS] 发送 StartTranscription 失败", e);
        }
    }

    private void sendStopCommand(AsrSession session) {
        try {
            Map<String, Object> header = Map.of(
                    "message_id", UUID.randomUUID().toString().replace("-", ""),
                    "task_id", session.getTaskId(),
                    "namespace", "SpeechTranscriber",
                    "name", "StopTranscription",
                    "appkey", appKey
            );

            Map<String, Object> command = Map.of(
                    "header", header,
                    "payload", Map.of(),
                    "status", Map.of("code", 20000000, "message", "SUCCESS")
            );

            session.getNlsWebSocket().send(objectMapper.writeValueAsString(command));
        } catch (Exception e) {
            log.error("[NLS] 发送 StopTranscription 失败", e);
        }
    }

    private void sendBinaryToNls(AsrSession session, byte[] pcmData) {
        okhttp3.WebSocket ws = session.getNlsWebSocket();
        if (ws != null && !session.isClosed()) {
            ws.send(okio.ByteString.of(pcmData));
        }
    }

    private void handleNlsMessage(AsrSession session, String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String name = root.path("header").path("name").asText("");
            int statusCode = root.path("status").path("code").asInt(0);
            String statusMsg = root.path("status").path("message").asText("");

            // 打印所有收到的消息（诊断用）
            log.info("[NLS] 收到: name={}, code={}, msg={}", name, statusCode, statusMsg);

            switch (name) {
                case "TranscriptionStarted":
                    log.info("[NLS] 转写已启动, taskId={}", session.getTaskId());
                    session.setReady(true);
                    flushPcmBuffer(session);
                    break;

                case "SentenceBegin":
                    log.debug("[NLS] 检测到语音开始");
                    break;

                case "TranscriptionResultChanged":
                    String interimText = extractResultText(root);
                    session.setCurrentText(interimText);
                    if (listener != null && interimText != null && !interimText.isEmpty()) {
                        listener.onIntermediateResult(session.getSessionId(), interimText);
                    }
                    break;

                case "SentenceEnd":
                    String sentenceText = extractResultText(root);
                    log.info("[NLS] 句子结束: '{}'", sentenceText);
                    if (sentenceText != null && !sentenceText.isEmpty()) {
                        session.appendFinalText(sentenceText);
                        if (listener != null) {
                            listener.onFinalResult(session.getSessionId(), sentenceText);
                        }
                    }
                    break;

                case "TranscriptionCompleted":
                    log.info("[NLS] 转写完成, taskId={}", session.getTaskId());
                    session.setCompleted(true);
                    if (listener != null) {
                        listener.onComplete(session.getSessionId(), session.getFinalText());
                    }
                    break;

                default:
                    if (statusCode != 20000000) {
                        String errorMsg = statusMsg.isEmpty() ? "Unknown error" : statusMsg;
                        log.error("[NLS] 错误响应: name={}, code={}, msg={}, 完整响应:\n{}",
                                name, statusCode, errorMsg, text);
                        session.setError(errorMsg);
                        notifyError(session, errorMsg);
                    } else {
                        log.debug("[NLS] 收到消息: name={}", name);
                    }
            }
        } catch (Exception e) {
            log.error("[NLS] 处理消息异常: {}", text, e);
        }
    }

    private String extractResultText(JsonNode root) {
        JsonNode result = root.path("payload").path("result");

        if (result.isTextual()) {
            String text = result.asText();
            return text.isEmpty() ? null : text;
        }

        StringBuilder sb = new StringBuilder();
        if (result.isArray()) {
            for (JsonNode item : result) {
                String txt = item.path("text").asText("");
                if (!txt.isEmpty()) sb.append(txt);
            }
        }
        String resultStr = sb.toString();
        return resultStr.isEmpty() ? null : resultStr;
    }

    private void flushPcmBuffer(AsrSession session) {
        List<byte[]> buffer = session.drainPcmBuffer();
        if (!buffer.isEmpty()) {
            log.info("[NLS] 刷新 {} 帧缓存数据到 NLS", buffer.size());
            for (byte[] pcm : buffer) {
                sendBinaryToNls(session, pcm);
            }
        }
    }

    private void notifyError(AsrSession session, String error) {
        if (listener != null) {
            listener.onError(session.getSessionId(), error);
        }
    }

    private void closeNlsConnection(AsrSession session) {
        try {
            okhttp3.WebSocket ws = session.getNlsWebSocket();
            if (ws != null) ws.close(1000, "Normal closure");
        } catch (Exception e) {
            log.warn("[NLS] 关闭连接异常", e);
        }
    }

    // ========== 内部类 ==========

    @Data
    private static class AsrSession {
        private final String sessionId;
        private final String taskId;
        private volatile okhttp3.WebSocket nlsWebSocket;
        private volatile boolean ready = false;
        private volatile boolean completed = false;
        private volatile boolean closed = false;
        private volatile String currentText = "";
        private final StringBuilder finalText = new StringBuilder();
        private final List<byte[]> pcmBuffer = new CopyOnWriteArrayList<>();
        private volatile String error = null;

        public AsrSession(String sessionId, String taskId) {
            this.sessionId = sessionId;
            this.taskId = taskId;
        }

        public void bufferPcm(byte[] pcm) { pcmBuffer.add(pcm); }
        public List<byte[]> drainPcmBuffer() { List<byte[]> data = List.copyOf(pcmBuffer); pcmBuffer.clear(); return data; }
        public void appendFinalText(String text) { finalText.append(text); }
        public String getFinalText() { return finalText.toString().trim(); }
    }
}
