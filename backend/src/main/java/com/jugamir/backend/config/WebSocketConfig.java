package com.jugamir.backend.config;

import com.jugamir.backend.security.StompSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompSecurity security;
    private final String origin;
    public WebSocketConfig(StompSecurity security, @Value("${app.url}") String origin) {
        this.security = security;
        this.origin = java.net.URI.create(origin).getScheme() + "://" + java.net.URI.create(origin).getRawAuthority();
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(origin);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(security); }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) { registration.interceptors(security.outbound()); }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16 * 1024).setSendBufferSizeLimit(64 * 1024).setTimeToFirstMessage(10_000);
    }
}
