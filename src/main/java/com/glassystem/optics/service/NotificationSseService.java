package com.glassystem.optics.service;

import com.glassystem.optics.dto.response.NotificationResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationSseService {

    static final long SSE_TIMEOUT = 0L;

    ConcurrentHashMap<String, Set<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        userEmitters.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));

        sendEvent(userId, emitter, "connected", "Notification SSE connected");

        return emitter;
    }

    public void publishToUser(String userId, NotificationResponse notification) {
        Set<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            sendEvent(userId, emitter, "notification", notification);
        }
    }

    private void sendEvent(String userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (Exception e) {
            log.debug("SSE emitter send failed, removing emitter", e);
            try {
                emitter.completeWithError(e);
            } catch (Exception completionException) {
                log.debug("SSE emitter completion failed", completionException);
            }
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        Set<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }
}
