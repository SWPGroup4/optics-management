package com.glassystem.optics.controller.policy;

import com.glassystem.optics.dto.request.PolicyCreateRequest;
import com.glassystem.optics.dto.request.PolicyUpdateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.PolicyPageResponse;
import com.glassystem.optics.dto.response.PolicyResponse;
import com.glassystem.optics.service.PolicyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PolicyController {
	PolicyService policyService;

	// =====================================================================
	// API 1: POST /api/policies — Tạo policy mới (Manager)
	// =====================================================================

	@PostMapping
	@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
	ApiResponse<PolicyResponse> createPolicy(@RequestBody @Valid PolicyCreateRequest request) {
		return ApiResponse.<PolicyResponse>builder()
				.result(policyService.createPolicy(request))
				.message("Tạo policy thành công")
				.build();
	}

	// =====================================================================
	// API 2: PUT /api/policies/{id} — Cập nhật policy (Manager)
	// =====================================================================

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
	ApiResponse<PolicyResponse> updatePolicy(
			@PathVariable Integer id,
			@RequestBody @Valid PolicyUpdateRequest request) {
		return ApiResponse.<PolicyResponse>builder()
				.result(policyService.updatePolicy(id, request))
				.message("Cập nhật policy thành công")
				.build();
	}

	// =====================================================================
	// API 3: DELETE /api/policies/{id} — Xóa policy (Manager)
	// =====================================================================

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
	ApiResponse<Void> deletePolicy(@PathVariable Integer id) {
		policyService.deletePolicy(id);
		return ApiResponse.<Void>builder()
				.message("Xóa policy thành công")
				.build();
	}

	// =====================================================================
	// API 4: GET /api/policies/{id} — Lấy chi tiết policy
	// =====================================================================

	@GetMapping("/{id}")
	ApiResponse<PolicyResponse> getPolicyDetail(@PathVariable Integer id) {
		return ApiResponse.<PolicyResponse>builder()
				.result(policyService.getPolicyDetail(id))
				.build();
	}

	// =====================================================================
	// API 5: GET /api/policies — Lấy danh sách policy (search, filter, sort, paging)
	// =====================================================================

	@GetMapping
	ApiResponse<PolicyPageResponse> getAllPolicies(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();
		PageRequest pageable = PageRequest.of(page, size, sort);

		return ApiResponse.<PolicyPageResponse>builder()
				.result(policyService.getAllPolicies(keyword, effectiveFrom, effectiveTo, pageable))
				.build();
	}
}
