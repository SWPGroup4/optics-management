package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.*;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.dto.response.PrescriptionResponse;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.OrderItemStatus;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.OrderMapper;
import com.glassystem.optics.mapper.PrescriptionMapper;
import com.glassystem.optics.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /* ===================== 1. CUSTOMER FLOW (APIs cho khách hàng) ===================== */

    @Transactional
    public OrderResponse createOrder(OrderCreationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDate.now());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemCreationRequest reqItem : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductVariantId(reqItem.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < reqItem.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() + reqItem.getQuantity());
            inventory.setQuantity(inventory.getQuantity() - reqItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setOrderItemType(reqItem.getOrderItemType());
            item.setInventory(inventory);
            item.setQuantity(reqItem.getQuantity());
            item.setUnitPrice(inventory.getProductVariant().getPrice());

            BigDecimal itemTotalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            item.setTotalPrice(itemTotalPrice);

            if(reqItem.getPrescription() != null) {
                Prescription prescription = prescriptionMapper.toPrescription(reqItem.getPrescription());
                prescription = prescriptionRepository.save(prescription);
                item.setPrescription(prescription);
            }

            order.getItems().add(item);
            totalAmount = totalAmount.add(inventory.getProductVariant().getPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity())));
        }
        order.setTotalAmount(totalAmount);
        return orderMapper.toOrderResponse(orderRepository.save(order));
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
        if (request.getItems() != null) {
            for (OrderItemUpdateRequest requestItem : request.getItems()) {
                OrderItem orderItem = orderItemRepository.findById(requestItem.getOrderItemId())
                        .orElseThrow(() -> new AppException(ErrorCode.ORDER_ITEM_NOT_FOUND));

                if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
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
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
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

    /* ===================== 2. MANAGEMENT FLOW (APIs cho Admin/Sales) ===================== */

    public List<OrderResponse> getOrders() {
        return orderRepository.findAll().stream().map(orderMapper::toOrderResponse).toList();
    }

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
    public OrderResponse verifyOrder(String orderId, boolean isPrescriptionValid) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.ON_HOLD)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        boolean hasInvalidPrescription = false;
        boolean hasPrescriptionItem = false;

        for (OrderItem item : order.getItems()) {
            if (item.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
                hasPrescriptionItem = true;
                if (!isPrescriptionValid) {
                    hasInvalidPrescription = true;
                    break;
                }
            }
        }

        if (hasPrescriptionItem && hasInvalidPrescription) {
            order.setStatus(OrderStatus.ON_HOLD);
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
        }
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
        return orderRepository.findByStatus(status)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    /* ===================== 3. PRODUCTION FLOW (APIs cho Kỹ thuật/Sản xuất) ===================== */

    @Transactional
    public OrderResponse startProduction(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().equals(OrderStatus.CONFIRMED)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        boolean hasPrescriptionItem = order.getItems().stream()
                .anyMatch(orderItem -> orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION));

        if (!hasPrescriptionItem) {
            throw new AppException(ErrorCode.INVALID_ORDER_ITEM_TYPE);
        }
        order.setStatus(OrderStatus.PROCESSING);

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
                if (orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION)) {
                    orderItem.setStatus(OrderItemStatus.PRODUCED);
                }
            }
        }
        order.setStatus(OrderStatus.PRODUCED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrdersFinishProduction() {
        return orderRepository.findByStatus(OrderStatus.PRODUCED).stream().map(orderMapper::toOrderResponse).toList();
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

        if (allFinished) {
            order.setStatus(OrderStatus.PRODUCED);
        }

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    /* ===================== 4. LOGISTICS FLOW (Vận chuyển & Kết thúc) ===================== */

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
                if (!order.getStatus().equals(OrderStatus.CONFIRMED) && !order.getStatus().equals(OrderStatus.PRODUCED)) {
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
}