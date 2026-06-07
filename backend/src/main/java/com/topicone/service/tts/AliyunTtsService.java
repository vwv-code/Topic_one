package com.topicone.service.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * TTS 服务实现 - 阿里云 NLS 流式语音合成（完整实现）
 *
 * 通过 OkHttp WebSocket 连接阿里云智能语音交互(NLS)服务，
 * 将文本实时转换为 PCM 语音数据。
 *
 * 鉴权流程（与 ASR 相同）:
 *   Step 1: 调用 CreateToken POP API → 获取临时 Token（24h 有效）
 *   Step 2: WebSocket 连接时携带 Token 参数
 *
 * 数据流:
 *   StartSynthesis → 文本数据 → 接收二进制音频帧 → StopSynthesis
 */
@Slf4j
@Service
public class AliyunTtsService implements TtsService {

    @Value("${tts.access-key-id:}")
    private String accessKeyId;

    @Value("${tts.access-key-secret:}")
    private String accessKeySecret;

    @Value("${tts.app-key:}")
    private String appKey;

    /** TTS 参数 */
    @Value("${tts.voice:xiaoyun}")
    private String voice;

    @Value("${tts.format:pcm}")
    private String format;

    @Value("${tts.sample-rate:16000}")
    private int sampleRate;

    @Value("${tts.volume:50}")
    private int volume;

    @Value("${tts.speech-rate:0}")
    private int speechRate;

    @Value("${tts.pitch-rate:0}")
    private int pitchRate;

    /** NLS 网关地址（与 ASR 共用） */
    private static final String NLS_GATEWAY = "nls-gateway-cn-beijing.aliyuncs.com";
    private static final String NLS_PATH = "/ws/v1";

    /** Token 服务地址（与 ASR 共用） */
    private static final String TOKEN_DOMAIN = "nls-meta.cn-shanghai.aliyuncs.com";
    private static final String TOKEN_ACTION = "CreateToken";
    private static final String TOKEN_VERSION = "2019-02-28";
    private static final String TOKEN_REGION_ID = "cn-shanghai";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** 缓存的 Token 及其过期时间（与 ASR 独立缓存，实际可共用） */
    private volatile String cachedToken;
    private volatile long tokenExpireTime; // 秒级时间戳

    public AliyunTtsService() {
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // TTS 可能较长时间无消息
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("AliyunTtsService 初始化完成, voice={}, format={}, sampleRate={}",
                voice, format, sampleRate);
        if (accessKeyId == null || accessKeyId.isBlank() || "your-access-key-id".equals(accessKeyId)) {
            log.warn("阿里云 TTS AccessKey 未配置！TTS 功能将无法使用。请在 application.yml 中配置 tts.*");
        }
    }

    // ========== TtsService 接口实现 ==========

    /**
     * 流式文本转语音
     *
     * 流程：
     * 1. 获取 Token
     * 2. 建立 NLS WebSocket 连接
     * 3. 发送 StartSynthesis 指令
     * 4. 发送文本数据
     * 5. 接收二进制音频帧 → 回调 onAudioData()
     * 6. 收到 SynthesisCompleted → 回调 onComplete()
     */
    @Override
    public void synthesizeStream(String text, int speechRate, TtsStreamListener listener) {
        try {
            // Step 1: 获取 Token
            String token = getToken();

            // Step 2: 构建 WebSocket URL
            String url = String.format("wss://%s%s?token=%s", NLS_GATEWAY, NLS_PATH, token);

            // Step 3: 建立连接并发送指令
            Request request = new Request.Builder().url(url).build();

            OkHttpClient.Builder builder = httpClient.newBuilder();
            builder.readTimeout(60, TimeUnit.SECONDS); // 单次合成超时

            okhttp3.WebSocket ws = builder.build().newWebSocket(request, new TtsWebSocketListener(text, speechRate, listener));
            log.info("[TTS] 开始合成, 文本长度={}, speechRate={}, url={}", text.length(), speechRate, url);
        } catch (Exception e) {
            log.error("[TTS] 合成启动失败", e);
            listener.onError("TTS 启动失败: " + e.getMessage());
        }
    }

    // ========== Token 管理（与 ASR 相同逻辑）==========

