package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.*;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.dto.response.PaymentRequirementResponse;
import com.glassystem.optics.dto.response.PrescriptionResponse;
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
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderService {
    private final OrderMapper orderMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final FileStorageService fileStorageService;
    private final PaymentRepository paymentRepository;

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

        boolean hasPrescription = request.getItems().stream()
                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRESCRIPTION);
        boolean hasPreOrder = request.getItems().stream()
                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRE_ORDER);

        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean fileUploaded = false;

        for (OrderItemCreationRequest orderItemRequest : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductVariantId(orderItemRequest.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

            validateInventory(inventory, orderItemRequest.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setOrderItemType(orderItemRequest.getOrderItemType());
            item.setInventory(inventory);
            item.setQuantity(orderItemRequest.getQuantity());
            item.setUnitPrice(inventory.getProductVariant().getPrice());
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(orderItemRequest.getQuantity())));

            if (orderItemRequest.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
                Prescription prescription = new Prescription();
                if (orderItemRequest.getPrescription() != null) {
                    prescriptionMapper.updatePrescription(prescription, orderItemRequest.getPrescription());
                }
                if (file != null && !file.isEmpty() && !fileUploaded) {
                    String url = fileStorageService.uploadFile(file, S3ImageName.PRESCRIPTION);
                    prescription.setImageUrl(url);
                    fileUploaded = true;
                }
                item.setPrescription(prescriptionRepository.save(prescription));
            }

            updateInventoryStock(inventory, orderItemRequest.getQuantity());
            order.getItems().add(item);
            totalAmount = totalAmount.add(inventory.getProductVariant().getPrice()
                    .multiply(BigDecimal.valueOf(orderItemRequest.getQuantity())));
        }

        if (hasPrescription) {
            order.setDepositAmount(totalAmount);
            order.setRemainingAmount(BigDecimal.ZERO);
            order.setPaymentMethod(PaymentMethod.VNPAY);
        } else if (hasPreOrder) {
            order.setDepositAmount(totalAmount.multiply(BigDecimal.valueOf(0.5)));
            order.setRemainingAmount(totalAmount.multiply(BigDecimal.valueOf(0.5)));
            order.setPreOrderStatus(PreOrderStatus.DEPOSIT_PENDING);
            order.setPaymentMethod(PaymentMethod.VNPAY);

            for (OrderItem item : order.getItems()) {
                if (item.getOrderItemType().equals(OrderItemType.PRE_ORDER)) {
                    item.setDepositPrice(item.getTotalPrice().multiply(BigDecimal.valueOf(0.5)));
                    item.setRemainingPrice(item.getTotalPrice().multiply(BigDecimal.valueOf(0.5)));
                }
            }
        } else {
            order.setDepositAmount(BigDecimal.ZERO);
            PaymentMethod paymentMethod = request.getPaymentMethod() != null
                    ? request.getPaymentMethod()
                    : PaymentMethod.COD;
            order.setPaymentMethod(paymentMethod);
            if (paymentMethod == PaymentMethod.VNPAY) {
                order.setDepositAmount(totalAmount);
            } else {
                order.setDepositAmount(BigDecimal.ZERO);
            }
        }

        order.setTotalAmount(totalAmount);
        return orderMapper.toOrderResponse(orderRepository.save(order));
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

    @Transactional
    public PaymentRequirementResponse getPaymentRequirement(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> items = order.getItems();

        boolean hasPrescription = items.stream()
                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRESCRIPTION);
        boolean hasPreOrder = items.stream()
                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRE_ORDER);

        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        boolean hasDepositPaid = payments.stream()
                .anyMatch(payment -> payment.getPaymentPurpose() == PaymentPurpose.DEPOSIT
                        && payment.getStatus() == PaymentStatus.PAID);

        double percentage = 0;
        boolean allowCOD = true;
        String message;

        if (hasPrescription) {
            percentage = 1.0;
            allowCOD = false;
            message = "Đơn hàng có sản phẩm kê đơn, bắt buộc thanh toán trước 100%.";
        } else if (hasPreOrder) {
            allowCOD = false;
            if (!hasDepositPaid) {
                percentage = 0.5;
                message = "Bắt buộc cọc 50% (pre-order)";
            } else {
                percentage = 0.5;
                message = "Đã cọc 50%, vui lòng thanh toán 50% còn lại";
            }

        } else {

            if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
                percentage = 1.0;
                allowCOD = false;
                message = "Thanh toán trước 100% với phương thức VNPAY.";
            } else {
                percentage = 0;
                message = "Đơn hàng có thể thanh toán khi nhận hàng (COD).";
            }
        }

        return PaymentRequirementResponse.builder()
                .depositPercentage(percentage)
                .requiredAmount(order.getTotalAmount().multiply(BigDecimal.valueOf(percentage)))
                .allowCOD(allowCOD)
                .message(message)
                .build();

    }

    @Transactional
    public PrescriptionResponse uploadPrescriptionImage(String orderItemId, MultipartFile file) throws IOException {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        if (!orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
            throw new AppException(ErrorCode.INVALID_ORDER_ITEM_TYPE);
        }

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

                if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)
                        && requestItem.getPrescription() != null) {
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
        if (!orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
            throw new AppException(ErrorCode.INVALID_ORDER_ITEM_TYPE);
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

        if (!order.getStatus().equals(OrderStatus.SHIPPED)) {
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
                .anyMatch(orderItem -> orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)
                        || orderItem.getOrderItemType().equals(OrderItemType.PRE_ORDER));

        if (isApproved) {
            order.setStatus(requiresProcessing ? OrderStatus.PROCESSING : OrderStatus.CONFIRMED);
        } else {
            order.setStatus(OrderStatus.ON_HOLD);
        }

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse revertVerification(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderStatus> revertsibleStatuses = List.of(
                OrderStatus.ON_HOLD,
                OrderStatus.PROCESSING,
                OrderStatus.CONFIRMED,
                OrderStatus.AWAITING_VERIFICATION);

        if (!revertsibleStatuses.contains(order.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_REVERT_STATUS);
        }

        order.setStatus(OrderStatus.PENDING);
        return orderMapper.toOrderResponse(orderRepository.save(order));
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
            if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
                orderItem.setStatus(OrderItemStatus.IN_PRODUCTION);
            } else {
                orderItem.setStatus(OrderItemStatus.PRODUCED);
            }
        }
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrdersProcessing() {
        return orderRepository.findByStatus(OrderStatus.PROCESSING).stream().map(orderMapper::toOrderResponse).toList();
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
                if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)
                        || orderItem.getStatus().equals(OrderItemStatus.IN_PRODUCTION) ) {
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

        if (!orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
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

    /*
     * ===================== 4. LOGISTICS FLOW (Vận chuyển & Kết thúc)
     * =====================
     */

    @Transactional
    public OrderResponse markAsShipped(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
                if (!order.getStatus().equals(OrderStatus.PRODUCED)) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            } else if (orderItem.getOrderItemType().equals(OrderItemType.IN_STOCK)) {
                if (!order.getStatus().equals(OrderStatus.CONFIRMED)
                        && !order.getStatus().equals(OrderStatus.PRODUCED)) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            }
        }

        order.setStatus(OrderStatus.SHIPPED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrdersShipped() {
        return orderRepository.findByStatus(OrderStatus.SHIPPED).stream().map(orderMapper::toOrderResponse).toList();
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

    /* ===================== 5. PRIVATE LOGIC (Hàm phụ trợ) ===================== */

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
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(newQty)));
    }
}