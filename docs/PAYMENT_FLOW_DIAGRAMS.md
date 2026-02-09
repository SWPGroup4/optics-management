# Sơ Đồ Luồng Thanh Toán (Payment Flow Diagrams)

## 1. Sơ Đồ Luồng Tổng Quan

```
┌─────────────────────────────────────────────────────────────────────┐
│                     KHÁCH HÀNG TẠO ĐƠN HÀNG                        │
└────────────────────────┬────────────────────────────────────────────┘
                         │
              ┌──────────▼──────────┐
              │ Chọn phương thức TT │
              └──────────┬──────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    ┌───▼────┐      ┌────▼────┐    ┌─────▼──────┐
    │  COD   │      │ VNPAY   │    │ VNPAY PRE  │
    │        │      │         │    │            │
    └───┬────┘      └────┬────┘    └─────┬──────┘
        │                │              │
        │          ┌─────▼────────┐    │
        │          │ Tạo Payment  │    │
        │          │ (UNPAID)     │    │
        │          └──────┬───────┘    │
        │                 │ Chuyển      │
        │                 │ hướng       │
        │          ┌─────▼──────────┐  │
        │          │   VNPay Portal │  │
        │          │ (Khách thanh   │  │
        │          │  toán)         │  │
        │          └─────┬──────────┘  │
        │                │             │
        │          ┌─────▼──────────┐◄─┘
        │          │ VNPay Callback │
        │          │ (Xác minh)      │
        │          └─────┬──────────┘
        │                │
        │          Success?
        │      Yes   │    No
        │          ┌─▼─┐
        │          │   │
        │     ┌────┴───┴────┐
        │     │              │
        │┌────▼────┐    ┌────▼─────┐
        ││ PAID    │    │ FAILED   │
        │└────┬────┘    └────┬─────┘
        │     │              │
        └─────┴──────────────┘
              │
         ┌────▼──────────────┐
         │ Cập nhật Order    │
         │ - Status          │
         │ - PreOrderStatus  │
         └───────┬───────────┘
               │
          ┌────▼─────────┐
          │ Tạo Transaction
          │ (DEPOSIT/CHARGE
          └────┬─────────┘
              │
         ┌────▼────────────────┐
         │ Gửi thông báo email │
         └─────────────────────┘
```

---

## 2. Quy Trình Chi Tiết - Thanh Toán COD

```
START (Khách tạo đơn NORMAL + COD)
│
├─ 1. Tạo Order
│   ├─ totalAmount = tổng giá sản phẩm
│   ├─ paymentMethod = COD
│   └─ status = PENDING (hoặc COMPLETED)
│
├─ 2. Kiểm tra PaymentRequirement
│   ├─ Kiểm tra: có PRESCRIPTION không? → NO
│   ├─ Kiểm tra: có PRE_ORDER không? → NO
│   ├─ Kiểm tra: paymentMethod? → COD
│   ├─ Kết quả:
│   │   - depositPercentage = 0%
│   │   - requireAmount = 0
│   │   - allowCOD = true
│   │   - message = "Thanh toán khi nhận hàng"
│   └─ Return PaymentRequirementResponse
│
├─ 3. Không cần thanh toán online
│   └─ Khách nhận hàng → thanh toán tiền mặt
│
└─ END
```

---

## 3. Quy Trình Chi Tiết - Thanh Toán NORMAL + VNPAY

