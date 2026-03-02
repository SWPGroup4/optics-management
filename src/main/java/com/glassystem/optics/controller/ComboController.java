package com.glassystem.optics.controller;

import com.glassystem.optics.dto.request.ComboCreateRequest;
import com.glassystem.optics.dto.request.ComboStatusRequest;
import com.glassystem.optics.dto.request.ComboUpdateRequest;
import com.glassystem.optics.dto.request.ComboValidateRequest;
import com.glassystem.optics.dto.response.*;
import com.glassystem.optics.enums.ComboStatus;
import com.glassystem.optics.service.ComboService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller quản lý Combo khuyến mãi.
 *
 * Bao gồm 8 endpoint chính (endpoint thứ 9 là background job, không có API):
 *
 * 1. POST   /api/combos              → Tạo combo (Manager)
 * 2. PUT    /api/combos/{comboId}     → Cập nhật combo (Manager)
 * 3. PATCH  /api/combos/{comboId}/status → Bật/tắt combo (Manager)
 * 4. GET    /api/combos              → Lấy danh sách combo (Admin view)
 * 5. GET    /api/combos/{comboId}    → Lấy chi tiết combo
 * 6. GET    /api/combos/available    → Lấy combo khả dụng (Sales/Customer)
 * 7. POST   /api/combos/validate     → Validate combo với giỏ hàng
 * 8. POST   /api/combos/check-stock  → Check tồn kho cho combo
 *
 * Quyền truy cập:
 * - API 1, 2, 3: Yêu cầu role OPERATION hoặc ADMIN (Manager)
 * - API 4, 5: Yêu cầu đăng nhập (authenticated)
 * - API 6, 7, 8: Yêu cầu đăng nhập (authenticated)
 */
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComboController {
	ComboService comboService;

	// =====================================================================
	// API 1: POST /api/combos — Tạo combo mới (Manager)
	// =====================================================================

	/**
	 * Tạo mới một Combo khuyến mãi.
	 *
	 * Yêu cầu quyền: OPERATION hoặc ADMIN
	 *
	 * Request body: ComboCreateRequest
	 * - name: Tên combo (bắt buộc)
	 * - description: Mô tả
	 * - discountType: PERCENT hoặc FIXED_AMOUNT (bắt buộc)
	 * - discountValue: Giá trị giảm > 0 (bắt buộc)
	 * - startTime: Thời điểm bắt đầu (bắt buộc)
	 * - endTime: Thời điểm kết thúc (bắt buộc)
	 * - isManuallyDisabled: true/false
	 * - comboItems: Danh sách item (bắt buộc, ít nhất 1)
	 */
	@PostMapping
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	ApiResponse<ComboResponse> createCombo(@RequestBody @Valid ComboCreateRequest request) {
		return ApiResponse.<ComboResponse>builder()
				.result(comboService.createCombo(request))
				.message("Tạo combo thành công")
				.build();
	}

	// =====================================================================
	// API 2: PUT /api/combos/{comboId} — Cập nhật combo (Manager)
	// =====================================================================

	/**
	 * Cập nhật Combo.
	 *
	 * Yêu cầu quyền: OPERATION hoặc ADMIN
	 * Không cho sửa combo đã EXPIRED.
	 * Danh sách comboItems sẽ được replace hoàn toàn.
	 */
	@PutMapping("/{comboId}")
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	ApiResponse<ComboResponse> updateCombo(
			@PathVariable String comboId,
			@RequestBody @Valid ComboUpdateRequest request) {
		return ApiResponse.<ComboResponse>builder()
				.result(comboService.updateCombo(comboId, request))
				.message("Cập nhật combo thành công")
				.build();
	}

	// =====================================================================
	// API 3: PATCH /api/combos/{comboId}/status — Bật/tắt combo (Manager)
	// =====================================================================

	/**
	 * Enable / Disable combo thủ công.
	 *
	 * Yêu cầu quyền: OPERATION hoặc ADMIN
	 * Chỉ cho phép chuyển sang ACTIVE hoặc INACTIVE.
	 * Không cho bật combo đã hết hạn.
	 */
	@PatchMapping("/{comboId}/status")
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	ApiResponse<ComboResponse> updateComboStatus(
			@PathVariable String comboId,
			@RequestBody @Valid ComboStatusRequest request) {
		return ApiResponse.<ComboResponse>builder()
				.result(comboService.updateComboStatus(comboId, request))
				.message("Cập nhật trạng thái combo thành công")
				.build();
	}

	// =====================================================================
	// API 4: GET /api/combos — Lấy danh sách combo (Admin view)
	// =====================================================================

	/**
	 * Lấy danh sách combo cho Admin view.
	 *
	 * Query params (tất cả optional):
	 * - keyword: Tìm theo tên combo
	 * - status: Lọc theo trạng thái (SCHEDULED, ACTIVE, INACTIVE, EXPIRED)
	 * - fromDate: Lọc combo có startTime >= fromDate
	 * - toDate: Lọc combo có endTime <= toDate
	 * - page, size: Phân trang (mặc định page=0, size=10)
	 * - sortBy: Trường sắp xếp (mặc định: createdAt)
	 * - sortDir: Hướng sắp xếp (asc/desc, mặc định: desc)
	 */
	@GetMapping
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	ApiResponse<ComboPageResponse> getAllCombos(
			@RequestParam(required = false) ComboStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();
		PageRequest pageable = PageRequest.of(page, size, sort);

		return ApiResponse.<ComboPageResponse>builder()
				.result(comboService.getAllCombos( status, fromDate, toDate, pageable))
				.build();
	}

	// =====================================================================
	// API 5: GET /api/combos/{comboId} — Lấy chi tiết combo
	// =====================================================================

	/**
	 * Lấy chi tiết combo.
	 *
	 * Trạng thái được tính realtime dựa trên thời gian hiện tại.
	 */
	@GetMapping("/{comboId}")
	ApiResponse<ComboResponse> getComboDetail(@PathVariable String comboId) {
		return ApiResponse.<ComboResponse>builder()
				.result(comboService.getComboDetail(comboId))
				.build();
	}

	// =====================================================================
	// API 6: GET /api/combos/available — Lấy combo khả dụng (Sales/Customer)
	// =====================================================================

	/**
	 * Lấy danh sách combo khả dụng.
	 *
	 * Chỉ trả về combo:
	 * - status = ACTIVE
	 * - Thời gian hiện tại nằm trong [startTime, endTime]
	 * - Tất cả item đều đủ tồn kho
	 *
	 * Query params (optional):
	 * - currentTime: Thời điểm kiểm tra (mặc định = now)
	 */
	@GetMapping("/available")
	ApiResponse<List<ComboResponse>> getAvailableCombos(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime currentTime) {
		return ApiResponse.<List<ComboResponse>>builder()
				.result(comboService.getAvailableCombos(currentTime))
				.build();
	}

	// =====================================================================
	// API 7: POST /api/combos/validate — Validate combo với giỏ hàng
	// =====================================================================

	/**
	 * Validate combo với giỏ hàng (chưa tạo order).
	 *
	 * Request body: ComboValidateRequest
	 * - comboId: ID combo cần validate
	 * - cartItems: Danh sách item trong giỏ (skuId + quantity)
	 *
	 * Response:
	 * - isValid: true/false
	 * - discountAmount: Số tiền giảm (nếu valid)
	 * - reason: Lý do không hợp lệ (nếu invalid)
	 *   + OUT_OF_STOCK, NOT_MATCH_RULE, EXPIRED, NOT_ACTIVE, COMBO_NOT_FOUND
	 */
	@PostMapping("/validate")
	ApiResponse<ComboValidateResponse> validateCombo(@RequestBody @Valid ComboValidateRequest request) {
		return ApiResponse.<ComboValidateResponse>builder()
				.result(comboService.validateCombo(request))
				.build();
	}

	// =====================================================================
	// API 8: POST /api/combos/check-stock — Check tồn kho cho combo
	// =====================================================================

	/**
	 * Check tồn kho cho tất cả item trong combo.
	 *
	 * Request param: comboId
	 *
	 * Response:
	 * - isAvailable: true nếu tất cả item đều đủ hàng
	 * - failedItems: Danh sách item thiếu hàng (skuId, requiredQuantity, availableQuantity)
	 */
	@PostMapping("/check-stock")
	ApiResponse<ComboStockCheckResponse> checkComboStock(@RequestParam String comboId) {
		return ApiResponse.<ComboStockCheckResponse>builder()
				.result(comboService.checkComboStock(comboId))
				.build();
	}

	// =====================================================================
	// API 9: DELETE /api/combos/{comboId} — Xóa combo (Soft Delete)
	// =====================================================================

	@DeleteMapping("/{comboId}")
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	ApiResponse<Void> deleteCombo(@PathVariable String comboId) {
		comboService.deleteCombo(comboId);
		return ApiResponse.<Void>builder()
				.message("Combo deleted successfully")
				.build();
	}
}