    private synchronized String getToken() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now < tokenExpireTime - 300) {
            log.debug("[TTS-Token] 使用缓存的 Token");
            return cachedToken;
        }
        log.info("[TTS-Token] 正在获取新的 Token...");
        String token = fetchTokenFromApi();
        cachedToken = token;
        return token;
    }

    private String fetchTokenFromApi() throws IOException {
        String timestamp = formatUtcTimestamp();
        String nonce = UUID.randomUUID().toString();

        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", TOKEN_ACTION);
        params.put("Format", "JSON");
        params.put("RegionId", TOKEN_REGION_ID);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", nonce);
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", timestamp);
        params.put("Version", TOKEN_VERSION);

        String signature = computePopSignature(params, "POST");

        StringBuilder bodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (bodyBuilder.length() > 0) bodyBuilder.append("&");
            bodyBuilder.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
        }
        bodyBuilder.append("&Signature=").append(urlEncode(signature));

        RequestBody requestBody = RequestBody.create(bodyBuilder.toString(),
                MediaType.parse("application/x-www-form-urlencoded"));
        Request request = new Request.Builder()
                .url("https://" + TOKEN_DOMAIN + "/")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CreateToken HTTP 失败: " + response.code());
            }
            JsonNode json = objectMapper.readTree(response.body().string());
            JsonNode tokenObj = json.path("Token");
            if (tokenObj.isMissingNode()) {
                throw new IOException("CreateToken 响应格式异常");
            }
            String tokenId = tokenObj.path("Id").asText("");
            long expireTime = tokenObj.path("ExpireTime").asLong(0);
            if (tokenId.isEmpty()) {
                throw new IOException("CreateToken 返回空 Token");
            }
            tokenExpireTime = expireTime;
            log.info("[TTS-Token] 新 Token 已获取");
            return tokenId;
        }
    }

    private String computePopSignature(Map<String, String> params, String httpMethod) throws IOException {
        try {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > 0) queryString.append("&");
                queryString.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            }
            String stringToSign = httpMethod +
                    "&" + urlEncode("/") +
                    "&" + urlEncode(queryString.toString());

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IOException("POP 签名计算失败", e);
        }
    }

    private static String formatUtcTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return sdf.format(new Date());
    }

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

    // ========== WebSocket 监听器 ==========

    /**
     * TTS WebSocket 消息监听器
     *
     * 处理 NLS 语音合成（免费版 SpeechSynthesizer）的完整生命周期：
     * onOpen → StartSynthesis(文本直接放 payload.text) → 接收二进制音频帧 → SynthesisCompleted → 关闭
     *
     * 与商用版 FlowingSpeechSynthesizer 的区别：
     * - namespace = "SpeechSynthesizer"（免费额度支持，1万字符/月）
     * - 文本直接放在 StartSynthesis 的 payload.text 中，不需要 RunSynthesis
     * - 不支持流式追加文本（适合我们：LLM 完成后一次性合成）
     */
    private class TtsWebSocketListener extends okhttp3.WebSocketListener {

        private final String text;
        private final int speechRate;
        private final TtsStreamListener listener;
        /** 整个会话共享的 task_id */
        private final String taskId;

        TtsWebSocketListener(String text, int speechRate, TtsStreamListener listener) {
            this.text = text;
            this.speechRate = speechRate;
            this.listener = listener;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, Response response) {
            log.info("[TTS] WebSocket 已连接, taskId={}", taskId);

            try {
                // 发送 StartSynthesis 指令（文本直接放在 payload 里）
                Map<String, Object> header = new LinkedHashMap<>();
                header.put("message_id", UUID.randomUUID().toString().replace("-", ""));
                header.put("task_id", taskId);
                header.put("namespace", "SpeechSynthesizer");  // ★ 免费版 namespace
                header.put("name", "StartSynthesis");
                header.put("appkey", appKey);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("text", text);          // ★ 文本直接放这里，不需要 RunSynthesis
                payload.put("voice", voice);
                payload.put("format", format);
                payload.put("sample_rate", sampleRate);
                payload.put("volume", volume);
                payload.put("speech_rate", speechRate);
                payload.put("pitch_rate", pitchRate);

                Map<String, Object> status = new LinkedHashMap<>();
                status.put("code", 20000000);
                status.put("message", "SUCCESS");

                Map<String, Object> command = new LinkedHashMap<>();
                command.put("header", header);
                command.put("payload", payload);
                command.put("status", status);

                String json = objectMapper.writeValueAsString(command);
                log.info("[TTS] 发送 StartSynthesis (文本长度={}):\n{}", text.length(), json);
                webSocket.send(json);
            } catch (Exception e) {
                log.error("[TTS] 发送 StartSynthesis 失败", e);
                listener.onError("发送指令失败: " + e.getMessage());
            }
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String textMessage) {
            try {
                JsonNode root = objectMapper.readTree(textMessage);
                String name = root.path("header").path("name").asText("");
                int statusCode = root.path("status").path("code").asInt(0);
                String statusMsg = root.path("status").path("message").asText("");

                log.info("[TTS] 收到: name={}, code={}, msg={}", name, statusCode, statusMsg);

                switch (name) {
                    case "SynthesisStarted":
                        log.info("[TTS] 合成已启动, 等待音频数据...");
                        break;

                    case "SynthesisCompleted":
                        log.info("[TTS] 合成完成");
                        listener.onComplete();
                        webSocket.close(1000, "Normal closure");
                        break;

                    case "TaskFailed":
                        String errorMsg = statusMsg.isEmpty() ? "Unknown error" : statusMsg;
                        log.error("[TTS] 任务失败: code={}, msg={}, 完整响应:\n{}",
                                statusCode, errorMsg, textMessage);
                        listener.onError("TTS 错误[" + statusCode + "]: " + errorMsg);
                        webSocket.close(1000, "TaskFailed");
                        break;

                    default:
                        if (statusCode != 20000000) {
                            String err = statusMsg.isEmpty() ? "Unknown" : statusMsg;
                            log.warn("[TTS] 非成功响应: name={}, code={}, msg={}", name, statusCode, err);
                        }
                        break;
                }
            } catch (Exception e) {
                log.error("[TTS] 处理文本消息异常: {}", textMessage, e);
            }
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
            // 收到二进制音频帧（PCM 数据）
            byte[] audioData = bytes.toByteArray();
            log.debug("[TTS] 收到音频帧: {} bytes", audioData.length);
            listener.onAudioData(audioData);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
            log.error("[TTS] WebSocket 异常: {}", t.getMessage(), t);
            listener.onError("TTS 连接失败: " + t.getMessage());
        }

        @Override
        public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
            log.info("[TTS] WebSocket 已关闭: code={}, reason={}", code, reason);
        }
    }
}
