package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.NotificationResponse;
import com.glassystem.optics.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    NotificationResponse toResponse(Notification notification);
}
