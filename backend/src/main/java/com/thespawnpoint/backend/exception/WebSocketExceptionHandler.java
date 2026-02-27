package com.thespawnpoint.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;

import java.util.Map;

@Slf4j
public abstract class WebSocketExceptionHandler {

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public Map<String, Object> handleApiException(ApiException ex) {
        log.warn("WS ApiException: {}", ex.getMessage());
        return Map.of("status", ex.getStatus().value(), "message", ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, Object> handleException(Exception ex) {
        log.error("WS unexpected error", ex);
        return Map.of("status", 500, "message", "Internal server error");
    }
}