```
START (Khách tạo đơn NORMAL + VNPAY)
│
├─ 1. Tạo Order
│   ├─ totalAmount = tổng giá sản phẩm
│   ├─ paymentMethod = VNPAY
│   └─ status = PENDING
│
├─ 2. Kiểm tra PaymentRequirement
│   ├─ Kiểm tra: có PRESCRIPTION không? → NO
│   ├─ Kiểm tra: có PRE_ORDER không? → NO
│   ├─ Kiểm tra: paymentMethod? → VNPAY
│   ├─ Kết quả:
│   │   - depositPercentage = 100%
│   │   - requireAmount = totalAmount
│   │   - allowCOD = false
│   │   - message = "Thanh toán trước 100%"
│   └─ Return PaymentRequirementResponse
│
├─ 3. Khách gọi API Checkout
│   │ POST /payment/checkout?orderId=XXX
│   │
│   ├─ Xác định PaymentPurpose = FULL
│   ├─ Tính amount = totalAmount (100%)
│   ├─ Kiểm tra: amount > 0? → YES
│   ├─ Kiểm tra: paymentMethod == VNPAY? → YES
│   ├─ Tạo Payment entity:
│   │   - amount = totalAmount
│   │   - paymentPurpose = FULL
│   │   - paymentMethod = VNPAY
│   │   - status = UNPAID
│   ├─ Lưu Payment vào DB
│   └─ Return: VNPay payment URL
│
├─ 4. Khách được chuyển hướng sang VNPay
│   └─ Thực hiện thanh toán trên portal VNPay
│
├─ 5. VNPay gửi Callback
│   │ GET /payment/vnpay-callback
│   │
│   ├─ Nhận parameters từ VNPay:
│   │   - vnp_TxnRef (Payment ID)
│   │   - vnp_TransactionNo (Transaction ID)
│   │   - vnp_Amount (Số tiền)
│   │   - vnp_ResponseCode (Kết quả)
│   │
│   ├─ Xác minh Hash
│   │   - Recalculate HMAC SHA512
│   │   - So sánh với vnp_SecureHash
│   │
│   ├─ Kiểm tra vnp_ResponseCode
│   │   ├─ Nếu = "00" → SUCCESS
│   │   └─ Nếu khác → FAILED
│   │
│   ├─ Xử lý SUCCESS:
│   │   ├─ Cập nhật Payment:
│   │   │   ├─ status = PAID
│   │   │   ├─ paymentDate = now()
│   │   │
│   │   ├─ Tạo Transaction:
│   │   │   ├─ type = CHARGE (vì FULL, không DEPOSIT)
│   │   │   ├─ amount = totalAmount / 100
│   │   │   └─ gatewayReference = vnp_TransactionNo
│   │   │
│   │   ├─ Cập nhật Order:
│   │   │   ├─ Kiểm tra items:
│   │   │   │   ├─ Có PRESCRIPTION? → status = PROCESSING
│   │   │   │   ├─ Có PRE_ORDER? → status = PROCESSING
│   │   │   │   └─ Chỉ NORMAL? → status = COMPLETED
│   │   │   └─ Lưu
│   │   │
│   │   ├─ Gửi Email thông báo
│   │   └─ Redirect: checkout/success?orderId=XXX&email=YYY
│   │
│   └─ Xử lý FAILED:
│       ├─ Cập nhật Payment:
│       │   ├─ status = FAILED
│       │   └─ paymentDate = now()
│       └─ Redirect: checkout/failure
│
└─ END
```

---

## 4. Quy Trình Chi Tiết - Thanh Toán PRE_ORDER (2 lần)

