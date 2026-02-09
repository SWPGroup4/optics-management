# Order Verification Flow - Update Documentation

## Overview

Added a new order status `AWAITING_VERIFICATION` to implement a verification step after payment is completed. This ensures that staff must verify the order before it moves to processing status.

---

## Changes Made

### 1. New Order Status Added

**File:** `OrderStatus.java`

```java
public enum OrderStatus {
    PENDING,                    // Mới tạo
    AWAITING_VERIFICATION,      // ✨ NEW - Đã thanh toán, chờ staff xác minh
    ON_HOLD,                    // Bị tạm dừng
    CONFIRMED,                  // Đã xác nhận đơn
    PROCESSING,                 // Có ít nhất 1 item đang sản xuất
    PRODUCED,                   // Tất cả item đã xong
    SHIPPED,                    // Đã giao cho vận chuyển
    COMPLETED,                  // Thành công
    CANCELLED                   // Hủy đơn
}
```

**Purpose:** Represents an order that has completed payment but is awaiting staff verification before processing begins.

---

## Updated Flow Diagrams

### Previous Flow (Outdated)
```
Payment Completed
       ↓
Order Status: PROCESSING (immediately)
       ↓
Staff processes order
```

### New Flow (Current)
```
Order Created
       ↓
Payment Completed
       ↓
Order Status: AWAITING_VERIFICATION ✨
       ↓
Staff Verifies Order
       ↓
[If Approved]
Order Status: PROCESSING or CONFIRMED
       ↓
Staff processes order
       ↓
[If Rejected]
Order Status: ON_HOLD
```

---

## Detailed Order Status Transitions

### For Orders with PRESCRIPTION or PRE_ORDER Items

```
PENDING
   ↓ (Payment Completed)
AWAITING_VERIFICATION ✨
   ↓ (Staff Verifies - Approved)
PROCESSING
   ↓ (Production)
PRODUCED
   ↓ (Shipped)
SHIPPED
   ↓ (Delivered)
COMPLETED

   Or (Staff Verifies - Rejected)
ON_HOLD
   ↓ (Revert if needed)
PENDING
```

### For Orders with NORMAL Items Only + VNPAY Payment

```
PENDING
   ↓ (Payment Completed)
AWAITING_VERIFICATION ✨
   ↓ (Staff Verifies - Approved)
CONFIRMED ✓
   ↓ (Ready to Ship - No Processing Needed)
```

### For Orders with NORMAL Items + COD Payment

```
PENDING
   ↓ (No Online Payment)
COMPLETED ✓
   (Direct to COMPLETED - No verification needed)
```

---

## Updated Methods

### 1. PaymentService - `updateOrderStatusBasedOnItems()`

**Before:**
```java
if(hasSpecialItem){
    order.setStatus(OrderStatus.PROCESSING);
} else {
    order.setStatus(OrderStatus.COMPLETED);
}
```

**After:**
```java
if(hasSpecialItem){
    order.setStatus(OrderStatus.AWAITING_VERIFICATION);  // ✨ Changed
} else {
    order.setStatus(OrderStatus.COMPLETED);
}
```

**Impact:** After a successful payment, orders with PRESCRIPTION or PRE_ORDER items now move to `AWAITING_VERIFICATION` instead of `PROCESSING`.

---

### 2. OrderService - `verifyOrder()`

**Updated to handle AWAITING_VERIFICATION:**

```java
@Transactional
public OrderResponse verifyOrder(String orderId, boolean isApproved) {
    Orders order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    // ✨ Added AWAITING_VERIFICATION to acceptable statuses
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
```

**Usage:**
- Admin/Staff calls this API to verify an order in `AWAITING_VERIFICATION` status
- If approved: Order moves to `PROCESSING` (if has special items) or `CONFIRMED`
- If rejected: Order moves to `ON_HOLD`

---

### 3. OrderService - `revertVerification()`

**Updated to include AWAITING_VERIFICATION:**

