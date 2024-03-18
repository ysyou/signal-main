package com.clamos.signal.config;

import com.clamos.signal.handler.WebSocketDeviceHandler;
import com.clamos.signal.handler.WebSocketWebHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer  {

    private final WebSocketDeviceHandler webSocketDeviceHandler;
    private final WebSocketWebHandler webSocketWebHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketDeviceHandler,"/device").setAllowedOrigins("*");
        registry.addHandler(webSocketWebHandler,"/web").setAllowedOrigins("*");
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub");
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/status").setAllowedOrigins("*");
        registry.addEndpoint("/status").setAllowedOrigins("*").withSockJS().setHeartbeatTime(10_000);
    }
}