```
START (Khách tạo đơn PRE_ORDER + VNPAY)
│
├─ 1. Tạo Order
│   ├─ totalAmount = tổng giá sản phẩm
│   ├─ depositAmount = totalAmount * 50%
│   ├─ remainingAmount = totalAmount * 50%
│   ├─ paymentMethod = VNPAY
│   └─ status = PENDING
│
├─ 2. Kiểm tra PaymentRequirement (Lần 1)
│   ├─ Kiểm tra: có PRESCRIPTION không? → NO
│   ├─ Kiểm tra: có PRE_ORDER không? → YES
│   ├─ Kiểm tra: đã cọc 50% chưa?
│   │   │ SELECT * FROM payment
│   │   │ WHERE orderId = XXX
│   │   │ AND paymentPurpose = DEPOSIT
│   │   │ AND status = PAID
│   │   ├─ Nếu chưa → requireAmount = depositeAmount (50%)
│   │   └─ Kết quả:
│   │       - depositPercentage = 50%
│   │       - requireAmount = depositAmount
│   │       - allowCOD = false
│   │       - message = "Bắt buộc cọc 50% (pre-order)"
│   └─ Return PaymentRequirementResponse
│
├─ 3. Khách gọi API Checkout Lần 1
│   │ POST /payment/checkout?orderId=XXX
│   │
│   ├─ Xác định PaymentPurpose = DEPOSIT (chưa cọc)
│   ├─ Tính amount = depositAmount (50%)
│   ├─ Tạo Payment:
│   │   - amount = depositAmount
│   │   - paymentPurpose = DEPOSIT
│   │   - paymentMethod = VNPAY
│   │   - status = UNPAID
│   ├─ Lưu Payment (Payment #1)
│   └─ Return: VNPay URL
│
├─ 4. VNPay Callback - Lần 1 (SUCCESS)
│   │
│   ├─ Cập nhật Payment #1:
│   │   ├─ status = PAID
│   │   └─ paymentDate = now()
│   │
│   ├─ Tạo Transaction #1:
│   │   ├─ type = DEPOSIT
│   │   └─ amount = depositAmount / 100
│   │
│   ├─ Cập nhật Order:
│   │   ├─ preOrderStatus = DEPOSIT_PAID
│   │   └─ status = PROCESSING
│   │
│   └─ Redirect: checkout/success
│
├─ 5. Khách quay lại kiểm tra PaymentRequirement (Lần 2)
│   ├─ Kiểm tra: có PRE_ORDER không? → YES
│   ├─ Kiểm tra: đã cọc 50% chưa?
│   │   │ SELECT * FROM payment
│   │   │ WHERE orderId = XXX
│   │   │ AND paymentPurpose = DEPOSIT
│   │   │ AND status = PAID
│   │   ├─ Nếu có → requireAmount = remainingAmount (50%)
│   │   └─ Kết quả:
│   │       - depositPercentage = 50%
│   │       - requireAmount = remainingAmount
│   │       - allowCOD = false
│   │       - message = "Đã cọc 50%, vui lòng thanh toán 50% còn lại"
│   └─ Return PaymentRequirementResponse
│
├─ 6. Khách gọi API Checkout Lần 2
│   │ POST /payment/checkout?orderId=XXX
│   │
│   ├─ Xác định PaymentPurpose:
│   │   │ - Kiểm tra có DEPOSIT_PAID chưa?
│   │   │ - Có → PaymentPurpose = REMAINING
│   │   │ - Không → PaymentPurpose = DEPOSIT
│   │   └─ Kết quả: REMAINING
│   │
│   ├─ Tính amount = remainingAmount (50%)
│   ├─ Tạo Payment:
│   │   - amount = remainingAmount
│   │   - paymentPurpose = REMAINING
│   │   - paymentMethod = VNPAY
│   │   - status = UNPAID
│   ├─ Lưu Payment (Payment #2)
│   └─ Return: VNPay URL
│
├─ 7. VNPay Callback - Lần 2 (SUCCESS)
│   │
│   ├─ Cập nhật Payment #2:
│   │   ├─ status = PAID
│   │   └─ paymentDate = now()
│   │
│   ├─ Tạo Transaction #2:
│   │   ├─ type = CHARGE
│   │   └─ amount = remainingAmount / 100
│   │
│   ├─ Cập nhật Order:
│   │   ├─ preOrderStatus = REMAINING_PAID
│   │   └─ status = COMPLETED
│   │
│   └─ Redirect: checkout/success
│
└─ END
```

---

## 5. Quy Trình Chi Tiết - Thanh Toán PRESCRIPTION

```
START (Khách tạo đơn PRESCRIPTION + VNPAY)
│
├─ 1. Tạo Order
│   ├─ totalAmount = tổng giá sản phẩm (theo đơn)
│   ├─ paymentMethod = VNPAY (BẮT BUỘC)
│   ├─ Kiểm tra: paymentMethod = COD? → ERROR
│   │   └─ Throw: INVALID_PAYMENT_METHOD
│   └─ status = PENDING
│
├─ 2. Kiểm tra PaymentRequirement
│   ├─ Kiểm tra: có PRESCRIPTION không? → YES
│   ├─ Kết quả:
│   │   - depositPercentage = 100%
│   │   - requireAmount = totalAmount
│   │   - allowCOD = false ❌
│   │   - message = "Sản phẩm kê đơn bắt buộc thanh toán 100%"
│   └─ Return PaymentRequirementResponse
│
├─ 3. Khách gọi API Checkout
│   │ POST /payment/checkout?orderId=XXX
│   │
│   ├─ Xác định PaymentPurpose = FULL (luôn)
│   ├─ Tính amount = totalAmount (100%)
│   ├─ Kiểm tra logic:
│   │   ├─ Có PRESCRIPTION? → YES → return FULL
│   │   └─ (không kiểm tra PRE_ORDER nếu có PRESCRIPTION)
│   │
│   ├─ Tạo Payment:
│   │   - amount = totalAmount
│   │   - paymentPurpose = FULL
│   │   - paymentMethod = VNPAY
│   │   - status = UNPAID
│   ├─ Lưu Payment
│   └─ Return: VNPay URL
│
├─ 4. VNPay Callback (SUCCESS)
│   │
│   ├─ Cập nhật Payment:
│   │   ├─ status = PAID
│   │   └─ paymentDate = now()
│   │
│   ├─ Tạo Transaction:
│   │   ├─ type = CHARGE (không phải DEPOSIT)
│   │   └─ amount = totalAmount / 100
│   │
│   ├─ Cập nhật Order:
│   │   ├─ Kiểm tra items:
│   │   │   ├─ Có PRESCRIPTION? → YES
│   │   │   └─ Nên status = PROCESSING
│   │   │       (cần may đo, xác nhận, v.v.)
│   │   └─ Lưu
│   │
│   ├─ Gửi Email thông báo
│   └─ Redirect: checkout/success
│
├─ 5. Xử lý Đơn Hàng Kê Đơn
│   ├─ Admin/Staff review thông tin kê đơn
│   ├─ Liên hệ nhà cung cấp (nếu cần)
│   ├─ Xác nhận may đo
│   ├─ Tạo sản phẩm theo đơn
│   └─ Cập nhật Order → COMPLETED
│
└─ END
```

