# Hướng Dẫn Nghiệp Vụ Thanh Toán - Optics Management System

## 1. Tổng Quan Hệ Thống Thanh Toán

Hệ thống thanh toán của Optics Management được thiết kế để hỗ trợ nhiều phương thức thanh toán khác nhau với các quy tắc thanh toán linh hoạt tùy theo loại đơn hàng và sản phẩm.

---

## 2. Các Khái Niệm Chính

### 2.1 Phương Thức Thanh Toán (Payment Method)

| Phương Thức | Mã Code | Mô Tả |
|-------------|---------|-------|
| **COD** | COD | Thanh toán khi nhận hàng (Cash On Delivery) |
| **VNPay** | VNPAY | Thanh toán trực tuyến qua cổng VNPay |

### 2.2 Mục Đích Thanh Toán (Payment Purpose)

| Mục Đích | Mã Code | Mô Tả | Tỷ Lệ |
|----------|---------|-------|-------|
| **Đặt Cọc** | DEPOSIT | Thanh toán trước (dùng cho Pre-Order) | 50% |
| **Thanh Toán Toàn Bộ** | FULL | Thanh toán hoàn toàn cả đơn hàng | 100% |
| **Thanh Toán Phần Còn Lại** | REMAINING | Thanh toán phần còn lại sau DEPOSIT | 50% |

### 2.3 Trạng Thái Thanh Toán (Payment Status)

| Trạng Thái | Mã Code | Mô Tả |
|-----------|---------|-------|
| **Chưa Thanh Toán** | UNPAID | Đơn hàng chưa được thanh toán |
| **Đã Thanh Toán** | PAID | Thanh toán thành công |
| **Thất Bại** | FAILED | Giao dịch thanh toán thất bại |
| **Hoàn Tiền** | REFUNDED | Tiền đã hoàn lại cho khách |

---

## 3. Loại Đơn Hàng và Quy Tắc Thanh Toán

Hệ thống hỗ trợ 3 loại OrderItem:

| Loại Đơn Hàng | Mô Tả | Yêu Cầu Thanh Toán |
|---------------|-------|-------------------|
| **NORMAL** | Sản phẩm bình thường | Tùy phương thức thanh toán (COD hoặc VNPAY 100%) |
| **PRE_ORDER** | Đặt hàng trước | **Bắt buộc thanh toán 2 lần:** 50% tiền cọc + 50% phần còn lại |
| **PRESCRIPTION** | Sản phẩm kê đơn | **Bắt buộc VNPAY 100%** (không cho phép COD) |

---

## 4. Quy Tắc Xác Định Yêu Cầu Thanh Toán

### 4.1 Logic Xác Định Yêu Cầu Thanh Toán

Khi khách hàng thực hiện thanh toán, hệ thống sẽ kiểm tra các điều kiện trong đơn hàng để xác định yêu cầu thanh toán:

```
IF đơn hàng có PRESCRIPTION:
    ├─ Yêu cầu: 100% (FULL)
    ├─ Phương thức: Chỉ VNPAY
    └─ Không cho phép: COD

ELSE IF đơn hàng có PRE_ORDER:
    ├─ Nếu chưa cọc:
    │  ├─ Yêu cầu: 50% (DEPOSIT)
    │  └─ Phương thức: VNPAY
    └─ Nếu đã cọc rồi:
       ├─ Yêu cầu: 50% (REMAINING)
       └─ Phương thức: VNPAY

ELSE (NORMAL):
    ├─ Nếu phương thức là VNPAY:
    │  ├─ Yêu cầu: 100% (FULL)
    │  └─ Phương thức: VNPAY
    └─ Nếu phương thức là COD:
       ├─ Yêu cầu: 0% (thanh toán sau)
       └─ Phương thức: COD
```

### 4.2 Thông Tin Chi Tiết Yêu Cầu Thanh Toán

```json
{
  "depositPercentage": 0.5,  // Tỷ lệ phần trăm cần thanh toán
  "requiredAmount": 500000,  // Số tiền cần thanh toán (VND)
  "allowCOD": false,         // Có cho phép COD không
  "message": "Bắt buộc cọc 50% (pre-order)"  // Thông báo cho khách
}
```

---

## 5. Luồng Đơn Hàng và Trạng Thái

### 5.1 Trạng Thái Đơn Hàng (Order Status)

