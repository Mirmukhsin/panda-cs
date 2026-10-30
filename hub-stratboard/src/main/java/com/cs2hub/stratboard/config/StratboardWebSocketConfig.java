package com.cs2hub.stratboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StratboardWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Direct WebSocket endpoint
        registry.addEndpoint("/ws/stratboard")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:5173", "*");

        // SockJS fallback endpoint
        registry.addEndpoint("/ws/stratboard")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:5173", "*")
                .withSockJS();
    }
}
