package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PolicyResponse {
	Integer id;
	String managerUserId;
	String managerUsername;
	String code;
	String title;
	String description;
	LocalDate effectiveFrom;
	LocalDate effectiveTo;
	LocalDateTime createdAt;
}
