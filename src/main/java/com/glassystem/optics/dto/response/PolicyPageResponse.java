package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PolicyPageResponse {
	List<PolicyResponse> items;
	int page;
	int size;
	long totalElements;
	int totalPages;
}
