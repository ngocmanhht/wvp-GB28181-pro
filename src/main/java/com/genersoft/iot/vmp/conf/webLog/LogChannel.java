package com.genersoft.iot.vmp.conf.webLog;

import lombok.extern.slf4j.Slf4j;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint(value = "/channel/log")
@Slf4j
public class LogChannel {

    public static final ConcurrentMap<String, LogChannel> CHANNELS = new ConcurrentHashMap<>();

    private Session session;

    @OnMessage(maxMessageSize = 1) // MaxMessage 1 byte
    public void onMessage(String message) {

        try {
            this.session.close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "This node does not receive any client information"));
        } catch (IOException e) {
            log.error("[Web-Log] Connection close failed: id={}, err={}", this.session.getId(), e.getMessage());
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        this.session.setMaxIdleTimeout(0);
        CHANNELS.put(this.session.getId(), this);

        log.info("[Web-Log] Connection established: id={}", this.session.getId());
    }

    @OnClose
    public void onClose(CloseReason closeReason) {

        log.info("[Web-Log] The connection has been lost: id={}, err={}", this.session.getId(), closeReason);
        CHANNELS.remove(this.session.getId());
    }

    @OnError
    public void onError(Throwable throwable) throws IOException {
        log.info("[Web-Log] Connection error: id={}, err= {}", this.session.getId(), throwable.getMessage());
        if (this.session.isOpen()) {
            this.session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
        }
    }

    /**
     * Push messages to all clients
     *
     * @param message
     */
    public static void push(String message) {
        CHANNELS.values().stream().forEach(endpoint -> {
            if (endpoint.session.isOpen()) {
                endpoint.session.getAsyncRemote().sendText(message);
            }
        });
    }
}
