package com.mzinx.mongodb.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.mzinx.mongodb.messaging")
@Import({ AutoConfigurationPackageRegistrar.class, MessagingHeartbeatSchedulerConfig.class })
@EnableWebSocketMessageBroker
public class MessagingAutoConfig implements WebSocketMessageBrokerConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MessagingProperties messagingProperties;

    // Lazily resolved so this configurer doesn't hard-depend on the scheduler
    // during context wiring (avoids a bean-creation cycle with the delegating
    // WebSocket broker configuration). The scheduler bean is defined in the
    // separate MessagingHeartbeatSchedulerConfig.
    private final ObjectProvider<TaskScheduler> heartbeatScheduler;

    MessagingAutoConfig(
            @Qualifier("messagingHeartbeatScheduler") ObjectProvider<TaskScheduler> heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable the simple broker with NO destination prefixes so it keeps
        // matching every destination (the queue broadcasts to configurable paths
        // such as /cmd and /sync, plus app-defined ones like /private/<id>), which
        // preserves the prior behavior of relying on Spring's implicit default
        // broker. The only functional addition is heartbeats.
        SimpleBrokerRegistration broker = config.enableSimpleBroker();

        MessagingProperties.Heartbeat hb = messagingProperties.getHeartbeat();
        TaskScheduler scheduler = hb.isEnabled() ? heartbeatScheduler.getIfAvailable() : null;
        if (scheduler != null) {
            broker.setHeartbeatValue(new long[] { hb.getServerMs(), hb.getClientMs() })
                  .setTaskScheduler(scheduler);
            logger.info("STOMP heartbeats enabled (server={}ms, client={}ms)",
                    hb.getServerMs(), hb.getClientMs());
        } else {
            logger.info("STOMP heartbeats disabled");
        }
        // config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(messagingProperties.getWebSocketEndpoint()).setAllowedOriginPatterns("*");
        registry.addEndpoint(messagingProperties.getWebSocketEndpoint()).setAllowedOriginPatterns("*").withSockJS();
    }

}