| Trạng Thái | Mô Tả |
|-----------|-------|
| **PENDING** | Đơn hàng mới tạo, chờ thanh toán |
| **PROCESSING** | Đã thanh toán, đang xử lý (có PRE_ORDER hoặc PRESCRIPTION) |
| **COMPLETED** | Đơn hàng hoàn thành (chỉ NORMAL items hoặc PRE_ORDER đã thanh toán hết) |

### 5.2 Trạng Thái Pre-Order (Pre Order Status)

| Trạng Thái | Mô Tả |
|-----------|-------|
| **DEPOSIT_PAID** | Đã thanh toán 50% tiền cọc |
| **REMAINING_PAID** | Đã thanh toán 50% phần còn lại (hoàn tất pre-order) |

---

## 6. Quy Trình Thanh Toán Chi Tiết

### 6.1 Quy Trình Thanh Toán COD (Cash On Delivery)

```
1. Khách hàng tạo đơn hàng
   └─ Loại: NORMAL item
   └─ Phương thức: COD
   
2. Hệ thống kiểm tra cho phép COD
   └─ Lưu thông tin đơn hàng
   └─ Trạng thái: PENDING / COMPLETED
   
3. Khách nhận hàng và thanh toán tiền mặt
   └─ Không cần API thanh toán
```

**Khi nào sử dụng:**
- Đơn hàng chỉ có sản phẩm bình thường (NORMAL)
- Khách chọn thanh toán COD

### 6.2 Quy Trình Thanh Toán VNPay

```
1. Khách hàng tạo đơn hàng
   └─ Loại: PRESCRIPTION, PRE_ORDER, hoặc NORMAL
   └─ Phương thức: VNPAY
   
2. Khách yêu cầu thanh toán
   ├─ Gọi API: GET /payment/orders/{orderId}/requirement
   └─ Nhận thông tin yêu cầu thanh toán
   
3. Hệ thống tạo đơn thanh toán
   ├─ Xác định Payment Purpose (DEPOSIT / REMAINING / FULL)
   ├─ Tính toán số tiền cần thanh toán
   └─ Tạo bản ghi Payment với trạng thái UNPAID
   
4. Khách chuyển hướng sang VNPay
   ├─ Gọi API: POST /payment/checkout?orderId={orderId}
   └─ Nhận URL thanh toán VNPay
   
5. Khách thanh toán trên cổng VNPay
   
6. VNPay gửi callback đến hệ thống
   ├─ Endpoint: GET /payment/vnpay-callback
   ├─ Hệ thống xác minh chữ ký điện tử
   ├─ Cập nhật trạng thái Payment thành PAID
   ├─ Cập nhật trạng thái Order (PROCESSING hoặc COMPLETED)
   └─ Lưu thông tin giao dịch vào bảng Transaction
   
7. Hệ thống chuyển hướng khách
   ├─ Nếu thành công: /checkout/success?orderId={id}&email={email}
   └─ Nếu thất bại: /checkout/failure
```

**Khi nào sử dụng:**
- Đơn hàng có PRESCRIPTION item (bắt buộc)
- Đơn hàng có PRE_ORDER item (bắt buộc)
- Khách hàng chọn VNPAY cho đơn NORMAL

---

## 7. Chi Tiết Quy Trình Thanh Toán Pre-Order

### 7.1 Thanh Toán Lần 1 - Tiền Cọc (DEPOSIT)

```
Bước 1: Khách tạo đơn hàng PRE_ORDER
        └─ depositAmount = totalAmount * 0.5
        └─ remainingAmount = totalAmount * 0.5
        
Bước 2: Khách thanh toán tiền cọc
        ├─ Payment Purpose: DEPOSIT
        ├─ Amount: depositAmount (50%)
        └─ Payment Method: VNPAY
        
Bước 3: VNPay callback thành công
        ├─ Cập nhật Payment status: PAID
        ├─ Cập nhật Order PreOrderStatus: DEPOSIT_PAID
        └─ Cập nhật Order status: PROCESSING
        
Bước 4: Tạo Transaction record
        └─ Type: DEPOSIT
        └─ Amount: 50% giá trị đơn hàng
```

### 7.2 Thanh Toán Lần 2 - Phần Còn Lại (REMAINING)

```
Bước 1: Khách yêu cầu thanh toán phần còn lại
        └─ Hệ thống kiểm tra: có DEPOSIT_PAID chưa?
        
Bước 2: Tính toán Payment Purpose
        ├─ Nếu có payment DEPOSIT với status PAID
        └─ Thì Payment Purpose = REMAINING
        
Bước 3: Khách thanh toán phần còn lại
        ├─ Payment Purpose: REMAINING
        ├─ Amount: remainingAmount (50%)
        └─ Payment Method: VNPAY
        
Bước 4: VNPay callback thành công
        ├─ Cập nhật Payment status: PAID
        ├─ Cập nhật Order PreOrderStatus: REMAINING_PAID
        └─ Cập nhật Order status: COMPLETED
        
Bước 5: Tạo Transaction record
        └─ Type: CHARGE
        └─ Amount: 50% giá trị đơn hàng
```

