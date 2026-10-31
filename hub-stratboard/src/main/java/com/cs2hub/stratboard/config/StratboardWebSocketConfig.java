package com.cs2hub.stratboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StratboardWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "https://all-in-one-cs2.vercel.app",
            "https://*.vercel.app",
            "http://localhost:3000",
            "http://localhost:5173",
            "*"
    };

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Direct WebSocket endpoint
        registry.addEndpoint("/ws/stratboard")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);

        // SockJS fallback endpoint
        registry.addEndpoint("/ws/stratboard")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                .withSockJS();
    }
}
