package com.example.demo.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
    private String allowedOrigins;

    @Value("${demo.websocket.broker.mode:simple}")
    private String brokerMode;

    @Value("${demo.websocket.simple-broker.heartbeat-ms:10000}")
    private long simpleBrokerHeartbeatMs;

    @Value("${demo.websocket.relay.host:localhost}")
    private String relayHost;

    @Value("${demo.websocket.relay.port:61613}")
    private Integer relayPort;

    @Value("${demo.websocket.relay.client-login:guest}")
    private String relayClientLogin;

    @Value("${demo.websocket.relay.client-passcode:guest}")
    private String relayClientPasscode;

    @Value("${demo.websocket.relay.system-login:guest}")
    private String relaySystemLogin;

    @Value("${demo.websocket.relay.system-passcode:guest}")
    private String relaySystemPasscode;

    @Value("${demo.websocket.relay.virtual-host:/}")
    private String relayVirtualHost;

    @Value("${demo.websocket.transport.message-size-limit-bytes:65536}")
    private Integer messageSizeLimitBytes;

    @Value("${demo.websocket.transport.send-time-limit-ms:15000}")
    private Integer sendTimeLimitMs;

    @Value("${demo.websocket.transport.send-buffer-size-limit-bytes:524288}")
    private Integer sendBufferSizeLimitBytes;

    private final WebSocketJwtHandshakeInterceptor webSocketJwtHandshakeInterceptor;

    public WebSocketConfig(WebSocketJwtHandshakeInterceptor webSocketJwtHandshakeInterceptor) {
        this.webSocketJwtHandshakeInterceptor = webSocketJwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("relay".equalsIgnoreCase(brokerMode)) {
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayClientLogin)
                    .setClientPasscode(relayClientPasscode)
                    .setSystemLogin(relaySystemLogin)
                    .setSystemPasscode(relaySystemPasscode)
                    .setVirtualHost(relayVirtualHost)
                    .setTaskScheduler(webSocketBrokerTaskScheduler())
                    .setSystemHeartbeatSendInterval(simpleBrokerHeartbeatMs)
                    .setSystemHeartbeatReceiveInterval(simpleBrokerHeartbeatMs);
        } else {
            registry.enableSimpleBroker("/topic")
                    .setHeartbeatValue(new long[]{simpleBrokerHeartbeatMs, simpleBrokerHeartbeatMs})
                    .setTaskScheduler(webSocketBrokerTaskScheduler());
        }
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(messageSizeLimitBytes);
        registry.setSendTimeLimit(sendTimeLimitMs);
        registry.setSendBufferSizeLimit(sendBufferSizeLimitBytes);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .addInterceptors(webSocketJwtHandshakeInterceptor)
            .setAllowedOriginPatterns(parseAllowedOrigins())
            .withSockJS()
            .setStreamBytesLimit(sendBufferSizeLimitBytes)
            .setHttpMessageCacheSize(512)
            .setDisconnectDelay(30_000);
    }

    @Bean
    public TaskScheduler webSocketBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ws-broker-");
        scheduler.setPoolSize(4);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }
}
