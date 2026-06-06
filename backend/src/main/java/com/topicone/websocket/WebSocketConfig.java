package com.topicone.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置类
 *
 * 注册原生 WebSocket 端点（/voice），用于实时语音对话，支持二进制 PCM 数据
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceWebSocketHandler voiceWebSocketHandler;

    public WebSocketConfig(VoiceWebSocketHandler voiceWebSocketHandler) {
        this.voiceWebSocketHandler = voiceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端连接: ws://localhost:8080/voice?conversationId=xxx
        registry.addHandler(voiceWebSocketHandler, "/voice")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new VoiceHandshakeInterceptor());
    }

    /**
     * 握手拦截器：从 URL 参数提取 conversationId 等参数到 session attributes
     */
    public static class VoiceHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                       org.springframework.http.server.ServerHttpResponse response,
                                       org.springframework.web.socket.WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
            if (request.getURI().getQuery() != null) {
                for (String param : request.getURI().getQuery().split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2) {
                        attributes.put(kv[0], kv[1]);
                    }
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Exception exception) {
            // 握手后无需处理
        }
    }
}
