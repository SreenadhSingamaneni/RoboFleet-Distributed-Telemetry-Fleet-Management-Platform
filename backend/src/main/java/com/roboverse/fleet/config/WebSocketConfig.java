package com.roboverse.fleet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final FleetProperties properties;

    public WebSocketConfig(FleetProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        var websocket = properties.websocket();
        if (websocket.relayEnabled()) {
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(websocket.relayHost())
                    .setRelayPort(websocket.relayPort())
                    .setClientLogin(websocket.relayLogin())
                    .setClientPasscode(websocket.relayPasscode())
                    .setSystemLogin(websocket.relayLogin())
                    .setSystemPasscode(websocket.relayPasscode());
        } else {
            registry.enableSimpleBroker("/topic");
        }
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(properties.security().allowedOrigins().toArray(String[]::new));
    }
}

