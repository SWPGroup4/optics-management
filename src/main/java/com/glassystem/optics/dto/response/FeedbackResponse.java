package com.glassystem.optics.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackResponse {
    String feedbackId;
    String orderId;
    String productId;
    String productName;
    String customerId;
    String customerName;
    Integer rating;
    String comment;
    List<String> imageUrls;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
