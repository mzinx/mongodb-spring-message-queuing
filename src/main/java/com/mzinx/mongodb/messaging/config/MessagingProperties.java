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

    /** STOMP broker heartbeat settings. */
    private Heartbeat heartbeat = new Heartbeat();

    /**
     * STOMP heartbeat configuration for the simple broker. Heartbeats let the
     * server detect a dead connection (e.g. an intermittent network drop where no
     * clean close is received) within roughly one interval, instead of relying on
     * OS-level TCP timeouts. This is what makes WebSocket {@code SessionDisconnectEvent}
     * fire promptly for non-clean disconnects.
     */
    @Data
    public static class Heartbeat {
        /**
         * How often (ms) the server sends a heartbeat to clients. {@code 0}
         * disables server-to-client heartbeats.
         *
         * <p>Kept relatively relaxed (25s) so transient jitter — network hiccups,
         * a paused/throttled client tab, GC stalls — is not mistaken for a dead
         * connection and does not trigger a needless disconnect/reconnect cycle
         * (which drops any events broadcast during the gap). Must stay aligned
         * with the client's {@code heartbeatIncoming}.
         */
        private long serverMs = 25000;

        /**
         * How often (ms) the server expects a heartbeat from clients. {@code 0}
         * disables the client-to-server expectation. A client that stops sending
         * within this window has its STOMP session closed. Must stay aligned with
         * the client's {@code heartbeatOutgoing}.
         */
        private long clientMs = 25000;

        /** {@code true} when either direction is enabled (non-zero). */
        public boolean isEnabled() {
            return this.serverMs > 0 || this.clientMs > 0;
        }
    }
}
