package com.mzinx.mongodb.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Provides the {@link TaskScheduler} used to deliver STOMP broker heartbeats.
 * <p>
 * Kept in a <em>separate</em> configuration class from {@code MessagingAutoConfig}
 * on purpose: {@code MessagingAutoConfig} is a
 * {@link org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer},
 * and Spring's {@code DelegatingWebSocketMessageBrokerConfiguration} depends on
 * every configurer. If the scheduler {@code @Bean} lived on the configurer, that
 * configurer would depend on a bean it defines itself, producing a bean-creation
 * cycle. Defining the scheduler here breaks that cycle — the configurer only
 * injects it.
 * <p>
 * The bean is created only when the application hasn't already supplied a
 * {@code TaskScheduler} named {@code messagingHeartbeatScheduler}, so an app can
 * provide its own scheduler to reuse. It is wired into the broker only when
 * heartbeats are enabled (see {@code MessagingAutoConfig#configureMessageBroker});
 * the extra idle thread when heartbeats are off is negligible.
 */
@Configuration(proxyBeanMethods = false)
public class MessagingHeartbeatSchedulerConfig {

    @Bean
    @ConditionalOnMissingBean(name = "messagingHeartbeatScheduler")
    TaskScheduler messagingHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("mq-ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
