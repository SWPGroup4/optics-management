package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO response phân trang cho danh sách Combo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboPageResponse {
	List<ComboResponse> items;
	int page;
	int size;
	long totalElements;
	int totalPages;
}
