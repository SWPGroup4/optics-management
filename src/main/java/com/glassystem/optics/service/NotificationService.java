package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.NotificationCreateRequest;
import com.glassystem.optics.dto.response.NotificationResponse;
import com.glassystem.optics.entity.Notification;
import com.glassystem.optics.entity.User;
import com.glassystem.optics.enums.NotificationTemplate;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.NotificationMapper;
import com.glassystem.optics.repository.NotificationRepository;
import com.glassystem.optics.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    NotificationSseService notificationSseService;
    NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String senderId = SecurityContextHolder.getContext().getAuthentication().getName();

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(request.getTitle())
                .content(request.getContent())
                .senderId(senderId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse response = notificationMapper.toResponse(notificationRepository.save(notification));
        notificationSseService.publishToUser(notification.getRecipient().getId(), response);

        return response;
    }

    @Transactional
    public NotificationResponse createSystemNotification(String recipientId, NotificationTemplate template, Object... args) {
        return createSystemNotification(recipientId, template.getTitle(args), template.getContent(args));
    }


    @Transactional
    public NotificationResponse createSystemNotification(String recipientId, String title, String content) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .content(content)
                .senderId("SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationResponse response = notificationMapper.toResponse(notificationRepository.save(notification));
        notificationSseService.publishToUser(notification.getRecipient().getId(), response);

        return response;
    }

    public List<NotificationResponse> getMyNotifications() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public long getMyUnreadCount() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUserId);
    }

    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        NotificationResponse response = notificationMapper.toResponse(notificationRepository.save(notification));
        notificationSseService.publishToUser(notification.getRecipient().getId(), response);

        return response;
    }

    @Transactional
    public int markAllAsRead() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .filter(notification -> !notification.isRead())
                .toList();

        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        });

        if (!notifications.isEmpty()) {
            List<Notification> updatedNotifications = notificationRepository.saveAll(notifications);
            updatedNotifications.forEach(updatedNotification ->
                    notificationSseService.publishToUser(currentUserId, notificationMapper.toResponse(updatedNotification))
            );
        }

        return notifications.size();
    }
}
