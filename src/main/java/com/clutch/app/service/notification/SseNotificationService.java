package com.clutch.app.service.notification;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String importId) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes

        emitter.onCompletion(() -> emitters.remove(importId));
        emitter.onTimeout(() -> emitters.remove(importId));
        emitter.onError((e) -> emitters.remove(importId));

        emitters.put(importId, emitter);
        return emitter;
    }

    public void sendProgress(String importId, int processed, int total) {
        SseEmitter emitter = emitters.get(importId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("processed", processed, "total", total)));
            } catch (IOException e) {
                emitters.remove(importId);
            }
        }
    }

    public void sendComplete(String importId) {
        SseEmitter emitter = emitters.get(importId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("complete").data("Done"));
                emitter.complete();
            } catch (IOException ignored) {}
            emitters.remove(importId);
        }
    }

    public void sendError(String importId, String errorMessage) {
        SseEmitter emitter = emitters.get(importId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", errorMessage, "timestamp", Instant.now())));
                emitter.complete(); // close connection
            } catch (IOException ignored) {
                // probably connection closed already
            } finally {
                emitters.remove(importId);
            }
        }
    }

}

