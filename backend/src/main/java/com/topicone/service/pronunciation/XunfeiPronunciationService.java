package com.topicone.service.pronunciation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topicone.dto.pronunciation.PronunciationResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 讯飞语音评测（流式版）服务实现
 *
 * 协议：WSS 握手 + JSON 帧（ssb→auw音频上传→结果返回）
 * 文档：eng.md — 讯飞开放平台 ISE API
 *
 * 流程（单句评测）：
 *   1. 生成带签名的 WS URL（HMAC-SHA256）
 *   2. WS 连接成功 → 发送 ssb 帧（参数上传）
 *   3. 逐帧发送 base64 音频（auw, 每帧 ≤1280B）
 *   4. 发送最后一帧（aus=4, status=2）
 *   5. 解析返回的 XML 结果 → PronunciationResult
 */
@Slf4j
@Service
public class XunfeiPronunciationService implements PronunciationService {

    private static final String HOST = "ise-api.xfyun.cn";
    private static final String WS_URL = "wss://ise-api.xfyun.cn/v2/open-ise";
    private static final String PATH = "/v2/open-ise";

    /** 每帧音频字节数（建议 1280B ≈ 40ms PCM） */
    private static final int FRAME_SIZE = 1280;

    @Value("${xunfei.app-id:}")
    private String appId;

    @Value("${xunfei.api-key:}")
    private String apiKey;

    @Value("${xunfei.api-secret:}")
    private String apiSecret;

    @Value("${xunfei.mock:true}")
    private boolean mock;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // ================================================================
    // 公开接口
    // ================================================================

    @Override
    public void evaluate(byte[] pcmAudio, String refText,
                         Consumer<PronunciationResult> onComplete,
                         Consumer<String> onError) {
        if (mock) {
            onComplete.accept(buildMockResult(refText));
            return;
        }
        if (!checkConfig(onError)) return;

        String wsUrl = buildAuthUrl();
        if (wsUrl == null) {
            onError.accept("构建鉴权 URL 失败");
            return;
        }

        XfEvaluator evaluator = new XfEvaluator(pcmAudio, refText, onComplete, onError);
        httpClient.newWebSocket(new Request.Builder().url(wsUrl).build(), evaluator);
    }

    @Override
    public void evaluateBatch(List<UserUtterance> utterances,
                              Consumer<List<PronunciationResult>> onComplete,
                              Consumer<String> onError) {
        if (utterances == null || utterances.isEmpty()) {
            onComplete.accept(Collections.emptyList());
            return;
        }

        if (mock) {
            onComplete.accept(utterances.stream()
                    .map(u -> buildMockResult(u.text()))
                    .toList());
            return;
        }
        if (!checkConfig(onError)) return;

        evaluateBatchSequential(utterances, 0, new ArrayList<>(), onComplete, onError);
    }

    private void evaluateBatchSequential(List<UserUtterance> utterances, int index,
                                         List<PronunciationResult> accum,
                                         Consumer<List<PronunciationResult>> onComplete,
                                         Consumer<String> onError) {
        if (index >= utterances.size()) {
            onComplete.accept(accum);
            return;
        }
        UserUtterance u = utterances.get(index);
        evaluate(u.pcmAudio(), u.text(),
                result -> {
                    accum.add(result);
                    evaluateBatchSequential(utterances, index + 1, accum, onComplete, onError);
                },
                error -> {
                    log.warn("[讯飞评测] 第{}句评测失败: {}", index + 1, error);
                    evaluateBatchSequential(utterances, index + 1, accum, onComplete, onError);
                });
    }

    // ================================================================
    // 鉴权 URL 构建（HMAC-SHA256）
    // ================================================================

    private boolean checkConfig(Consumer<String> onError) {
        if (isBlank(appId) || isBlank(apiKey) || isBlank(apiSecret)) {
            onError.accept("讯飞评测未配置 app-id/api-key/api-secret");
            return false;
        }
        return true;
    }

    /**
     * 构建带签名的 WS URL
     * eng.md 规范：authorization = base64(api_key="...", algorithm="hmac-sha256",
     *             headers="host date request-line", signature="...")
     */
    private String buildAuthUrl() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = sdf.format(new Date());

            // signature_origin = "host: $host\ndate: $date\nGET $path HTTP/1.1"
            String signatureOrigin = "host: " + HOST + "\n" +
                                     "date: " + date + "\n" +
                                     "GET " + PATH + " HTTP/1.1";