---

## 8. Chi Tiết Quy Trình Thanh Toán Prescription

```
Bước 1: Khách tạo đơn hàng có PRESCRIPTION
        ├─ Phương thức thanh toán: Chỉ VNPAY (bắt buộc)
        └─ Amount: totalAmount (100%)
        
Bước 2: Khách thanh toán
        ├─ Payment Purpose: FULL
        ├─ Amount: totalAmount (100%)
        └─ Payment Method: VNPAY
        
Bước 3: VNPay callback thành công
        ├─ Cập nhật Payment status: PAID
        └─ Cập nhật Order status: PROCESSING
        
Bước 4: Tạo Transaction record
        └─ Type: CHARGE
        └─ Amount: 100% giá trị đơn hàng
```

---

## 9. Các Quy Tắc Kinh Doanh (Business Rules)

### 9.1 Quy Tắc 1: PRESCRIPTION Bắt Buộc Thanh Toán Trước 100%

```
IF order.items.contains(OrderItemType.PRESCRIPTION)
  THEN {
    paymentPurpose = FULL (100%)
    paymentMethod = VNPAY (ONLY)
    allowCOD = FALSE
  }
```

**Ý nghĩa:** Sản phẩm kê đơn cần được xác nhận thanh toán trước khi xử lý.

### 9.2 Quy Tắc 2: PRE_ORDER Thanh Toán 2 Lần

```
IF order.items.contains(OrderItemType.PRE_ORDER) AND orderHasNoPrescription
  THEN {
    - Lần 1: DEPOSIT 50%
    - Lần 2: REMAINING 50%
    - paymentMethod = VNPAY (ONLY)
    - allowCOD = FALSE
  }
```

**Ý nghĩa:** Giảm rủi ro cho cả khách hàng và cửa hàng bằng cách chia thanh toán thành 2 lần.

### 9.3 Quy Tắc 3: NORMAL + VNPAY = Thanh Toán 100%

```
IF order.items.allNormal() AND paymentMethod = VNPAY
  THEN {
    paymentPurpose = FULL (100%)
    amount = totalAmount
  }
```

**Ý nghĩa:** Nếu chọn thanh toán VNPAY, phải thanh toán hết, không cọc riêng.

### 9.4 Quy Tắc 4: NORMAL + COD = Không Cần Thanh Toán Trước

```
IF order.items.allNormal() AND paymentMethod = COD
  THEN {
    paymentPurpose = FULL (0%)
    allowCOD = TRUE
    deposit = 0
  }
```

**Ý nghĩa:** Cho phép khách thanh toán khi nhận hàng.

### 9.5 Quy Tắc 5: Cập Nhật Trạng Thái Đơn Hàng

```
AFTER Payment Status = PAID:
  IF order.items.contains(PRESCRIPTION or PRE_ORDER)
    THEN order.status = PROCESSING
  ELSE
    THEN order.status = COMPLETED
```

**Ý nghĩa:** Đơn hàng có PRE_ORDER hoặc PRESCRIPTION cần xử lý thêm, chưa thể hoàn tất.

---

## 10. Cấu Trúc Dữ Liệu

### 10.1 Entity Payment

```java
@Entity
@Table(name = "payment")
public class Payment {
    String id;                          // UUID
    BigDecimal percentage;              // Tỷ lệ phần trăm
    String description;                 // Mô tả thanh toán
    PaymentMethod paymentMethod;        // COD hoặc VNPAY
    PaymentPurpose paymentPurpose;      // DEPOSIT / REMAINING / FULL
    BigDecimal amount;                  // Số tiền cần thanh toán
    PaymentStatus status;               // UNPAID / PAID / FAILED / REFUNDED
    LocalDateTime paymentDate;          // Thời gian thanh toán
    Orders order;                       // Đơn hàng liên quan
}
```

### 10.2 Entity Transaction

```java
@Entity
@Table(name = "transaction")
public class Transaction {
    String id;
    TransactionType type;               // DEPOSIT hoặc CHARGE
    BigDecimal amount;
    String gatewayReference;            // Mã giao dịch từ VNPay
    LocalDateTime createdAt;
    Payment payment;
}
```

