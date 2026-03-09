package com.glassystem.optics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassystem.optics.dto.request.*;
import com.glassystem.optics.dto.response.*;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.*;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.OrderItemMapper;
import com.glassystem.optics.mapper.OrderMapper;
import com.glassystem.optics.mapper.PrescriptionMapper;
import com.glassystem.optics.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OrderService {
    private final OrderMapper orderMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final FileStorageService fileStorageService;
    private final ComboRepository comboRepository;
    private final ComboService comboService;
    private final LensRepository lensRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ObjectMapper objectMapper;
    private final PaymentCalculationService paymentCalculationService;
    /*
     * ===================== 1. CUSTOMER FLOW (APIs cho khách hàng)
     * =====================
     */

    @Transactional
    public OrderResponse createOrder(OrderCreationRequest request, MultipartFile file) throws IOException {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDate.now());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPhoneNumber(request.getPhoneNumber());


        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean hasPrescriptionImage = file != null && !file.isEmpty();
        String prescriptionImageUrl = null;
        if (hasPrescriptionImage) {
            prescriptionImageUrl = fileStorageService.uploadFile(file, S3ImageName.PRESCRIPTION);
        }

        for (OrderItemCreationRequest orderItemRequest : request.getItems()) {

            ProductVariant productVariant = productVariantRepository.findAllByIdAndStatus(
                            orderItemRequest.getProductVariantId(), ProductVariantStatus.ACTIVE)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            Inventory inventory = inventoryRepository.findByProductVariantId(productVariant.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));


            validateInventory(inventory, orderItemRequest.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setOrderItemType(resolveOrderItemType(productVariant));
            item.setInventory(inventory);
            item.setQuantity(orderItemRequest.getQuantity());
            item.setUnitPrice(productVariant.getPrice());
            Lens lens = resolveLens(orderItemRequest.getLensId());
            BigDecimal lensPrice = lens == null ? BigDecimal.ZERO : normalizeAmount(lens.getPrice());
            item.setLensId(lens == null ? null : lens.getId());
            item.setLensName(lens == null ? null : lens.getName());
            item.setLensPrice(lensPrice);
            item.setTotalPrice(calculateOrderItemTotal(item.getUnitPrice(), lensPrice, orderItemRequest.getQuantity()));

            if (orderItemRequest.getPrescription() != null) {
                Prescription prescription = prescriptionMapper.toPrescription(orderItemRequest.getPrescription());
                if (hasPrescriptionImage) {
                    prescription.setImageUrl(prescriptionImageUrl);
                }
                prescription = prescriptionRepository.save(prescription);
                item.setPrescription(prescription);
            }
            if (lens != null && orderItemRequest.getPrescription() == null) {
                throw new AppException(ErrorCode.PRESCRIPTION_REQUIRED);
            }

            updateInventoryStock(inventory, orderItemRequest.getQuantity());
            order.getItems().add(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        // ===== COMBO: Áp dụng combo nếu có =====
        BigDecimal comboDiscountAmount = BigDecimal.ZERO;
        if (request.getComboId() != null && !request.getComboId().isBlank()) {
            comboDiscountAmount = applyComboToOrder(order, request.getComboId(), request.getItems(), totalAmount);
        }

        BigDecimal finalTotal = totalAmount.subtract(comboDiscountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        PaymentCalculationService.PaymentCalculationResult paymentCalculation =
                paymentCalculationService.calculatePaymentRequirement(order.getItems());


        for (OrderItem item : order.getItems()) {
            BigDecimal baseItemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal lensFeeTotal = normalizeAmount(item.getLensPrice())
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal paymentPercentage = item.getOrderItemType() == OrderItemType.PRE_ORDER
                    ? new BigDecimal("0.5")
                    : BigDecimal.ONE;
            BigDecimal itemDepositPrice = baseItemTotal.multiply(paymentPercentage).add(lensFeeTotal);
            item.setDepositPrice(itemDepositPrice);
            item.setRemainingPrice(item.getTotalPrice().subtract(itemDepositPrice));
        }

        BigDecimal requiredPaymentTotal = paymentCalculation.getRequiredPaymentTotal().min(finalTotal);
        order.setDepositAmount(requiredPaymentTotal);
        order.setRemainingAmount(finalTotal.subtract(requiredPaymentTotal));
        order.setPaymentMethod(PaymentMethod.VNPAY);

        boolean hasPreOrder = order.getItems().stream()
                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRE_ORDER);
        if (hasPreOrder) {
            order.setPreOrderStatus(PreOrderStatus.DEPOSIT_PENDING);
        }

        order.setTotalAmount(finalTotal);
        log.info("Tạo đơn hàng: totalGốc={}, comboDiscount={}, finalTotal={}, comboId={}",
                totalAmount, comboDiscountAmount, finalTotal, request.getComboId());
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    /**
     * Áp dụng combo vào đơn hàng:
     * 1. Validate combo tồn tại, đang ACTIVE, trong thời gian hiệu lực
     * 2. Validate rule: đơn hàng có đủ SKU + số lượng theo yêu cầu combo
     * 3. Check tồn kho cho combo items
     * 4. Tính discount amount
     * 5. Lưu combo info vào order (combo reference + snapshot cho audit)
     */
    private BigDecimal applyComboToOrder(Orders order, String comboId,
                                         List<OrderItemCreationRequest> orderItems, BigDecimal totalAmount) {
        // 1. Check combo tồn tại
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        // 2. Check combo đang ACTIVE
        if (combo.getStatus() != ComboStatus.ACTIVE) {
            throw new AppException(ErrorCode.ORDER_COMBO_NOT_ACTIVE);
        }

        // 3. Check thời gian hiệu lực
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(combo.getStartTime()) || now.isAfter(combo.getEndTime())) {
            throw new AppException(ErrorCode.ORDER_COMBO_NOT_ACTIVE);
        }

        // 4. Check rule: đơn hàng có đủ SKU + số lượng
        Map<String, Integer> orderItemMap = new HashMap<>();
        for (OrderItemCreationRequest item : orderItems) {
            orderItemMap.merge(item.getProductVariantId(), item.getQuantity(), Integer::sum);
        }

        for (ComboItem comboItem : combo.getComboItems()) {
            if (comboItem.getProductVariant() != null) {
                String skuId = comboItem.getProductVariant().getId();
                Integer orderQty = orderItemMap.get(skuId);
                if (orderQty == null || orderQty < comboItem.getRequiredQuantity()) {
                    throw new AppException(ErrorCode.ORDER_COMBO_RULE_NOT_MATCH);
                }
            }
        }

        // 5. Check tồn kho combo (dùng ComboService)
        var stockCheck = comboService.checkComboStock(comboId);
        if (!stockCheck.getIsAvailable()) {
            throw new AppException(ErrorCode.ORDER_COMBO_STOCK_INSUFFICIENT);
        }

        // 6. Tính discount amount
        BigDecimal discountAmount;
        if (combo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = combo.getDiscountValue();
        } else {
            // PERCENT: tính trên tổng giá trị các item trong combo
            BigDecimal comboItemsTotal = BigDecimal.ZERO;
            for (ComboItem comboItem : combo.getComboItems()) {
                if (comboItem.getProductVariant() != null && comboItem.getProductVariant().getPrice() != null) {
                    comboItemsTotal = comboItemsTotal.add(
                            comboItem.getProductVariant().getPrice()
                                    .multiply(BigDecimal.valueOf(comboItem.getRequiredQuantity()))
                    );
                }
            }
            discountAmount = comboItemsTotal.multiply(combo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // 7. Lưu combo info vào order
        order.setCombo(combo);
        order.setComboDiscountAmount(discountAmount);

        // 8. Tạo combo snapshot cho audit
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("comboId", combo.getId());
            snapshot.put("comboName", combo.getName());
            snapshot.put("discountType", combo.getDiscountType().name());
            snapshot.put("discountValue", combo.getDiscountValue());
            snapshot.put("discountAmount", discountAmount);
            snapshot.put("startTime", combo.getStartTime().toString());
            snapshot.put("endTime", combo.getEndTime().toString());
            snapshot.put("appliedAt", now.toString());
            order.setComboSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            log.warn("Không thể serialize combo snapshot: {}", e.getMessage());
        }

        log.info("Áp dụng combo vào đơn hàng: comboId={}, discountAmount={}", comboId, discountAmount);
        return discountAmount;
    }

    private void validateInventory(Inventory inventory, int quantity) {
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
    }

    private void updateInventoryStock(Inventory inventory, int quantity) {
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    private OrderItemType resolveOrderItemType(ProductVariant productVariant) {
        if (productVariant.getOrderItemType() == null) {
            throw new AppException(ErrorCode.PRODUCT_PRESCRIPTION_REQUIRED);
        }
        return productVariant.getOrderItemType();
    }


    public PaymentRequirementResponse getPaymentRequirement(PaymentRequirementRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.LIST_EMPTY);
        }

        List<PaymentCalculationService.PaymentItemInput> paymentItems = request.getItems().stream()
                .map(this::toPaymentItemInput)
                .toList();

        PaymentCalculationService.PaymentCalculationResult paymentCalculation =
                paymentCalculationService.calculatePaymentRequirementForPreview(paymentItems);

        BigDecimal remainingPaymentTotal = paymentCalculation.getOrderTotal()
                .subtract(paymentCalculation.getRequiredPaymentTotal());

        boolean allowCOD = paymentCalculation.getRequiredPaymentTotal().compareTo(BigDecimal.ZERO) == 0;
        String message = "So tien can thanh toan: gia san pham IN_STOCK 100%, PRE_ORDER 50%; phi lam trong thanh toan truoc 100%.";

        List<PaymentRequirementItemResponse> itemResponses = paymentCalculation.getItemRequirements().stream()
                .map(item -> PaymentRequirementItemResponse.builder()
                        .orderItemId(item.getOrderItemId())
                        .orderItemType(item.getOrderItemType())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lensPrice(item.getLensPrice())
                        .lensPriceTotal(item.getLensPriceTotal())
                        .baseItemTotal(item.getBaseItemTotal())
                        .itemTotal(item.getItemTotal())
                        .paymentPercentage(item.getPaymentPercentage())
                        .requiredPayment(item.getRequiredPayment())
                        .build())
                .toList();

        double depositPercentage = paymentCalculation.getOrderTotal().compareTo(BigDecimal.ZERO) == 0
                ? 0
                : paymentCalculation.getRequiredPaymentTotal().divide(
                paymentCalculation.getOrderTotal(), 4, RoundingMode.HALF_UP).doubleValue();

        return PaymentRequirementResponse.builder()
                .depositPercentage(depositPercentage)
                .requiredAmount(paymentCalculation.getRequiredPaymentTotal())
                .orderTotal(paymentCalculation.getOrderTotal())
                .requiredPaymentTotal(paymentCalculation.getRequiredPaymentTotal())
                .remainingPaymentTotal(remainingPaymentTotal)
                .itemRequirements(itemResponses)
                .allowCOD(allowCOD)
                .message(message)
                .build();

    }

    private PaymentCalculationService.PaymentItemInput toPaymentItemInput(PaymentRequirementItemRequest item) {
        if ((item.getProductVariantId() == null || item.getProductVariantId().isBlank())
                && (item.getLensId() == null || item.getLensId().isBlank())) {
            throw new AppException(ErrorCode.FIELD_MISSING);
        }

        ProductVariant productVariant = null;
        if (item.getProductVariantId() != null && !item.getProductVariantId().isBlank()) {
            productVariant = productVariantRepository.findAllByIdAndStatus(
                            item.getProductVariantId(), ProductVariantStatus.ACTIVE)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        }

        Lens lens = null;
        if (item.getLensId() != null && !item.getLensId().isBlank()) {
            lens = lensRepository.findById(item.getLensId())
                    .orElseThrow(() -> new AppException(ErrorCode.LENS_NOT_FOUND));
        }

        OrderItemType orderItemType = productVariant != null
                ? productVariant.getOrderItemType()
                : OrderItemType.IN_STOCK;

        BigDecimal unitPrice = productVariant != null ? normalizeAmount(productVariant.getPrice()) : BigDecimal.ZERO;
        BigDecimal lensPrice = lens != null ? normalizeAmount(lens.getPrice()) : BigDecimal.ZERO;
        String itemId = productVariant != null ? productVariant.getId() : lens.getId();

        return PaymentCalculationService.PaymentItemInput.builder()
                .orderItemId(itemId)
                .orderItemType(orderItemType)
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .lensPrice(lensPrice)
                .build();
    }

    @Transactional
    public PrescriptionResponse uploadPrescriptionImage(String orderItemId, MultipartFile file) throws IOException {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        String url = fileStorageService.uploadFile(file, S3ImageName.PRESCRIPTION);

        Prescription prescription = orderItem.getPrescription();
        if (prescription == null) {
            prescription = new Prescription();
        }

        if (prescription.getImageUrl() != null) {
            fileStorageService.deleteFileByKey(prescription.getImageUrl());
        }

        prescription.setImageUrl(url);
        prescription = prescriptionRepository.save(prescription);

        orderItem.setPrescription(prescription);
        orderItemRepository.save(orderItem);

        return prescriptionMapper.toPrescriptionResponse(prescription);
    }

    public List<OrderResponse> getMyOrders() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return orderRepository.findByCustomerId(userId).stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse updateOrder(String orderId, OrderUpdateRequest request) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String currentId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!orders.getCustomer().getId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!orders.getStatus().equals(OrderStatus.PENDING) && !orders.getStatus().equals(OrderStatus.ON_HOLD)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (request.getDeliveryAddress() != null) {
            orders.setDeliveryAddress(request.getDeliveryAddress());
        }
        if (request.getPhoneNumber() != null) {
            orders.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getItems() != null) {
            for (OrderItemUpdateRequest requestItem : request.getItems()) {
                OrderItem orderItem = orderItemRepository.findById(requestItem.getOrderItemId())
                        .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

                if (orderItem.getQuantity() != null && !orderItem.getQuantity().equals(requestItem.getQuantity())) {
                    updateQuantityAndInventory(orderItem, requestItem.getQuantity());
                }

                if (requestItem.getPrescription() != null) {
                    updatePrescriptionLogic(orderItem, requestItem.getPrescription());
                }
            }
        }
        return orderMapper.toOrderResponse(orderRepository.save(orders));
    }

    @Transactional
    public PrescriptionResponse updatePrescription(String orderItemId, PrescriptionRequest prescriptionRequest) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        Orders orders = orderItem.getOrder();
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!orders.getCustomer().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!orders.getStatus().equals(OrderStatus.PENDING) && !orders.getStatus().equals(OrderStatus.ON_HOLD)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        updatePrescriptionLogic(orderItem, prescriptionRequest);
        return prescriptionMapper.toPrescriptionResponse(orderItem.getPrescription());
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderStatus> cancellableStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.AWAITING_VERIFICATION,
                OrderStatus.ON_HOLD);

        if (!cancellableStatuses.contains(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        for (OrderItem item : order.getItems()) {
            Inventory inventory = item.getInventory();
            if (inventory != null) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getMyCancelledOrders() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return orderRepository.findByCustomerIdAndStatus(userId, OrderStatus.CANCELLED)
                .stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse completeOrder(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getCustomer().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.COMPLETED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    /*
     * ===================== 2. MANAGEMENT FLOW (APIs cho Admin/Sales)
     * =====================
     */

    public OrderResponse getOrderById(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String currentId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getCustomer().getId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse verifyOrder(String orderId, boolean isApproved) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.PENDING) &&
                !order.getStatus().equals(OrderStatus.ON_HOLD) &&
                !order.getStatus().equals(OrderStatus.AWAITING_VERIFICATION)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }


        boolean requiresProcessing = order.getItems().stream()
                .anyMatch(this::requiresProcessing);

        if (isApproved) {
            order.setStatus(requiresProcessing ? OrderStatus.PROCESSING : OrderStatus.CONFIRMED);
        } else {
            order.setStatus(OrderStatus.ON_HOLD);
        }
        Orders savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse revertVerification(String orderId) {

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new AppException(ErrorCode.CANNOT_REVERT_STATUS);
        }

        OrderStatus currentStatus = order.getStatus();
        OrderStatus previousStatus = OrderStatus.AWAITING_VERIFICATION;

        order.setStatus(previousStatus);
        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }


    @Transactional
    public OrderResponse rejectOrder(String orderId, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.ON_HOLD)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        for (OrderItem item : order.getItems()) {
            Inventory inventory = item.getInventory();
            if (inventory != null) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                inventoryRepository.save(inventory);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (status == null) {
            return orderRepository.findAll(sort).stream().map(orderMapper::toOrderResponse).toList();
        }

        return orderRepository.findByStatus(status)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByCustomerId(String customerId) {
        userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return orderRepository.findByCustomerId(customerId)
                .stream().map(orderMapper::toOrderResponse).toList();
    }

    /*
     * ===================== 3. PRODUCTION FLOW
     * =====================
     */

    @Transactional
    public OrderResponse startProduction(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.PROCESSING)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        for (OrderItem orderItem : order.getItems()) {
            if (requiresProcessing(orderItem)) {

                orderItem.setStatus(OrderItemStatus.IN_PRODUCTION);

            } else {

                orderItem.setStatus(OrderItemStatus.PRODUCED);
            }
        }
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse finishProduction(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.PROCESSING)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getItems() != null) {
            for (OrderItem orderItem : order.getItems()) {
                if (requiresProcessing(orderItem)
                        || orderItem.getStatus() == OrderItemStatus.IN_PRODUCTION) {

                    orderItem.setStatus(OrderItemStatus.PRODUCED);
                }
            }
        }
        order.setStatus(OrderStatus.PRODUCED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderItemProductionStatus(String orderItemId, OrderItemStatus status) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (!requiresProcessing(orderItem)) {

            throw new AppException(ErrorCode.INVALID_ORDER_ITEM_TYPE);
        }

        orderItem.setStatus(status);
        orderItemRepository.save(orderItem);

        Orders order = orderItem.getOrder();
        boolean allFinished = order.getItems().stream()
                .allMatch(item -> item.getStatus().equals(OrderItemStatus.PRODUCED));

        boolean anyInProduction = order.getItems().stream()
                .allMatch(item -> item.getStatus().equals(OrderItemStatus.IN_PRODUCTION));

        if (allFinished) {
            order.setStatus(OrderStatus.PRODUCED);
        } else if (anyInProduction) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse markAsPrepared(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getOrderItemType() == OrderItemType.IN_STOCK) {
                if (order.getStatus() != OrderStatus.PREPARING) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            } else {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }
        order.setStatus(OrderStatus.PREPARED);
        return orderMapper.toOrderResponse(order);
    }



    @Transactional
    public List<OrderResponse> markAsReadyToShip(List<String> orderIds) {
        List<OrderResponse> responses = new ArrayList<>();
        for (String orderId : orderIds) {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            for (OrderItem orderItem : order.getItems()) {
                if (requiresProcessing(orderItem)) {
                    if (order.getStatus() != OrderStatus.PRODUCED) {
                        throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                    }
                } else if (orderItem.getOrderItemType() == OrderItemType.IN_STOCK) {
                    if (order.getStatus() != OrderStatus.PREPARED
                            && order.getStatus() != OrderStatus.PRODUCED) {
                        throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                    }
                }
            }
            order.setStatus(OrderStatus.READY_TO_SHIP);
            responses.add(orderMapper.toOrderResponse(order));
        }
        return responses;
    }



    @Transactional
    public void deleteOrder(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getStatus().equals(OrderStatus.CANCELLED)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        for (OrderItem item : order.getItems()) {
            Inventory inventory = item.getInventory();
            if (inventory != null) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                inventoryRepository.save(inventory);
            }
        }
        orderRepository.delete(order);
    }


    /*
     * ===================== 4. LOGISTICS FLOW (Vận chuyển & Kết thúc)
     * =====================
     */

    @Transactional
    public OrderResponse acceptOrder(String orderId, String shipperId) {

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if(order.getStatus() != OrderStatus.READY_TO_SHIP){
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setShipperId(shipperId);
        order.setShippedAt(LocalDateTime.now());

        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse startDelivery(String orderId, String shipperId){
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if(!shipperId.equals(order.getShipperId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if(order.getStatus() != OrderStatus.SHIPPED){
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.DELIVERING);
        orderRepository.save(order);
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse confirmDelivered(String orderId, String shipperId){
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if(!shipperId.equals(order.getShipperId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if(order.getStatus() != OrderStatus.DELIVERING){
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        order.setStatus(OrderStatus.COMPLETED);

        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }




    /*
     * ===================== 5. PRICE CHECK & COMBO QUERY (APIs mới)
     * =====================
     */

    /**
     * POST /api/orders/price-check
     * Tính toán giá cuối cùng sau khi áp dụng combo và phát hiện xung đột giá:
     * - Giảm giá vượt ngưỡng (> 50%)
     * - Giá bán thấp hơn mức cho phép (< 10.000đ)
     */
    public PriceCheckResponse priceCheck(PriceCheckRequest request) {
        // Ngưỡng cảnh báo
        final BigDecimal MAX_DISCOUNT_PERCENT = BigDecimal.valueOf(50);
        final BigDecimal MIN_FINAL_PRICE = BigDecimal.valueOf(10000);

        List<PriceCheckResponse.PriceCheckItemDetail> itemDetails = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        // 1. Tính tổng giá gốc và chi tiết từng item
        for (PriceCheckItemRequest itemReq : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            originalTotal = originalTotal.add(lineTotal);

            String productName = variant.getProduct() != null ? variant.getProduct().getName() : null;
            String skuLabel = buildSkuLabel(variant);

            itemDetails.add(PriceCheckResponse.PriceCheckItemDetail.builder()
                    .productVariantId(variant.getId())
                    .productName(productName)
                    .skuLabel(skuLabel)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(variant.getPrice())
                    .lineTotal(lineTotal)
                    .build());
        }

        // 2. Tính combo discount nếu có
        BigDecimal comboDiscount = BigDecimal.ZERO;
        List<PriceCheckResponse.PriceConflictWarning> warnings = new ArrayList<>();

        if (request.getComboId() != null && !request.getComboId().isBlank()) {
            Optional<Combo> optCombo = comboRepository.findById(request.getComboId());
            if (optCombo.isEmpty()) {
                warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                        .type("COMBO_NOT_FOUND")
                        .message("Combo không tồn tại: " + request.getComboId())
                        .build());
            } else {
                Combo combo = optCombo.get();

                // Validate combo status & thời gian
                LocalDateTime now = LocalDateTime.now();
                if (combo.getStatus() != ComboStatus.ACTIVE) {
                    warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                            .type("COMBO_NOT_ACTIVE")
                            .message("Combo không ở trạng thái ACTIVE (hiện tại: " + combo.getStatus() + ")")
                            .build());
                } else if (now.isBefore(combo.getStartTime()) || now.isAfter(combo.getEndTime())) {
                    warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                            .type("COMBO_EXPIRED")
                            .message("Combo ngoài thời gian hiệu lực")
                            .build());
                } else {
                    // Validate rule: items có match combo không
                    Map<String, Integer> itemMap = new HashMap<>();
                    for (PriceCheckItemRequest pi : request.getItems()) {
                        itemMap.merge(pi.getProductVariantId(), pi.getQuantity(), Integer::sum);
                    }

                    boolean ruleMatch = true;
                    for (ComboItem comboItem : combo.getComboItems()) {
                        if (comboItem.getProductVariant() != null) {
                            String skuId = comboItem.getProductVariant().getId();
                            Integer qty = itemMap.get(skuId);
                            if (qty == null || qty < comboItem.getRequiredQuantity()) {
                                ruleMatch = false;
                                break;
                            }
                        }
                    }

                    if (!ruleMatch) {
                        warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                                .type("COMBO_RULE_NOT_MATCH")
                                .message("Danh sách sản phẩm không đáp ứng yêu cầu combo")
                                .build());
                    } else {
                        // Tính discount
                        if (combo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                            comboDiscount = combo.getDiscountValue();
                        } else {
                            BigDecimal comboItemsTotal = BigDecimal.ZERO;
                            for (ComboItem ci : combo.getComboItems()) {
                                if (ci.getProductVariant() != null && ci.getProductVariant().getPrice() != null) {
                                    comboItemsTotal = comboItemsTotal.add(
                                            ci.getProductVariant().getPrice()
                                                    .multiply(BigDecimal.valueOf(ci.getRequiredQuantity()))
                                    );
                                }
                            }
                            comboDiscount = comboItemsTotal.multiply(combo.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        }
                    }
                }
            }
        }

        // 3. Tính giá cuối cùng
        BigDecimal finalTotal = originalTotal.subtract(comboDiscount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // 4. Kiểm tra xung đột giá
        boolean isValid = true;

        // 4a. Giảm giá vượt ngưỡng
        if (originalTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountPercent = comboDiscount.multiply(BigDecimal.valueOf(100))
                    .divide(originalTotal, 2, RoundingMode.HALF_UP);
            if (discountPercent.compareTo(MAX_DISCOUNT_PERCENT) > 0) {
                isValid = false;
                warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                        .type("DISCOUNT_EXCEEDS_THRESHOLD")
                        .message("Giảm giá vượt ngưỡng cho phép (" + MAX_DISCOUNT_PERCENT + "%)")
                        .threshold(MAX_DISCOUNT_PERCENT)
                        .actualValue(discountPercent)
                        .build());
            }
        }

        // 4b. Giá bán thấp hơn mức cho phép
        if (finalTotal.compareTo(MIN_FINAL_PRICE) < 0 && originalTotal.compareTo(BigDecimal.ZERO) > 0) {
            isValid = false;
            warnings.add(PriceCheckResponse.PriceConflictWarning.builder()
                    .type("BELOW_MIN_PRICE")
                    .message("Giá cuối cùng thấp hơn mức tối thiểu cho phép (" + MIN_FINAL_PRICE + "đ)")
                    .threshold(MIN_FINAL_PRICE)
                    .actualValue(finalTotal)
                    .build());
        }

        log.info("Price check: original={}, comboDiscount={}, final={}, isValid={}, warnings={}",
                originalTotal, comboDiscount, finalTotal, isValid, warnings.size());

        return PriceCheckResponse.builder()
                .originalTotal(originalTotal)
                .comboDiscount(comboDiscount)
                .finalTotal(finalTotal)
                .isValid(isValid)
                .itemDetails(itemDetails)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    /**
     * GET /api/orders/{orderId} — Xem chi tiết đơn hàng kèm combo
     * Dành cho Sales, Manager, Customer
     */
    public OrderResponse getOrderDetailWithCombo(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toOrderResponse(order);
    }

    private String buildSkuLabel(ProductVariant variant) {
        if (variant == null) return null;
        String color = variant.getColorName();
        String size = variant.getSizeLabel();
        if (color != null && size != null) return color + " - " + size;
        if (color != null) return color;
        if (size != null) return size;
        return variant.getId();
    }

    /* ===================== 6. PRIVATE LOGIC (Hàm phụ trợ) ===================== */

    private void updatePrescriptionLogic(OrderItem orderItem, PrescriptionRequest prescriptionRequest) {
        Prescription prescription = orderItem.getPrescription();
        if (prescription == null) {
            prescription = new Prescription();
        }
        prescriptionMapper.updatePrescription(prescription, prescriptionRequest);
        prescription = prescriptionRepository.save(prescription);

        orderItem.setPrescription(prescription);
        orderItemRepository.save(orderItem);
    }

    private void updateQuantityAndInventory(OrderItem item, Integer newQty) {
        Inventory inventory = item.getInventory();
        int diff = newQty - item.getQuantity();

        if (diff > 0) {
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < diff)
                throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + diff);
        inventory.setQuantity(inventory.getQuantity() - diff);
        inventoryRepository.save(inventory);

        item.setQuantity(newQty);
        item.setTotalPrice(calculateOrderItemTotal(item.getUnitPrice(), item.getLensPrice(), newQty));
    }

    private BigDecimal calculateOrderItemTotal(BigDecimal unitPrice, BigDecimal lensPrice, Integer quantity) {
        BigDecimal normalizedUnitPrice = normalizeAmount(unitPrice);
        BigDecimal normalizedLensFee = normalizeAmount(lensPrice);
        BigDecimal qty = BigDecimal.valueOf(quantity == null ? 0 : quantity);
        return normalizedUnitPrice.add(normalizedLensFee).multiply(qty);
    }

    private boolean requiresProcessing(OrderItem orderItem) {
        return orderItem.getOrderItemType() == OrderItemType.PRE_ORDER
                || orderItem.getPrescription() != null
                || (orderItem.getLensId() != null && !orderItem.getLensId().isBlank());
    }

    private Lens resolveLens(String lensId) {
        if (lensId == null || lensId.isBlank()) {
            return null;
        }
        return lensRepository.findById(lensId)
                .orElseThrow(() -> new AppException(ErrorCode.LENS_NOT_FOUND));
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
