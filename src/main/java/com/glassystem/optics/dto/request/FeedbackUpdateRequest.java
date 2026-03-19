package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackUpdateRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Integer rating;

    String comment;
}
