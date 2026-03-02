package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.*;
import com.glassystem.optics.dto.response.*;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.ComboStatus;
import com.glassystem.optics.enums.DiscountType;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ComboMapper;
import com.glassystem.optics.repository.*;
import com.glassystem.optics.specification.ComboSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ nghiệp vụ Combo khuyến mãi.
 *
 * Bao gồm:
 * 1. Tạo combo (Manager)
 * 2. Cập nhật combo (Manager)
 * 3. Bật/tắt combo (Manager)
 * 4. Lấy danh sách combo (Admin view, có phân trang + filter)
 * 5. Lấy chi tiết combo
 * 6. Lấy combo khả dụng (Sales/Customer, check tồn kho)
 * 7. Validate combo với giỏ hàng
 * 8. Check stock cho combo
 * 9. Sync trạng thái combo (background job gọi)
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ComboService {
	ComboRepository comboRepository;
	ComboItemRepository comboItemRepository;
	ProductRepository productRepository;
	ProductVariantRepository productVariantRepository;
	InventoryRepository inventoryRepository;
	ComboMapper comboMapper;

	// =====================================================================
	// 1. TẠO COMBO (API 1: POST /api/combos)
	// =====================================================================

	/**
	 * Tạo mới một Combo khuyến mãi.
	 *
	 * Luồng xử lý:
	 * 1. Validate thời gian (startTime < endTime)
	 * 2. Validate discountValue (> 0, nếu PERCENT thì <= 100)
	 * 3. Validate comboItems (không trùng SKU, mỗi item phải có productId hoặc skuId)
	 * 4. Tính trạng thái ban đầu dựa trên thời gian hiện tại
	 * 5. Lưu Combo + ComboItems vào DB
	 */
	@Transactional
	public ComboResponse createCombo(ComboCreateRequest request) {
		// Validate nghiệp vụ nâng cao
		validateComboTime(request.getStartTime(), request.getEndTime());
		validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
		validateComboItems(request.getComboItems());

		// Tính trạng thái ban đầu
		ComboStatus initialStatus = calculateStatus(
				request.getStartTime(),
				request.getEndTime(),
				request.getIsManuallyDisabled() != null && request.getIsManuallyDisabled()
		);

		// Tạo entity Combo
		Combo combo = Combo.builder()
				.name(request.getName())
				.description(request.getDescription())
				.discountType(request.getDiscountType())
				.discountValue(request.getDiscountValue())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.status(initialStatus)
				.isManuallyDisabled(request.getIsManuallyDisabled() != null && request.getIsManuallyDisabled())
				.build();

		combo = comboRepository.save(combo);

		// Tạo các ComboItem
		List<ComboItem> items = buildComboItems(combo, request.getComboItems());
		comboItemRepository.saveAll(items);
		combo.setComboItems(items);

		log.info("Tạo combo thành công: id={}, name={}, status={}", combo.getId(), combo.getName(), combo.getStatus());
		return comboMapper.toComboResponse(combo);
	}

	// =====================================================================
	// 2. CẬP NHẬT COMBO (API 2: PUT /api/combos/{comboId})
	// =====================================================================

	/**
	 * Cập nhật Combo.
	 *
	 * Luồng xử lý:
	 * 1. Check combo tồn tại
	 * 2. Không cho sửa combo đã EXPIRED
	 * 3. Validate lại thời gian, discount, items
	 * 4. Replace toàn bộ danh sách comboItems
	 * 5. Tính lại trạng thái
	 */
	@Transactional
	public ComboResponse updateCombo(String comboId, ComboUpdateRequest request) {
		Combo combo = comboRepository.findById(comboId)
				.orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

		// Không cho sửa combo đã hết hạn
		if (combo.getStatus() == ComboStatus.EXPIRED) {
			throw new AppException(ErrorCode.COMBO_EXPIRED);
		}

		// Validate nghiệp vụ
		validateComboTime(request.getStartTime(), request.getEndTime());
		validateDiscountValue(request.getDiscountType(), request.getDiscountValue());
		validateComboItems(request.getComboItems());

		// Cập nhật thông tin combo
		combo.setName(request.getName());
		combo.setDescription(request.getDescription());
		combo.setDiscountType(request.getDiscountType());
		combo.setDiscountValue(request.getDiscountValue());
		combo.setStartTime(request.getStartTime());
		combo.setEndTime(request.getEndTime());

		if (request.getIsManuallyDisabled() != null) {
			combo.setIsManuallyDisabled(request.getIsManuallyDisabled());
		}

		// Tính lại trạng thái
		combo.setStatus(calculateStatus(
				combo.getStartTime(),
				combo.getEndTime(),
				combo.getIsManuallyDisabled()
		));

		combo = comboRepository.save(combo);

		// Replace danh sách comboItems: xóa cũ, tạo mới
		comboItemRepository.deleteAllByCombo_Id(comboId);
		List<ComboItem> newItems = buildComboItems(combo, request.getComboItems());
		comboItemRepository.saveAll(newItems);
		combo.setComboItems(newItems);

		log.info("Cập nhật combo thành công: id={}", comboId);
		return comboMapper.toComboResponse(combo);
	}

	// =====================================================================
	// 3. BẬT / TẮT COMBO (API 3: PATCH /api/combos/{comboId}/status)
	// =====================================================================

	/**
	 * Enable / Disable combo thủ công.
	 *
	 * Luồng xử lý:
	 * 1. Chỉ cho phép chuyển sang ACTIVE hoặc INACTIVE
	 * 2. Không cho bật combo đã hết hạn
	 * 3. Ghi đè trạng thái thủ công (override logic time)
	 */
	@Transactional
	public ComboResponse updateComboStatus(String comboId, ComboStatusRequest request) {
		Combo combo = comboRepository.findById(comboId)
				.orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

		ComboStatus newStatus = request.getStatus();

		// Chỉ cho phép ACTIVE hoặc INACTIVE
		if (newStatus != ComboStatus.ACTIVE && newStatus != ComboStatus.INACTIVE) {
			throw new AppException(ErrorCode.COMBO_STATUS_INVALID);
		}

		// Không cho bật combo đã hết hạn
		if (newStatus == ComboStatus.ACTIVE && combo.getEndTime().isBefore(LocalDateTime.now())) {
			throw new AppException(ErrorCode.COMBO_CANNOT_ACTIVATE_EXPIRED);
		}

		// Cập nhật cờ disable thủ công
		if (newStatus == ComboStatus.INACTIVE) {
			combo.setIsManuallyDisabled(true);
		} else {
			combo.setIsManuallyDisabled(false);
		}

		combo.setStatus(newStatus);
		combo = comboRepository.save(combo);

		log.info("Cập nhật trạng thái combo: id={}, status={}", comboId, newStatus);
		return comboMapper.toComboResponse(combo);
	}

	// =====================================================================
	// 4. LẤY DANH SÁCH COMBO (API 4: GET /api/combos)
	// =====================================================================

	/**
	 * Lấy danh sách combo cho Admin view.
	 *
	 * Hỗ trợ filter theo:
	 * - keyword: tìm theo tên combo
	 * - status: lọc theo trạng thái
	 * - fromDate, toDate: lọc theo khoảng thời gian
	 * - Phân trang (page, size)
	 *
	 * Không filter theo tồn kho, không filter theo thời gian (trừ khi truyền param).
	 */
	public ComboPageResponse getAllCombos(
			ComboStatus status,
			LocalDateTime fromDate,
			LocalDateTime toDate,
			Pageable pageable) {

		Page<Combo> result = comboRepository.findAll(
				ComboSpecification.filter(status, fromDate, toDate),
				pageable
		);

		return ComboPageResponse.builder()
				.items(result.getContent()
						.stream()
						.map(comboMapper::toComboResponse)
						.toList())
				.page(result.getNumber())
				.size(result.getSize())
				.totalElements(result.getTotalElements())
				.totalPages(result.getTotalPages())
				.build();
	}

	// =====================================================================
	// 5. LẤY CHI TIẾT COMBO (API 5: GET /api/combos/{comboId})
	// =====================================================================

	/**
	 * Lấy chi tiết combo.
	 *
	 * Tính trạng thái realtime dựa trên thời gian hiện tại
	 * (ACTIVE / EXPIRED / SCHEDULED) trước khi trả về.
	 */
	public ComboResponse getComboDetail(String comboId) {
		Combo combo = comboRepository.findById(comboId)
				.orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

		// Tính lại trạng thái realtime (không persist, chỉ hiển thị)
		ComboStatus realtimeStatus = calculateStatus(
				combo.getStartTime(),
				combo.getEndTime(),
				combo.getIsManuallyDisabled()
		);
		combo.setStatus(realtimeStatus);

		return comboMapper.toComboResponse(combo);
	}

	// =====================================================================
	// 6. LẤY COMBO KHẢ DỤNG (API 6: GET /api/combos/available)
	// =====================================================================

	/**
	 * Lấy danh sách combo khả dụng cho Sales/Customer.
	 *
	 * Luồng xử lý:
	 * 1. Filter combo: status = ACTIVE, currentTime ∈ [startTime, endTime]
	 * 2. Với mỗi combo: check tồn kho tất cả comboItems
	 * 3. Nếu 1 item fail → loại combo khỏi kết quả
	 */
	public List<ComboResponse> getAvailableCombos(LocalDateTime currentTime) {
		if (currentTime == null) {
			currentTime = LocalDateTime.now();
		}

		List<Combo> activeCombos = comboRepository.findAvailableCombos(currentTime);

		// Lọc combo có đủ tồn kho
		return activeCombos.stream()
				.filter(this::isComboInStock)
				.map(comboMapper::toComboResponse)
				.toList();
	}

	// =====================================================================
	// 7. VALIDATE COMBO VỚI GIỎ HÀNG (API 7: POST /api/combos/validate)
	// =====================================================================

	/**
	 * Validate combo với giỏ hàng (chưa tạo order).
	 *
	 * Luồng xử lý:
	 * 1. Check combo tồn tại & active
	 * 2. Check thời gian hiệu lực
	 * 3. Check rule: giỏ hàng có đủ SKU + số lượng >= requiredQuantity
	 * 4. Check tồn kho hiện tại
	 * 5. Tính discountAmount nếu hợp lệ
	 */
	public ComboValidateResponse validateCombo(ComboValidateRequest request) {
		// 1. Check combo tồn tại
		Optional<Combo> optCombo = comboRepository.findById(request.getComboId());
		if (optCombo.isEmpty()) {
			return ComboValidateResponse.builder()
					.isValid(false)
					.reason("COMBO_NOT_FOUND")
					.build();
		}

		Combo combo = optCombo.get();

		// 2. Check combo đang active
		if (combo.getStatus() != ComboStatus.ACTIVE) {
			return ComboValidateResponse.builder()
					.isValid(false)
					.reason("NOT_ACTIVE")
					.build();
		}

		// 3. Check thời gian hiệu lực
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(combo.getStartTime()) || now.isAfter(combo.getEndTime())) {
			return ComboValidateResponse.builder()
					.isValid(false)
					.reason("EXPIRED")
					.build();
		}

		// 4. Check rule: giỏ hàng có đủ SKU + số lượng
		// Tạo map skuId -> quantity từ giỏ hàng
		Map<String, Integer> cartMap = new HashMap<>();
		for (CartItemRequest cartItem : request.getCartItems()) {
			cartMap.merge(cartItem.getSkuId(), cartItem.getQuantity(), Integer::sum);
		}

		for (ComboItem comboItem : combo.getComboItems()) {
			String skuId = comboItem.getProductVariant() != null
					? comboItem.getProductVariant().getId()
					: null;

			if (skuId == null) {
				// Combo item chỉ yêu cầu product level → check bất kỳ variant nào của product trong giỏ
				boolean found = checkProductLevelMatch(comboItem, cartMap);
				if (!found) {
					return ComboValidateResponse.builder()
							.isValid(false)
							.reason("NOT_MATCH_RULE")
							.build();
				}
			} else {
				// Check đúng SKU
				Integer cartQty = cartMap.get(skuId);
				if (cartQty == null || cartQty < comboItem.getRequiredQuantity()) {
					return ComboValidateResponse.builder()
							.isValid(false)
							.reason("NOT_MATCH_RULE")
							.build();
				}
			}
		}

		// 5. Check tồn kho
		if (!isComboInStock(combo)) {
			return ComboValidateResponse.builder()
					.isValid(false)
					.reason("OUT_OF_STOCK")
					.build();
		}

		// 6. Tính discount amount
		BigDecimal discountAmount = calculateDiscountAmount(combo, cartMap);

		return ComboValidateResponse.builder()
				.isValid(true)
				.discountAmount(discountAmount)
				.build();
	}

	// =====================================================================
	// 8. CHECK STOCK CHO COMBO (API 8: POST /api/combos/check-stock)
	// =====================================================================

	/**
	 * Check tồn kho cho tất cả item trong combo.
	 *
	 * Luồng xử lý:
	 * - Với mỗi comboItem có SKU:
	 *   + availableQty = inventory.quantity - inventory.reservedQuantity
	 *   + So sánh với requiredQuantity
	 * - Trả về isAvailable + danh sách failedItems (nếu có)
	 */
	public ComboStockCheckResponse checkComboStock(String comboId) {
		Combo combo = comboRepository.findById(comboId)
				.orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

		List<ComboStockCheckResponse.FailedStockItem> failedItems = new ArrayList<>();

		for (ComboItem item : combo.getComboItems()) {
			if (item.getProductVariant() == null) {
				continue; // Bỏ qua item chỉ yêu cầu product level
			}

			String variantId = item.getProductVariant().getId();
			Optional<Inventory> optInventory = inventoryRepository.findByProductVariantId(variantId);

			int available = 0;
			if (optInventory.isPresent()) {
				Inventory inv = optInventory.get();
				available = (inv.getQuantity() != null ? inv.getQuantity() : 0)
						- (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0);
			}

			if (available < item.getRequiredQuantity()) {
				failedItems.add(ComboStockCheckResponse.FailedStockItem.builder()
						.skuId(variantId)
						.requiredQuantity(item.getRequiredQuantity())
						.availableQuantity(Math.max(available, 0))
						.build());
			}
		}

		return ComboStockCheckResponse.builder()
				.isAvailable(failedItems.isEmpty())
				.failedItems(failedItems.isEmpty() ? null : failedItems)
				.build();
	}

	// =====================================================================
	// 9. SYNC TRẠNG THÁI COMBO (Background Job gọi)
	// =====================================================================

	/**
	 * Đồng bộ trạng thái combo dựa trên thời gian hiện tại.
	 *
	 * Rule:
	 * - now < startTime → SCHEDULED
	 * - now ∈ [startTime, endTime] → ACTIVE
	 * - now > endTime → EXPIRED
	 * - KHÔNG override combo bị disable thủ công (isManuallyDisabled = true)
	 *
	 * Được gọi bởi ComboStatusScheduler (cron job).
	 */
	@Transactional
	public void syncComboStatuses() {
		List<Combo> combos = comboRepository.findAllByIsManuallyDisabledFalse();
		LocalDateTime now = LocalDateTime.now();
		int updated = 0;

		for (Combo combo : combos) {
			ComboStatus newStatus = calculateStatus(combo.getStartTime(), combo.getEndTime(), false);

			if (combo.getStatus() != newStatus) {
				combo.setStatus(newStatus);
				comboRepository.save(combo);
				updated++;
				log.debug("Sync combo status: id={}, oldStatus={}, newStatus={}", combo.getId(), combo.getStatus(), newStatus);
			}
		}

		if (updated > 0) {
			log.info("Combo status sync hoàn tất: {} combo được cập nhật", updated);
		}
	}

	// =====================================================================
	// PRIVATE HELPER METHODS
	// =====================================================================

	/**
	 * Tính trạng thái combo dựa trên thời gian hiện tại.
	 */
	private ComboStatus calculateStatus(LocalDateTime startTime, LocalDateTime endTime, boolean isManuallyDisabled) {
		if (isManuallyDisabled) {
			return ComboStatus.INACTIVE;
		}
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(startTime)) {
			return ComboStatus.SCHEDULED;
		}
		if (now.isAfter(endTime)) {
			return ComboStatus.EXPIRED;
		}
		return ComboStatus.ACTIVE;
	}

	/**
	 * Validate thời gian combo: startTime phải trước endTime.
	 */
	private void validateComboTime(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime == null || endTime == null) {
			throw new AppException(ErrorCode.COMBO_TIME_REQUIRED);
		}
		if (!startTime.isBefore(endTime)) {
			throw new AppException(ErrorCode.COMBO_TIME_INVALID);
		}
	}

	/**
	 * Validate giá trị giảm giá.
	 * - Phải > 0
	 * - Nếu PERCENT thì phải <= 100
	 */
	private void validateDiscountValue(DiscountType type, BigDecimal value) {
		if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new AppException(ErrorCode.COMBO_DISCOUNT_VALUE_INVALID);
		}
		if (type == DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new AppException(ErrorCode.COMBO_PERCENT_VALUE_INVALID);
		}
	}

	/**
	 * Validate danh sách combo items.
	 * - Mỗi item phải có productId hoặc skuId
	 * - requiredQuantity >= 1
	 * - Không trùng SKU trong cùng combo
	 */
	private void validateComboItems(List<ComboItemRequest> items) {
		if (items == null || items.isEmpty()) {
			throw new AppException(ErrorCode.COMBO_ITEMS_REQUIRED);
		}

		Set<String> skuIds = new HashSet<>();

		for (ComboItemRequest item : items) {
			// Phải có ít nhất productId hoặc skuId
			if ((item.getProductId() == null || item.getProductId().isBlank())
					&& (item.getSkuId() == null || item.getSkuId().isBlank())) {
				throw new AppException(ErrorCode.COMBO_ITEM_PRODUCT_OR_SKU_REQUIRED);
			}

			// requiredQuantity >= 1
			if (item.getRequiredQuantity() == null || item.getRequiredQuantity() < 1) {
				throw new AppException(ErrorCode.COMBO_ITEM_QUANTITY_INVALID);
			}

			// Check trùng SKU
			if (item.getSkuId() != null && !item.getSkuId().isBlank()) {
				if (!skuIds.add(item.getSkuId())) {
					throw new AppException(ErrorCode.COMBO_ITEM_DUPLICATE_SKU);
				}
			}
		}
	}

	/**
	 * Tạo danh sách ComboItem entity từ request.
	 * Lookup Product và ProductVariant từ DB để đảm bảo tồn tại.
	 */
	private List<ComboItem> buildComboItems(Combo combo, List<ComboItemRequest> itemRequests) {
		List<ComboItem> items = new ArrayList<>();

		for (ComboItemRequest req : itemRequests) {
			ComboItem.ComboItemBuilder builder = ComboItem.builder()
					.combo(combo)
					.requiredQuantity(req.getRequiredQuantity());

			// Lookup product nếu có
			if (req.getProductId() != null && !req.getProductId().isBlank()) {
				Product product = productRepository.findById(req.getProductId())
						.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
				builder.product(product);
			}

			// Lookup variant (SKU) nếu có
			if (req.getSkuId() != null && !req.getSkuId().isBlank()) {
				ProductVariant variant = productVariantRepository.findById(req.getSkuId())
						.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
				builder.productVariant(variant);

				// Nếu có SKU nhưng chưa set product → lấy product từ variant
				if (req.getProductId() == null || req.getProductId().isBlank()) {
					builder.product(variant.getProduct());
				}
			}

			items.add(builder.build());
		}

		return items;
	}

	/**
	 * Check tồn kho cho tất cả item trong combo.
	 * Trả về true nếu tất cả item đều đủ hàng.
	 */
	private boolean isComboInStock(Combo combo) {
		for (ComboItem item : combo.getComboItems()) {
			if (item.getProductVariant() == null) {
				continue;
			}

			String variantId = item.getProductVariant().getId();
			Optional<Inventory> optInventory = inventoryRepository.findByProductVariantId(variantId);

			int available = 0;
			if (optInventory.isPresent()) {
				Inventory inv = optInventory.get();
				available = (inv.getQuantity() != null ? inv.getQuantity() : 0)
						- (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0);
			}

			if (available < item.getRequiredQuantity()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check product-level match: giỏ hàng có bất kỳ variant nào của product
	 * với số lượng >= requiredQuantity không.
	 */
	private boolean checkProductLevelMatch(ComboItem comboItem, Map<String, Integer> cartMap) {
		if (comboItem.getProduct() == null) {
			return false;
		}

		String productId = comboItem.getProduct().getId();

		// Lấy tất cả variant ACTIVE của product
		List<ProductVariant> variants = productVariantRepository
				.findAllByProduct_IdAndStatus(productId,
						com.glassystem.optics.enums.ProductVariantStatus.ACTIVE);

		// Check xem giỏ hàng có chứa bất kỳ variant nào với đủ số lượng
		int totalQtyInCart = 0;
		for (ProductVariant v : variants) {
			Integer cartQty = cartMap.get(v.getId());
			if (cartQty != null) {
				totalQtyInCart += cartQty;
			}
		}

		return totalQtyInCart >= comboItem.getRequiredQuantity();
	}

	/**
	 * Tính số tiền giảm giá dựa trên loại discount.
	 *
	 * - PERCENT: tính tổng giá các SKU trong combo từ giỏ hàng, rồi nhân %
	 * - FIXED_AMOUNT: trả về discountValue trực tiếp
	 */
	private BigDecimal calculateDiscountAmount(Combo combo, Map<String, Integer> cartMap) {
		if (combo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
			return combo.getDiscountValue();
		}

		// PERCENT: tính tổng giá trị các item trong combo
		BigDecimal totalPrice = BigDecimal.ZERO;

		for (ComboItem item : combo.getComboItems()) {
			if (item.getProductVariant() != null && item.getProductVariant().getPrice() != null) {
				int qty = item.getRequiredQuantity();
				totalPrice = totalPrice.add(
						item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(qty))
				);
			}
		}

		// discountAmount = totalPrice * discountValue / 100
		return totalPrice.multiply(combo.getDiscountValue())
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}
}