            // HMAC-SHA256(apiSecret, signature_origin) → base64 → signature
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));

            // authorization_origin
            String authOrigin = String.format(
                    "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", signature);

            // authorization = base64(authorization_origin)
            String authorization = Base64.getEncoder().encodeToString(
                    authOrigin.getBytes(StandardCharsets.UTF_8));

            return WS_URL + "?authorization=" + authorization +
                   "&host=" + HOST +
                   "&date=" + urlEncode(date);
        } catch (Exception e) {
            log.error("[讯飞评测] 构建鉴权 URL 失败: {}", e.getMessage());
            return null;
        }
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    // ================================================================
    // WebSocket 评测器
    // ================================================================

    private class XfEvaluator extends WebSocketListener {
        private final byte[] pcmAudio;
        private final String refText;
        private final Consumer<PronunciationResult> onComplete;
        private final Consumer<String> onError;
        private boolean ssbSent;
        private int audioChunkOffset;

        XfEvaluator(byte[] pcmAudio, String refText,
                    Consumer<PronunciationResult> onComplete, Consumer<String> onError) {
            this.pcmAudio = pcmAudio;
            this.refText = refText;
            this.onComplete = onComplete;
            this.onError = onError;
        }

        @Override
        public void onOpen(@NotNull WebSocket ws, @NotNull Response response) {
            sendSsb(ws);
        }

        @Override
        public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
            try {
                JsonNode root = objectMapper.readTree(text);
                int code = root.path("code").asInt(-1);

                if (code != 0) {
                    String msg = root.path("message").asText("未知错误");
                    onError.accept("讯飞评测错误(code=" + code + "): " + msg);
                    ws.close(1000, "评测失败");
                    return;
                }

                // 解析结果：data.data 是 base64 编码的 XML
                JsonNode dataNode = root.path("data");
                int status = dataNode.path("status").asInt(0);
                String b64Xml = dataNode.path("data").asText("");

                if (status == 2 && !b64Xml.isEmpty()) {
                    PronunciationResult result = parseXmlResult(refText, b64Xml);
                    onComplete.accept(result);
                    ws.close(1000, "评测完成");
                } else if (status == 1) {
                    // 中间结果，部分得分可能已返回
                    log.debug("[讯飞评测] 收到中间结果, status=1");
                }
            } catch (Exception e) {
                log.warn("[讯飞评测] 消息解析异常: {}", e.getMessage());
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket ws, @NotNull Throwable t, @Nullable Response r) {
            log.error("[讯飞评测] WS 连接失败: {}", t.getMessage());
            onError.accept("讯飞评测连接失败: " + t.getMessage());
        }

        // ---- 发送帧 ----

        private void sendSsb(WebSocket ws) {
            try {
                Map<String, Object> common = new LinkedHashMap<>();
                common.put("app_id", appId);

                Map<String, Object> business = new LinkedHashMap<>();
                business.put("cmd", "ssb");
                business.put("sub", "ise");
                business.put("ent", "en_vip");
                business.put("category", "read_sentence");
                business.put("text", "\uFEFF" + refText);  // UTF-8 BOM
                business.put("tte", "utf-8");
                business.put("ttp_skip", true);
                business.put("aue", "raw");
                business.put("auf", "audio/L16;rate=16000");
                business.put("rstcd", "utf8");
                business.put("rst", "entirety");
                business.put("ise_unite", "1");
                business.put("extra_ability", "multi_dimension");

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", 0);

                Map<String, Object> frame = new LinkedHashMap<>();
                frame.put("common", common);
                frame.put("business", business);
                frame.put("data", data);

                ws.send(objectMapper.writeValueAsString(frame));
                ssbSent = true;
                log.debug("[讯飞评测] ssb 帧已发送, text={}", refText);

                // ssb 发送后立即开始发音频
                sendAudioChunk(ws);
            } catch (Exception e) {
                onError.accept("发送 ssb 帧失败: " + e.getMessage());
            }
        }

        private void sendAudioChunk(WebSocket ws) {
            if (audioChunkOffset >= pcmAudio.length) {
                sendAudioFinal(ws);
                return;
            }

            int end = Math.min(audioChunkOffset + FRAME_SIZE, pcmAudio.length);
            byte[] chunk = new byte[end - audioChunkOffset];
            System.arraycopy(pcmAudio, audioChunkOffset, chunk, 0, chunk.length);

            int aus = (audioChunkOffset == 0) ? 1 : 2;
            audioChunkOffset = end;

            try {
                Map<String, Object> business = new LinkedHashMap<>();
                business.put("cmd", "auw");
                business.put("aus", aus);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", 1);
                data.put("data", Base64.getEncoder().encodeToString(chunk));

                Map<String, Object> frame = new LinkedHashMap<>();
                frame.put("business", business);
                frame.put("data", data);

                ws.send(objectMapper.writeValueAsString(frame));

                // 继续发下一帧
                if (audioChunkOffset < pcmAudio.length) {
                    sendAudioChunk(ws);
                } else {
                    sendAudioFinal(ws);
                }
            } catch (Exception e) {
                onError.accept("发送音频帧失败: " + e.getMessage());
            }
        }

        private void sendAudioFinal(WebSocket ws) {
            try {
                Map<String, Object> business = new LinkedHashMap<>();
                business.put("cmd", "auw");
                business.put("aus", 4);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", 2);
                data.put("data", "");

                Map<String, Object> frame = new LinkedHashMap<>();
                frame.put("business", business);
                frame.put("data", data);

                ws.send(objectMapper.writeValueAsString(frame));
                log.debug("[讯飞评测] 音频最后一帧已发送 (aus=4, status=2)");
            } catch (Exception e) {
                onError.accept("发送结束帧失败: " + e.getMessage());
            }
        }
    }

    // ================================================================
    // XML 结果解析
    // ================================================================

    /**
     * 解析讯飞返回的 XML 结果（base64 → XML → PronunciationResult）
     *
     * 讯飞返回格式（eng.md 中文评测部分）：
     *   <read_sentence>
     *     <rec_paper>
     *       <read_sentence total_score="92.51" accuracy_score="100.00"
     *           fluency_score="87.62" integrity_score="100.00" ...>
     *         <sentence ...>
     *           <word content="text" total_score="86.96" ...>
     *             <syll ...>
     *               <phone .../>
     *             </syll>
     *           </word>
     *         </sentence>
     *       </read_sentence>
     *     </rec_paper>
     *   </read_sentence>
     */
    private PronunciationResult parseXmlResult(String refText, String b64Xml) {
        try {
            byte[] xmlBytes = Base64.getDecoder().decode(b64Xml);
            String xml = new String(xmlBytes, StandardCharsets.UTF_8);

            double overall = extractDouble(xml, "total_score");
            double accuracy = extractDouble(xml, "accuracy_score");
            double fluency = extractDouble(xml, "fluency_score");
            double integrity = extractDouble(xml, "integrity_score");
            double phoneScore = extractDouble(xml, "phone_score");

            // 单词详情
            List<PronunciationResult.WordDetail> wordDetails = new ArrayList<>();
            String[] wordParts = xml.split("<word ");
            for (int i = 1; i < wordParts.length; i++) {
                String wp = wordParts[i];
                String word = extractAttr(wp, "content");
                Double wScore = extractDoubleAttr(wp, "total_score");
                if (wScore == null) continue;

                List<PronunciationResult.PhonemeDetail> phonemes = new ArrayList<>();
                String[] phoneParts = wp.split("<phone ");
                for (int j = 1; j < phoneParts.length; j++) {
                    String pp = phoneParts[j];
                    String ph = extractAttr(pp, "content");
                    Double phScore = extractDoubleAttr(pp, "phone_score");
                    if (ph == null || ph.isEmpty()) continue;
                    phonemes.add(PronunciationResult.PhonemeDetail.builder()
                            .phoneme(ph).score(phScore != null ? phScore : 0)
                            .hasError(false).build());
                }

                wordDetails.add(PronunciationResult.WordDetail.builder()
                        .word(word).score(wScore).phonemes(phonemes).build());
            }

            return PronunciationResult.builder()
                    .refText(refText)
                    .overallScore(overall > 0 ? overall : null)
                    .accuracyScore(accuracy > 0 ? accuracy : null)
                    .fluencyScore(fluency > 0 ? fluency : null)
                    .integrityScore(integrity > 0 ? integrity : null)
                    .wordDetails(wordDetails)
                    .build();
        } catch (Exception e) {
            log.error("[讯飞评测] XML 解析失败: {}", e.getMessage());
            return PronunciationResult.builder().refText(refText).build();
        }
    }

    private double extractDouble(String xml, String attr) {
        int idx = xml.indexOf(attr + "=\"");
        if (idx < 0) return 0;
        int start = idx + attr.length() + 2;
        int end = xml.indexOf("\"", start);
        if (end < 0) return 0;
        try { return Double.parseDouble(xml.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private Double extractDoubleAttr(String xml, String attr) {
        double v = extractDouble(xml, attr);
        return v > 0 || xml.contains(attr + "=\"") ? v : null;
    }

    private String extractAttr(String xml, String attr) {
        int idx = xml.indexOf(attr + "=\"");
        if (idx < 0) return "";
        int start = idx + attr.length() + 2;
        int end = xml.indexOf("\"", start);
        if (end < 0) return "";
        return xml.substring(start, end);
    }

    // ================================================================
    // Mock
    // ================================================================

    private PronunciationResult buildMockResult(String refText) {
        String[] words = refText.split("\\s+");
        double avgScore = 70 + Math.random() * 20;

        List<PronunciationResult.WordDetail> wordDetails = new ArrayList<>();
        for (String w : words) {
            w = w.replaceAll("[.,!?;:]", "");
            if (w.isBlank()) continue;
            wordDetails.add(PronunciationResult.WordDetail.builder()
                    .word(w).score(65 + Math.random() * 30)
                    .phonemes(mockPhonemes(w)).build());
        }

        return PronunciationResult.builder()
                .refText(refText)
                .overallScore(avgScore)
                .accuracyScore(avgScore - 3 + Math.random() * 6)
                .fluencyScore(avgScore - 5 + Math.random() * 10)
                .integrityScore(avgScore - 2 + Math.random() * 4)
                .wordDetails(wordDetails)
                .build();
    }

    private List<PronunciationResult.PhonemeDetail> mockPhonemes(String word) {
        List<PronunciationResult.PhonemeDetail> list = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            list.add(PronunciationResult.PhonemeDetail.builder()
                    .phoneme(String.valueOf(word.charAt(i)))
                    .score(60 + Math.random() * 35)
                    .hasError(Math.random() < 0.2).build());
        }
        return list;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