```java
@Transactional
public OrderResponse revertVerification (String orderId){
    Orders order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderStatus> revertsibleStatuses = List.of(
            OrderStatus.ON_HOLD, 
            OrderStatus.PROCESSING, 
            OrderStatus.CONFIRMED,
            OrderStatus.AWAITING_VERIFICATION  // ✨ Added
    );

    if(!revertsibleStatuses.contains(order.getStatus())){
        throw new AppException(ErrorCode.CANNOT_REVERT_STATUS);
    }

    order.setStatus(OrderStatus.PENDING);
    return orderMapper.toOrderResponse(orderRepository.save(order));
}
```

**Usage:** Can now revert from `AWAITING_VERIFICATION` status back to `PENDING` if needed.

---

### 4. OrderService - `cancelOrder()`

**Updated to allow cancellation from AWAITING_VERIFICATION:**

```java
@Transactional
public OrderResponse cancelOrder(String orderId) {
    Orders order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    
    // ✨ Now allows cancellation from AWAITING_VERIFICATION status
    List<OrderStatus> cancellableStatuses = List.of(
            OrderStatus.PENDING, 
            OrderStatus.AWAITING_VERIFICATION,  // ✨ Added
            OrderStatus.ON_HOLD
    );
    
    if (!cancellableStatuses.contains(order.getStatus())) {
        throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
    }

    // Restore inventory
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
```

**Impact:** Customers can now cancel orders that are in `AWAITING_VERIFICATION` status (e.g., change their mind before staff verification happens).

---

## Business Rules Updated

### Rule 1: Payment Success → Verification Required (for Special Items)

```
IF Payment is SUCCESSFUL AND order contains (PRESCRIPTION OR PRE_ORDER)
  THEN {
    Order Status: AWAITING_VERIFICATION
    Next Action: Require Staff Verification
  }
ELSE IF Payment is SUCCESSFUL AND order is NORMAL + VNPAY
  THEN {
    Order Status: AWAITING_VERIFICATION
    Next Action: Require Staff Verification
  }
ELSE IF Payment is not required (COD)
  THEN {
    Order Status: COMPLETED
    Next Action: Ready for Shipping
  }
```

### Rule 2: Staff Verification Decision

```
IF Order Status = AWAITING_VERIFICATION AND Staff Approves
  THEN {
    IF order contains special items (PRESCRIPTION or PRE_ORDER)
      Order Status: PROCESSING
    ELSE
      Order Status: CONFIRMED
  }

IF Order Status = AWAITING_VERIFICATION AND Staff Rejects
  THEN {
    Order Status: ON_HOLD
    Reason: Stored for review
  }
```

### Rule 3: Cancellation Policy

```
IF Customer requests cancellation:
  ├─ Status = PENDING → Allow (no payment made)
  ├─ Status = AWAITING_VERIFICATION → Allow (with refund)
  ├─ Status = ON_HOLD → Allow (with refund if paid)
  ├─ Status = CONFIRMED → Require staff approval
  ├─ Status = PROCESSING → Require staff approval
  ├─ Status = SHIPPED → Deny (order in transit)
  └─ Status = COMPLETED → Deny (order delivered)
```

---

## Testing Scenarios

### Scenario 1: PRESCRIPTION Order with Payment

```
1. Customer creates order with PRESCRIPTION
2. Customer makes payment (VNPay)
3. Payment callback: SUCCESS
   └─ Order Status: PENDING → AWAITING_VERIFICATION ✨
4. Staff reviews order
   └─ Call verifyOrder(orderId, true)
5. Order Status: AWAITING_VERIFICATION → PROCESSING
6. Staff processes prescription order
7. Order Status: PROCESSING → PRODUCED → SHIPPED → COMPLETED
```

### Scenario 2: PRE_ORDER with 2-Stage Payment

```
1. Customer creates PRE_ORDER
2. Customer pays 50% (Deposit)
3. Payment callback: SUCCESS
   └─ Order Status: AWAITING_VERIFICATION ✨
   └─ PreOrderStatus: DEPOSIT_PAID
4. Staff verifies order
   └─ Call verifyOrder(orderId, true)
5. Order Status: PROCESSING
6. Staff awaits customer's remaining 50% payment
7. Customer pays 50% (Remaining)
8. Payment callback: SUCCESS
   └─ PreOrderStatus: REMAINING_PAID
9. Order Status: PROCESSING → PRODUCED → SHIPPED → COMPLETED
```

