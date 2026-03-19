package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {

    String id;
    String recipientId;
    String recipientName;
    String title;
    String content;
    String senderId;
    boolean isRead;
    LocalDateTime createdAt;
    LocalDateTime readAt;
}