### 10.3 Entity Orders (Phần Thanh Toán)

```java
@Entity
@Table(name = "orders")
public class Orders {
    String id;
    BigDecimal totalAmount;             // Tổng tiền đơn hàng
    BigDecimal depositAmount;           // Tiền cọc (50% cho PRE_ORDER)
    BigDecimal remainingAmount;         // Tiền còn lại (50% cho PRE_ORDER)
    PaymentMethod paymentMethod;        // Phương thức thanh toán
    OrderStatus status;                 // PENDING / PROCESSING / COMPLETED
    PreOrderStatus preOrderStatus;      // DEPOSIT_PAID / REMAINING_PAID
}
```

---

## 11. API Endpoints

### 11.1 Lấy Thông Tin Yêu Cầu Thanh Toán

```
GET /payment/orders/{orderId}/requirement

Response:
{
  "depositPercentage": 0.5,
  "requiredAmount": 500000,
  "allowCOD": false,
  "message": "Bắt buộc cọc 50% (pre-order)"
}
```

### 11.2 Khởi Tạo Thanh Toán VNPay

```
POST /payment/checkout?orderId={orderId}

Response:
{
  "result": "https://sandbox.vnpayment.vn/paygate/..."
}
```

### 11.3 Callback từ VNPay

```
GET /payment/vnpay-callback?vnp_TxnRef={paymentId}&vnp_TransactionNo={transactionNo}&...

Redirect:
- Success: {frontendUrl}/checkout/success?orderId={id}&email={email}
- Failure: {frontendUrl}/checkout/failure
```

### 11.4 Lấy Lịch Sử Thanh Toán

```
GET /payment/orders/{orderId}/history

Response:
[
  {
    "id": "payment-uuid",
    "paymentMethod": "VNPAY",
    "paymentPurpose": "DEPOSIT",
    "amount": 500000,
    "percentage": 0.5,
    "status": "PAID",
    "paymentDate": "2024-01-15T10:30:00",
    "description": "Payment for deposit",
    "transactionReference": "vnpay-txn-123"
  }
]
```

---

## 12. Quy Trình Xử Lý Lỗi

### 12.1 Lỗi Phương Thức Thanh Toán Không Hợp Lệ

```
Điều kiện: Đơn hàng có PRESCRIPTION nhưng chọn phương thức COD
Mã lỗi: INVALID_PAYMENT_METHOD
Thông báo: "Sản phẩm kê đơn bắt buộc thanh toán online qua VNPay"
```

### 12.2 Lỗi Đơn Hàng Đã Xử Lý

```
Điều kiện: Cố gắng thanh toán cho đơn hàng đã hoàn tất (COMPLETED)
Mã lỗi: ORDER_ALREADY_PROCESSED
Thông báo: "Đơn hàng này đã được xử lý"
```

### 12.3 Lỗi Đơn Hàng Không Tồn Tại

```
Điều kiện: OrderId không tồn tại
Mã lỗi: ORDER_NOT_FOUND
Thông báo: "Không tìm thấy đơn hàng"
```

---

## 13. Ví Dụ Thực Tế

### 13.1 Ví Dụ 1: Đơn Hàng Bình Thường - COD

```
Bước 1: Khách tạo đơn hàng
  ├─ Items: [NORMAL: Kính mắt 500k]
  ├─ Total: 500.000 VND
  └─ Payment Method: COD

Bước 2: Kiểm tra yêu cầu thanh toán
  GET /payment/orders/order-123/requirement
  Response: {
    "depositPercentage": 0,
    "requiredAmount": 0,
    "allowCOD": true,
    "message": "Đơn hàng có thể thanh toán khi nhận hàng (COD)"
  }

Bước 3: Không cần thanh toán online
  └─ Khách nhận hàng → thanh toán tiền mặt

Bước 4: Cập nhật trạng thái
  └─ Order Status: COMPLETED
```

### 13.2 Ví Dụ 2: Pre-Order - Thanh Toán 2 Lần