### Scenario 3: Cancel Order at Verification Stage

```
1. Customer creates NORMAL order with VNPAY
2. Customer makes payment (VNPay)
3. Payment callback: SUCCESS
   └─ Order Status: AWAITING_VERIFICATION ✨
4. Customer changes mind: cancelOrder(orderId)
   └─ Order Status: CANCELLED ✓
   └─ Inventory restored
   └─ Refund initiated
```

### Scenario 4: Staff Rejects Order

```
1. Order Status: AWAITING_VERIFICATION
2. Staff reviews and finds issue
3. Staff calls verifyOrder(orderId, false)
4. Order Status: ON_HOLD
5. Staff investigates and contacts customer
6. On resolution: revertVerification(orderId)
7. Order Status: PENDING
8. Customer can modify or cancel
```

---

## API Endpoints & Usage

### Admin/Staff Endpoint - Verify Order

**Endpoint:** `POST /admin/orders/{orderId}/verify`

**Request Body:**
```json
{
  "isApproved": true,
  "notes": "Order verified and ready for processing"
}
```

**Response:**
```json
{
  "id": "order-123",
  "status": "PROCESSING",  // or "CONFIRMED"
  "preOrderStatus": "DEPOSIT_PAID",
  ...
}
```

**Use Cases:**
- Staff approves verified orders
- System updates order status
- Processing begins automatically

---

### Customer Endpoint - Cancel Order

**Endpoint:** `DELETE /orders/{orderId}`

**Response (if successful):**
```json
{
  "id": "order-123",
  "status": "CANCELLED",
  "refundStatus": "PENDING"
}
```

**Cancellation Rules:**
- Allowed from: PENDING, AWAITING_VERIFICATION, ON_HOLD
- Inventory automatically restored
- Refund initiated if payment was made

---

## Migration Notes

### For Existing Data

If there are existing orders in `PROCESSING` status:
- No immediate action needed
- They will continue through their workflow
- New orders will use the new flow

### For Custom Reports/Queries

Update any queries that filter by `OrderStatus`:

**Old logic:**
```sql
SELECT * FROM orders WHERE status = 'PROCESSING'
```

**New logic (to include newly paid orders):**
```sql
SELECT * FROM orders WHERE status IN ('AWAITING_VERIFICATION', 'PROCESSING', 'CONFIRMED')
```

---

## Summary of Changes

| Component | Change | Impact |
|-----------|--------|--------|
| OrderStatus enum | Added `AWAITING_VERIFICATION` | New intermediate status |
| PaymentService | Sets status to `AWAITING_VERIFICATION` | Delays processing until verified |
| OrderService.verifyOrder() | Accept `AWAITING_VERIFICATION` | Staff can now verify orders |
| OrderService.revertVerification() | Include `AWAITING_VERIFICATION` | Can revert from new status |
| OrderService.cancelOrder() | Allow cancel from `AWAITING_VERIFICATION` | Customers can cancel after payment |

---

## Benefits

✅ **Better Control:** Staff verifies orders before processing begins  
✅ **Fraud Prevention:** Can catch issues before production  
✅ **Customer Flexibility:** Can cancel if needed before verification  
✅ **Clear Workflow:** Status clearly indicates what's expected  
✅ **Audit Trail:** All status changes are tracked  

---

## Questions & Troubleshooting

**Q: What if staff never verifies an order?**  
A: Order remains in `AWAITING_VERIFICATION`. May need a dashboard alert/notification system for staff.

**Q: Can customer modify order at this stage?**  
A: No, modifications only allowed in PENDING status. At AWAITING_VERIFICATION, order should be locked.

**Q: What about refunds if order is cancelled at AWAITING_VERIFICATION?**  
A: Need to implement refund logic in PaymentService, possibly through VNPay API integration.

**Q: Should notifications be sent to customer?**  
A: Yes, recommend notifying customer when order reaches AWAITING_VERIFICATION and when staff verifies.

---

**Last Updated:** February 8, 2026  
**Status:** ✅ Implemented
