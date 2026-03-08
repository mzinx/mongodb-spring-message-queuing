package com.mzinx.mongodb.messaging.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@ConfigurationProperties("messaging")
@Component
public class MessagingProperties {
    private boolean enabled = true;
    private String collection = "_messages";    
    private String webSocketEndpoint = "/ws";
    private String pushPath = "/push";
    private String commandPath = "/cmd";
    private String syncPath = "/sync";
    private long maxLifeTime = 86400000;
    private List<String> watchCollections = new ArrayList<>();
}