```
Bước 1: Khách tạo đơn hàng
  ├─ Items: [PRE_ORDER: Kính cao cấp 1.000.000 VND]
  ├─ Total: 1.000.000 VND
  ├─ Deposit: 500.000 VND (50%)
  ├─ Remaining: 500.000 VND (50%)
  └─ Payment Method: VNPAY

Bước 2: Kiểm tra yêu cầu thanh toán lần 1
  GET /payment/orders/order-456/requirement
  Response: {
    "depositPercentage": 0.5,
    "requiredAmount": 500000,
    "allowCOD": false,
    "message": "Bắt buộc cọc 50% (pre-order)"
  }

Bước 3: Thanh toán tiền cọc lần 1
  POST /payment/checkout?orderId=order-456
  └─ Hệ thống tạo Payment: amount=500000, purpose=DEPOSIT
  └─ Chuyển hướng sang VNPay

Bước 4: VNPay callback (thành công)
  ├─ Payment Status: PAID
  ├─ Order PreOrderStatus: DEPOSIT_PAID
  ├─ Order Status: PROCESSING
  └─ Tạo Transaction: type=DEPOSIT, amount=500000

Bước 5: Khách quay lại kiểm tra yêu cầu lần 2
  GET /payment/orders/order-456/requirement
  Response: {
    "depositPercentage": 0.5,
    "requiredAmount": 500000,
    "allowCOD": false,
    "message": "Đã cọc 50%, vui lòng thanh toán 50% còn lại"
  }

Bước 6: Thanh toán phần còn lại lần 2
  POST /payment/checkout?orderId=order-456
  └─ Hệ thống tạo Payment: amount=500000, purpose=REMAINING
  └─ Chuyển hướng sang VNPay

Bước 7: VNPay callback (thành công)
  ├─ Payment Status: PAID
  ├─ Order PreOrderStatus: REMAINING_PAID
  ├─ Order Status: COMPLETED
  └─ Tạo Transaction: type=CHARGE, amount=500000

Bước 8: Lịch sử thanh toán
  GET /payment/orders/order-456/history
  Response: [
    {
      "paymentPurpose": "DEPOSIT",
      "amount": 500000,
      "status": "PAID",
      "paymentDate": "2024-01-15T10:00:00"
    },
    {
      "paymentPurpose": "REMAINING",
      "amount": 500000,
      "status": "PAID",
      "paymentDate": "2024-01-16T14:30:00"
    }
  ]
```

### 13.3 Ví Dụ 3: Đơn Hàng Kê Đơn - Bắt Buộc 100%

```
Bước 1: Khách tạo đơn hàng
  ├─ Items: [PRESCRIPTION: Kính theo đơn vào 2.000.000 VND]
  ├─ Total: 2.000.000 VND
  └─ Phương thức: VNPAY (bắt buộc, không cho phép COD)

Bước 2: Kiểm tra yêu cầu thanh toán
  GET /payment/orders/order-789/requirement
  Response: {
    "depositPercentage": 1.0,
    "requiredAmount": 2000000,
    "allowCOD": false,
    "message": "Đơn hàng có sản phẩm kê đơn, bắt buộc thanh toán trước 100%"
  }

Bước 3: Thanh toán 100%
  POST /payment/checkout?orderId=order-789
  └─ Hệ thống tạo Payment: amount=2000000, purpose=FULL
  └─ Chuyển hướng sang VNPay

Bước 4: VNPay callback (thành công)
  ├─ Payment Status: PAID
  ├─ Order Status: PROCESSING (vì có PRESCRIPTION)
  └─ Tạo Transaction: type=CHARGE, amount=2000000

Bước 5: Hệ thống xử lý đơn hàng kê đơn
  └─ Cần xác nhận với nhà cccungycap, may đo, v.v.
```

---

## 14. Tóm Tắt Bảng Quy Tắc Thanh Toán

| Loại Đơn | Phương Thức | Yêu Cầu Thanh Toán | Lần Thanh Toán | Cho Phép COD |
|----------|------------|-------------------|---------------|------------|
| NORMAL | VNPAY | 100% (FULL) | 1 | ❌ |
| NORMAL | COD | 0% | 1 (TT sau) | ✅ |
| PRE_ORDER | VNPAY | 50% + 50% (DEPOSIT + REMAINING) | 2 | ❌ |
| PRESCRIPTION | VNPAY | 100% (FULL) | 1 | ❌ |
| PRESCRIPTION | COD | ❌ (Không cho phép) | - | ❌ |

---

## 15. Luồng Quyết Định Thanh Toán - Sơ Đồ

