package com.glassystem.optics.controller.notification;

import com.glassystem.optics.dto.request.NotificationCreateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMyNotifications() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return notificationSseService.subscribe(currentUserId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ApiResponse<NotificationResponse> createNotification(@RequestBody @Valid NotificationCreateRequest request) {
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.createNotification(request))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notificationService.getMyNotifications())
                .build();
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<Map<String, Long>> getMyUnreadCount() {
        return ApiResponse.<Map<String, Long>>builder()
                .result(Map.of("unreadCount", notificationService.getMyUnreadCount()))
                .build();
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable String notificationId) {
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.markAsRead(notificationId))
                .build();
    }

    @PatchMapping("/me/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead() {
        return ApiResponse.<Map<String, Integer>>builder()
                .result(Map.of("updated", notificationService.markAllAsRead()))
                .build();
    }
}
