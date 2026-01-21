package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.OrderCreationRequest;
import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.OrderType;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.OrderMapper;
import com.glassystem.optics.repository.InventoryRepository;
import com.glassystem.optics.repository.OrderRepository;
import com.glassystem.optics.repository.ProductVariantRepository;
import com.glassystem.optics.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderService {
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;


    public OrderResponse createOrder(OrderCreationRequest request) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderType(request.getOrderType());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDate.now());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemCreationRequest reqItem : request.getItems()) {

            ProductVariant variant = productVariantRepository
                    .findById(reqItem.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            Inventory inventory = inventoryRepository
                    .findByProductVariantId(variant.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < reqItem.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() + reqItem.getQuantity());
            inventory.setQuantity(inventory.getQuantity() - reqItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setInventory(inventory);
            item.setQuantity(reqItem.getQuantity());
            item.setUnitPrice(variant.getPrice());
            item.setPrescriptionNote(reqItem.getPrescriptionNote());

            order.getItems().add(item);

            totalAmount = totalAmount.add(
                    variant.getPrice()
                            .multiply(BigDecimal.valueOf(reqItem.getQuantity()))
            );
        }

        order.setTotalAmount(totalAmount);

        if (order.getOrderType() == OrderType.PRE_ORDER) {
            order.setDepositAmount(totalAmount.multiply(BigDecimal.valueOf(0.5)));
        } else {
            order.setDepositAmount(BigDecimal.ZERO);
        }

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrders(){
        return orderRepository.findAll().stream().map(orderMapper::toOrderResponse).toList();
    }

    public List<OrderResponse> getMyOrders(){
        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return orderRepository.findByUserId(userId).stream().map(orderMapper::toOrderResponse).toList();
    }

    public OrderResponse getOrderById(String orderId){
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(()-> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String currentId= SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        if (!order.getCustomer().getId().equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return orderMapper.toOrderResponse(order);
    }


}