```
┌─────────────────────────────┐
│  Khách tạo đơn hàng         │
└──────────────┬──────────────┘
               │
        ┌──────▼──────┐
        │ Kiểm tra loại items │
        └──────┬────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
┌─────────┐ ┌─────────┐ ┌──────────┐
│PRE_ORDER│ │PRESCRIPTION
│NORMAL   │ │NORMAL    │ │          │
└────┬────┘ └────┬────┘ └──┬───────┘
     │           │         │
     ▼           ▼         ▼
 ┌────────┐ ┌────────┐ ┌──────────┐
 │Phương  │ │VNPAY   │ │Phương    │
 │thức?   │ │100%    │ │thức?     │
 └───┬────┘ │(FULL)  │ └────┬─────┘
     │      └────────┘      │
  ┌──┴───┐                ┌──┴────┐
  │      │                │       │
VNPAY   COD             VNPAY   COD
  │      │                │      │
  ▼      ▼                ▼      ▼
50%+50% KHÔNG        100%  0%
DEPOSIT VNPAY        (FULL) (COD)
+REM                       
```

---

## 16. Kiểm Tra & Xác Minh

### 16.1 Điểm Kiểm Tra Trước Thanh Toán

- [ ] Đơn hàng tồn tại
- [ ] Đơn hàng chưa hoàn tất (status ≠ COMPLETED)
- [ ] Phương thức thanh toán hợp lệ (không là COD cho PRESCRIPTION)
- [ ] Số tiền > 0 (trừ COD)
- [ ] Xác định đúng Payment Purpose

### 16.2 Điểm Kiểm Tra Sau Callback VNPay

- [ ] Xác minh chữ ký điện tử (Hash validation)
- [ ] Kiểm tra mã giao dịch từ VNPay
- [ ] Cập nhật trạng thái Payment
- [ ] Tạo Transaction record
- [ ] Cập nhật Order status phù hợp
- [ ] Gửi thông báo email/SMS cho khách

---

## 17. Yêu Cầu Không Chức Năng (Performance & Security)

### 17.1 Yêu Cầu Hiệu Suất

- Thời gian xử lý VNPay callback ≤ 2 giây
- Lịch sử thanh toán tải trong ≤ 1 giây
- Hỗ trợ ≥ 1000 giao dịch/giờ

### 17.2 Yêu Cầu Bảo Mật

- ✅ Xác minh chữ ký điện tử VNPay (HMAC SHA512)
- ✅ Mã hóa thông tin thanh toán (SSL/TLS)
- ✅ Ghi log tất cả giao dịch thanh toán
- ✅ Không lưu trữ số thẻ tín dụng
- ✅ Kiểm tra quyền truy cập (chỉ khách chủ sở hữu đơn mới xem được)

---

## 18. Tài Liệu Tham Khảo

### 18.1 Các File Liên Quan

- [PaymentController.java](../src/main/java/com/glassystem/optics/controller/payment/PaymentController.java)
- [PaymentService.java](../src/main/java/com/glassystem/optics/service/PaymentService.java)
- [VNPayService.java](../src/main/java/com/glassystem/optics/service/VNPayService.java)
- [Payment.java](../src/main/java/com/glassystem/optics/entity/Payment.java)
- [Orders.java](../src/main/java/com/glassystem/optics/entity/Orders.java)
- [OrderService.java](../src/main/java/com/glassystem/optics/service/OrderService.java)

### 18.2 Tài Liệu VNPay

- VNPay Payment Gateway Documentation: https://sandbox.vnpayment.vn/
- Security: HMAC SHA512 hash validation
- Test Environment: sandbox.vnpayment.vn

---

## 19. Câu Hỏi Thường Gặp (FAQ)

**Q: Tại sao PRESCRIPTION bắt buộc thanh toán trước 100%?**
A: Để đảm bảo khách hàng sẽ lấy hàng sau khi may đo, vì sản phẩm kê đơn không thể bán được cho khách khác.

**Q: Có thể hoàn tiền (refund) được không?**
A: Có, qua trạng thái REFUNDED. Cần liên hệ quản trị viên để xử lý hoàn tiền qua VNPay.

**Q: Nếu khách chỉ cọc 50% pre-order nhưng không thanh toán phần còn lại?**
A: Đơn hàng sẽ giữ trạng thái PROCESSING cho đến khi thanh toán phần còn lại hoặc hết hạn quy định.

**Q: Có giới hạn thời gian thanh toán không?**
A: Hiện tại chưa có giới hạn trong code, nên cần cấu hình theo chính sách kinh doanh.

**Q: Làm sao kiểm tra lịch sử thanh toán?**
A: Gọi API `GET /payment/orders/{orderId}/history` để lấy danh sách tất cả các lần thanh toán.

---

**Phiên bản:** 1.0  
**Ngày cập nhật:** 08/02/2026  
**Trạng thái:** Chính thức
