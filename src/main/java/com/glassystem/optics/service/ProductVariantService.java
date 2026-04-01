package com.glassystem.optics.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.glassystem.optics.dto.request.InventoryUpdateRequest;
import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.InventoryQuantityUpdateResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.dto.response.ProductVariantPageResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.entity.Inventory;
import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.NotificationTemplate;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.PreOrderStatus;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.OrderMapper;
import com.glassystem.optics.mapper.ProductVariantMapper;
import com.glassystem.optics.repository.InventoryRepository;
import com.glassystem.optics.repository.OrderItemRepository;
import com.glassystem.optics.repository.OrderRepository;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.repository.ProductVariantRepository;
import com.glassystem.optics.specification.ProductVariantSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantService {

    ProductVariantRepository productVariantRepository;
    ProductRepository productRepository;
    ProductVariantMapper productVariantMapper;
    InventoryRepository inventoryRepository;
    OrderItemRepository orderItemRepository;
    OrderRepository orderRepository;
    OrderMapper orderMapper;
    NotificationService notificationService;
	public List<ProductVariantResponse> getActiveVariantsByProductId(String productId) {
		return productVariantRepository
				.findAllByProductIdAndStatus(productId, ProductVariantStatus.ACTIVE)
				.stream()
				.map(productVariantMapper::toResponse)
				.toList();
	}


    @Transactional
    public ProductVariantResponse create(ProductVariantRequest request) {
        Optional<ProductVariant> existingVariant = productVariantRepository
                .findByProductIdAndColorNameAndSizeLabel( //unique 3 cai nay
                        request.getProductId(),
                        request.getColorName(),
                        request.getSizeLabel()
                );

        if (existingVariant.isPresent()) { //varian ton tai r
            ProductVariant productVariant = existingVariant.get();
            Inventory inventory = inventoryRepository.findByProductVariantId(productVariant.getId()) //lay inventory
                    .orElseGet(() -> inventoryRepository.save(  //neu chua co inven thi tao
                            Inventory.builder()
                                    .productVariant(productVariant)
                                    .quantity(0)
                                    .reservedQuantity(0)
                                    .build()
                    ));

            int currentQuantity = safeInt(inventory.getQuantity());
            int addQuantity = safeInt(request.getQuantity());
            int newQuantity = currentQuantity + addQuantity; // con trong kho + them moi

            inventory.setQuantity(newQuantity);
            inventoryRepository.save(inventory); // set sl moi va luu

            syncVariantStockState(productVariant, newQuantity); //trang thai moi
            productVariantRepository.save(productVariant);

            return productVariantMapper.toResponse(productVariant);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductVariant variant = productVariantMapper.toProductVariant(request);
        variant.setProduct(product); //tạo variant

        int initialQuantity = safeInt(request.getQuantity());
        syncVariantStockState(variant, initialQuantity); //set trang thai

        variant = productVariantRepository.save(variant);

        Inventory inventory = Inventory.builder() //tao inven
                .productVariant(variant)
                .quantity(initialQuantity)
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(inventory);

        return productVariantMapper.toResponse(variant);
    }

    @Transactional
    public InventoryQuantityUpdateResponse updateInventoryQuantity(InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductVariantId(request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        ProductVariant variant = inventory.getProductVariant();
        if (variant == null) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }

        InventoryUpdateResult updateResult = applyInventoryQuantity(variant, inventory, safeInt(request.getChangeAmount()));

        return InventoryQuantityUpdateResponse.builder()
                .productVariant(productVariantMapper.toResponse(updateResult.variant()))
                .updatedOrderCount(updateResult.updatedOrders().size())
                .updatedOrders(updateResult.updatedOrders())
                .build();
    }

    private List<OrderResponse> releaseEligiblePreOrdersForVariant(String variantId) {
        List<Orders> candidateOrders = orderRepository.findEligiblePreOrdersForVariant(
                variantId,
                OrderItemType.PRE_ORDER,
                PreOrderStatus.DEPOSIT_PAID,
                OrderStatus.PROCESSING
        );

        List<OrderResponse> updatedOrders = new ArrayList<>();

        for (Orders order : candidateOrders) {
            if (!canReleaseOrder(order)) {
                continue;
            }

            allocateStockForPreOrderItems(order);

            order.setStatus(OrderStatus.AWAITING_FINAL_PAYMENT);
            order.setPreOrderStatus(PreOrderStatus.REMAINING_PENDING);

            Orders savedOrder = orderRepository.saveAndFlush(order);
            sendRemainingPaymentDueNotification(savedOrder);
            updatedOrders.add(orderMapper.toOrderResponse(savedOrder));
        }

        return updatedOrders;
    }

    private void sendRemainingPaymentDueNotification(Orders order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getId() == null) {
            return;
        }

        BigDecimal remainingAmount = order.getRemainingAmount() == null ? BigDecimal.ZERO : order.getRemainingAmount();
        String amountText = remainingAmount.stripTrailingZeros().toPlainString();

        notificationService.createSystemNotification(
                order.getCustomer().getId(),
                NotificationTemplate.REMAINING_PAYMENT_DUE,
                order.getId(),
                amountText
        );
    }

    private boolean canReleaseOrder(Orders order) {
        Map<String, Integer> requiredByVariant = new HashMap<>();

        for (OrderItem item : order.getItems()) {
            if (item.getOrderItemType() != OrderItemType.PRE_ORDER || item.getProductVariant() == null) {
                continue;
            }

            requiredByVariant.merge(
                    item.getProductVariant().getId(),
                    safeInt(item.getQuantity()),
                    Integer::sum
            );
        }

        for (Map.Entry<String, Integer> entry : requiredByVariant.entrySet()) {
            Inventory inventory = inventoryRepository.findByProductVariantId(entry.getKey())
                    .orElse(null);

            int available = inventory == null ? 0 : safeInt(inventory.getQuantity());
            if (available < entry.getValue()) {
                return false;
            }
        }

        return !requiredByVariant.isEmpty();
    }

    private void allocateStockForPreOrderItems(Orders order) {
        for (OrderItem item : order.getItems()) {
            if (item.getOrderItemType() != OrderItemType.PRE_ORDER || item.getProductVariant() == null) {
                continue;
            }

            Inventory inventory = inventoryRepository.findByProductVariantId(item.getProductVariant().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

            int available = safeInt(inventory.getQuantity());
            int reserved = safeInt(inventory.getReservedQuantity());
            int itemQty = safeInt(item.getQuantity());

            if (available < itemQty) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setQuantity(available - itemQty);
            inventory.setReservedQuantity(reserved + itemQty);
            inventoryRepository.saveAndFlush(inventory);

            item.setInventory(inventory);
            item.setOrderItemType(OrderItemType.IN_STOCK);
            orderItemRepository.saveAndFlush(item);

            ProductVariant variant = item.getProductVariant();
            syncVariantStockState(variant, inventory.getQuantity());
            productVariantRepository.saveAndFlush(variant);
        }
    }

    private void syncVariantStockState(ProductVariant variant, int quantity) {
        variant.setQuantity(quantity);

        if (quantity > 0) {
            variant.setOrderItemType(OrderItemType.IN_STOCK);
        } else {
            variant.setOrderItemType(OrderItemType.PRE_ORDER);
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public ProductVariantResponse getById(String id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        return productVariantMapper.toResponse(variant);
    }

    @Transactional
    public ProductVariantResponse update(String id, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        productVariantMapper.updateEntity(variant, request);
        variant.setProduct(product);

        variant = productVariantRepository.save(variant);
        final ProductVariant finalVariant = variant;

        inventoryRepository.findByProductVariantId(variant.getId())
                .orElseGet(() -> inventoryRepository.saveAndFlush(
                        Inventory.builder()
                                .productVariant(finalVariant)
                                .quantity(0)
                                .reservedQuantity(0)
                                .build()
                ));

        InventoryQuantityUpdateResponse updateResponse = updateInventoryQuantity(
                InventoryUpdateRequest.builder()
                        .productVariantId(variant.getId())
                        .changeAmount(safeInt(request.getQuantity()))
                        .build()
        );

        return updateResponse.getProductVariant();
    }

    private InventoryUpdateResult applyInventoryQuantity(ProductVariant variant, Inventory inventory, int quantity) {
        inventory.setQuantity(quantity);
        inventoryRepository.saveAndFlush(inventory);

        syncVariantStockState(variant, quantity);
        productVariantRepository.saveAndFlush(variant);

        List<OrderResponse> updatedOrders = releaseEligiblePreOrdersForVariant(variant.getId());

        ProductVariant refreshedVariant = productVariantRepository.findById(variant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        return new InventoryUpdateResult(refreshedVariant, updatedOrders);
    }

    private record InventoryUpdateResult(
            ProductVariant variant,
            List<OrderResponse> updatedOrders
    ) {
    }

    public void delete(String id) {
        ProductVariant productVariant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        productVariant.setIsDeleted(true);
        productVariantRepository.save(productVariant);
    }

    public List<ProductVariantResponse> getVariants() {
        return productVariantRepository.findAll().stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    public ProductVariantPageResponse filterVariants(
            String q,
            String productId,
            String colorName,
            String frameFinish,
            String sizeLabel,
            Integer lensWidthMm,
            Integer bridgeWidthMm,
            Integer templeLengthMm,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductVariantStatus status,
            Pageable pageable) {

        Page<ProductVariant> page = productVariantRepository.findAll(
                ProductVariantSpecifications.build(
                        q,
                        productId,
                        colorName,
                        frameFinish,
                        sizeLabel,
                        lensWidthMm,
                        bridgeWidthMm,
                        templeLengthMm,
                        minPrice,
                        maxPrice,
                        status
                ),
                pageable
        );

        return ProductVariantPageResponse.builder()
                .items(page.getContent().stream().map(productVariantMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
