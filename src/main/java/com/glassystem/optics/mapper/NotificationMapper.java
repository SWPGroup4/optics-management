package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.NotificationResponse;
import com.glassystem.optics.entity.Notification;
import com.glassystem.optics.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    @Mapping(target = "recipientName", expression = "java(resolveRecipientName(notification))")
    @Mapping(target = "isRead", source = "read")
    NotificationResponse toResponse(Notification notification);

    default String resolveRecipientName(Notification notification) {
        if (notification == null) {
            return null;
        }
        return buildUserDisplayName(notification.getRecipient());
    }

    default String buildUserDisplayName(User user) {
        if (user == null) {
            return null;
        }

        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }
}
