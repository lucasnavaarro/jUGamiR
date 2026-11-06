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
    private final org.springframework.scheduling.TaskScheduler heartbeatScheduler;
    public WebSocketConfig(StompSecurity security, @Value("${app.url}") String origin,
            @org.springframework.beans.factory.annotation.Qualifier("brokerHeartbeatScheduler") org.springframework.scheduling.TaskScheduler heartbeatScheduler) {
        this.security = security;
        this.heartbeatScheduler = heartbeatScheduler;
        this.origin = java.net.URI.create(origin).getScheme() + "://" + java.net.URI.create(origin).getRawAuthority();
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(origin);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic").setTaskScheduler(heartbeatScheduler).setHeartbeatValue(new long[]{10000, 10000});
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(security); }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) { registration.interceptors(security.outbound()); }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16 * 1024).setSendBufferSizeLimit(64 * 1024).setTimeToFirstMessage(10_000);
    }

    @org.springframework.context.annotation.Bean("brokerHeartbeatScheduler")
    static org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler brokerHeartbeatScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    @org.springframework.context.annotation.Bean("taskScheduler")
    static org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler deadlineScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("app-scheduled-");
        scheduler.setDaemon(true);
        return scheduler;
    }
}