---

## 6. Sơ Đồ Quyết Định PaymentPurpose

```
determinePaymentPurpose(order) {
    items = order.getItems()
    
    ┌─ Kiểm tra PRESCRIPTION?
    │  YES ─────> return FULL ✓
    │
    └─ Kiểm tra PRE_ORDER?
       YES ─┐
            ├─ Kiểm tra đã cọc?
            │  ├─ YES ─> return REMAINING
            │  └─ NO  ─> return DEPOSIT
            │
       NO  ─> return FULL (NORMAL + VNPAY)
}
```

---

## 7. Sơ Đồ Cập Nhật Order Status

```
Payment Status → PAID ❓
    │
    ├─ YES ─> Kiểm tra Order items
    │         │
    │         ├─ Contains PRESCRIPTION? ─> YES ─> status = PROCESSING
    │         ├─ Contains PRE_ORDER? ───────> YES ─> status = PROCESSING
    │         └─ All NORMAL? ────────────────> YES ─> status = COMPLETED
    │
    └─ NO ──> (Không cập nhật Order)
```

---

## 8. Sơ Đồ Xác Minh VNPay Callback

```
VNPay Callback Request
│
├─ 1. Lấy vnp_SecureHash từ request
├─ 2. Loại bỏ: vnp_SecureHashType, vnp_SecureHash
├─ 3. Sort tất cả parameters theo alphabet
├─ 4. Tạo hash string: param1=value1&param2=value2&...
├─ 5. HMAC SHA512(vnp_HashSecret, hashString)
├─ 6. So sánh kết quả với vnp_SecureHash
│
├─ Match? ┐
│         ├─ YES ─> Hợp lệ ✓
│         └─ NO  ─> Từ chối ❌
│
└─ Kiểm tra vnp_ResponseCode
   ├─ "00" ─────> SUCCESS
   └─ Khác ──>  FAILED
```

---

## 9. Database Schema - Payment Tables

```
┌─────────────────────────────┐
│         PAYMENT             │
├─────────────────────────────┤
│ id (UUID)                   │ PK
│ order_id (UUID)             │ FK -> Orders
│ amount (DECIMAL)            │ Số tiền
│ percentage (DECIMAL)        │ Tỷ % (lưu tham khảo)
│ payment_method (ENUM)       │ COD / VNPAY
│ payment_purpose (ENUM)      │ DEPOSIT / REMAINING / FULL
│ status (ENUM)               │ UNPAID / PAID / FAILED / REFUNDED
│ payment_date (TIMESTAMP)    │ Thời gian thanh toán
│ description (TEXT)          │ Mô tả
├─────────────────────────────┤
│ created_at (TIMESTAMP)      │ (nếu có)
│ updated_at (TIMESTAMP)      │ (nếu có)
└─────────────────────────────┘
         │
         │ 1-to-many
         │
         ▼
┌─────────────────────────────┐
│      TRANSACTION            │
├─────────────────────────────┤
│ id (UUID)                   │ PK
│ payment_id (UUID)           │ FK -> Payment
│ type (ENUM)                 │ DEPOSIT / CHARGE
│ amount (DECIMAL)            │ Số tiền giao dịch
│ gateway_reference (VARCHAR) │ Mã từ VNPay
│ created_at (TIMESTAMP)      │
├─────────────────────────────┤
│ description (TEXT)          │ (optional)
│ status (VARCHAR)            │ (optional)
└─────────────────────────────┘
```

---

## 10. State Machine - Payment Status

