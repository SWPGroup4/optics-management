package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationCreateRequest {

    @NotBlank
    String recipientId;

    @NotBlank
    String title;

    @NotBlank
    String content;
}
