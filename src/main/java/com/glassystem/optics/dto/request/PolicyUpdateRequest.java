package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PolicyUpdateRequest {
	@NotBlank(message = "POLICY_CODE_REQUIRED")
	String code;

	@NotBlank(message = "POLICY_TITLE_REQUIRED")
	String title;

	String description;

	LocalDate effectiveFrom;

	LocalDate effectiveTo;
}