```
       CREATE
         │
         ▼
    ┌─────────┐
    │ UNPAID  │
    └────┬────┘
         │
    ┌────┴────────────────────────┐
    │                             │
    ▼                             ▼
┌─────────┐               ┌──────────┐
│  PAID   │               │ FAILED   │
└────┬────┘               └──────────┘
     │                         │
     │ (可选 refund)           │ (Retry)
     │                         │
     └─────┬────────────────┬──┘
           │                │
           ▼                ▼
      ┌──────────┐    ┌─────────┐
      │ REFUNDED │    │ UNPAID  │
      └──────────┘    └─────────┘
```

---

## 11. Timeline - Pre-Order Payment Flow

```
Timeline:
│
├─ T0: Khách tạo đơn Pre-Order
│      Order Status: PENDING
│      PreOrderStatus: null
│
├─ T1: Khách cọc 50% lần 1
│      Gọi: POST /payment/checkout
│      Payment #1 created (UNPAID)
│ 
├─ T2: VNPay callback thành công
│      Payment #1: UNPAID → PAID
│      PreOrderStatus: null → DEPOSIT_PAID
│      Order Status: PENDING → PROCESSING
│      Transaction #1 (DEPOSIT) created
│
├─ T3...T(N-1): Hệ thống may đo, sản xuất
│                Order Status: PROCESSING
│
├─ TN-1: Khách thanh toán 50% lần 2
│        Gọi: POST /payment/checkout
│        Payment #2 created (UNPAID)
│
├─ TN: VNPay callback thành công
│      Payment #2: UNPAID → PAID
│      PreOrderStatus: DEPOSIT_PAID → REMAINING_PAID
│      Order Status: PROCESSING → COMPLETED
│      Transaction #2 (CHARGE) created
│
├─ TN+1: Giao hàng
│        Khách nhận hàng
│
└─ END
```

---

## 12. Payment Flow - Khác Biệt Giữa Ba Loại

```
┌──────────────────────────────────────────────────────────────────┐
│ NORMAL                                                           │
├──────────────────────────────────────────────────────────────────┤
│ COD:         0% → 100% (khi nhận)  [1 lần, offline]           │
│ VNPAY:      100%  [1 lần, online]                             │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ PRE_ORDER (Must VNPAY)                                          │
├──────────────────────────────────────────────────────────────────┤
│ Lần 1: 50% DEPOSIT  → Order: PROCESSING                        │
│ Lần 2: 50% REMAINING → Order: COMPLETED                        │
│ Timeline: Có thể chêch nhau vài tuần                            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ PRESCRIPTION (Must VNPAY 100%, No COD)                          │
├──────────────────────────────────────────────────────────────────┤
│ (Chặn COD completement)                                          │
│ 100% FULL → Order: PROCESSING                                   │
│ (Cần admin xử lý sau)                                            │
└──────────────────────────────────────────────────────────────────┘
```

---

## 13. API Request/Response Flow

```
CLIENT                          BACKEND                      VNPAY
│                               │                             │
├─ GET /payment/orders/{id}/requirement                      │
│──────────────────────────────>│                            │
│                               │ (kiểm tra items)           │
│                               │ (kiểm tra payments)        │
│<──────────────────────────────│                            │
│ PaymentRequirementResponse    │                            │
│                               │                            │
├─ POST /payment/checkout?orderId={id}                       │
│──────────────────────────────>│                            │
│                               │ (tạo Payment)             │
│                               │ (tính amount)             │
│                               ├─ VNPayService             │
│                               │ (tạo URL)                 │
│                               │─────────────────────────>│
│<──────────────────────────────│ (trả VNPay URL)           │
│ VNPay Payment URL             │                           │
│                               │                           │
├────────────────────────────────────────────────────────>│
│ (Redirect to VNPay)                                      │
│                                                          │
│ (Khách thanh toán)                                       │
│                                                          │
│<──────────────────────────────────────────────────────┤
│ (Callback to Backend)                                  │
│ GET /payment/vnpay-callback?params...                  │
│──────────────────────────────>│                        │
│                               │ (xác minh hash)       │
│                               │ (cập nhật Payment)    │
│                               │ (cập nhật Order)      │
│                               │ (tạo Transaction)     │
│<──────────────────────────────│                        │
│ Redirect to /checkout/success │                        │
│ hoặc /checkout/failure        │                        │
│                               │                        │
│                               │                        │
├─ GET /payment/orders/{id}/history                      │
│──────────────────────────────>│                        │
│                               │ (query payments)      │
│<──────────────────────────────│                        │
│ [Payment list]                │                        │
```

---

**Sơ đồ này giúp hiểu rõ hơn về toàn bộ luồng thanh toán.**
