# Payment API Specification & Test Cases

## 1. Payment API Endpoints

### 1.1 GET /payment/orders/{orderId}/requirement

**Purpose:** Lấy thông tin yêu cầu thanh toán cho một đơn hàng

**Method:** `GET`

**Path Parameter:**
```
orderId (String, required): ID của đơn hàng
```

**Request Example:**
```bash
curl -X GET "http://localhost:8080/optics/payment/orders/550e8400-e29b-41d4-a716-446655440000/requirement" \
  -H "Authorization: Bearer <token>" \
  -H "Accept: application/json"
```

**Response (200 OK):**
```json
{
  "message": "Success",
  "code": 1000,
  "result": {
    "depositPercentage": 0.5,
    "requiredAmount": 500000,
    "allowCOD": false,
    "message": "Bắt buộc cọc 50% (pre-order)"
  }
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| depositPercentage | Double | Tỷ lệ phần trăm cần thanh toán (0.0 - 1.0) |
| requiredAmount | BigDecimal | Số tiền cần thanh toán (VND) |
| allowCOD | Boolean | Có cho phép COD không |
| message | String | Thông báo chi tiết cho khách hàng |

**Error Responses:**

**404 - ORDER_NOT_FOUND:**
```json
{
  "message": "Order not found",
  "code": 404,
  "result": null
}
```

**400 - Unauthorized:**
```json
{
  "message": "User must be the order's customer",
  "code": 403,
  "result": null
}
```

---

### 1.2 POST /payment/checkout

**Purpose:** Khởi tạo thanh toán VNPay cho một đơn hàng

**Method:** `POST`

**Query Parameter:**
```
orderId (String, required): ID của đơn hàng
```

**Request Example:**
```bash
curl -X POST "http://localhost:8080/optics/payment/checkout?orderId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json"
```

**Response (200 OK):**
```json
{
  "message": "Success",
  "code": 1000,
  "result": "https://sandbox.vnpayment.vn/paygate/pay?vnp_Amount=50000000&vnp_Command=pay&vnp_CreateDate=20240115102030&..."
}
```

**Response (Redirect):**
- Khách hàng được chuyển đến URL VNPay để thực hiện thanh toán

**Error Responses:**

**400 - INVALID_PAYMENT_METHOD:**
```json
{
  "message": "Invalid payment method for this order",
  "code": 400,
  "result": null
}
```
*Nguyên nhân:* Đơn hàng có PRESCRIPTION nhưng cố gắng thanh toán = 0 hoặc COD

**400 - ORDER_ALREADY_PROCESSED:**
```json
{
  "message": "Order has already been processed",
  "code": 400,
  "result": null
}
```
*Nguyên nhân:* Order status = COMPLETED

**404 - ORDER_NOT_FOUND:**
```json
{
  "message": "Order not found",
  "code": 404,
  "result": null
}
```

---

### 1.3 GET /payment/vnpay-callback

**Purpose:** Callback endpoint từ VNPay sau khi khách kết thúc thanh toán

**Method:** `GET`

**Query Parameters (từ VNPay):**
```
vnp_Amount           (Long)     : Số tiền (x100)
vnp_BankCode         (String)   : Mã ngân hàng
vnp_BankTranNo       (String)   : Mã giao dịch ngân hàng
vnp_CardType         (String)   : Loại thẻ
vnp_OrderInfo        (String)   : Thông tin đơn hàng
vnp_PayDate          (String)   : Thời gian thanh toán (yyyyMMddHHmmss)
vnp_ResponseCode     (String)   : 00 = success, khác = failed
vnp_SecureHash       (String)   : Chữ ký điện tử
vnp_TmnCode          (String)   : Mã terminal
vnp_TransactionNo    (String)   : Mã giao dịch tại VNPay
vnp_TxnRef           (String)   : Payment ID
```

**Request Example:**
```
GET /payment/vnpay-callback?vnp_Amount=50000000&vnp_BankCode=NCB&vnp_BankTranNo=20240115&vnp_OrderInfo=Thanh%20toan%20don%20hang%20order-123&vnp_PayDate=20240115102030&vnp_ResponseCode=00&vnp_SecureHash=...&vnp_TmnCode=TMNCODE123&vnp_TransactionNo=123456&vnp_TxnRef=payment-uuid
```

**Response (Redirect):**
- **Success (vnp_ResponseCode = "00"):**
  ```
  Redirect to: {frontendUrl}/checkout/success?orderId={orderId}&email={email}
  ```
  
- **Failure:**
  ```
  Redirect to: {frontendUrl}/checkout/failure
  ```

**Backend Processing:**
1. Xác minh chữ ký điện tử (HMAC SHA512)
2. Kiểm tra mã phản hồi từ VNPay
3. Cập nhật Payment record → PAID
4. Tạo Transaction record
5. Cập nhật Order status
6. Gửi email thông báo

---

### 1.4 GET /payment/orders/{orderId}/history

**Purpose:** Lấy lịch sử tất cả các lần thanh toán của một đơn hàng

**Method:** `GET`

**Path Parameter:**
```
orderId (String, required): ID của đơn hàng
```

**Request Example:**
```bash
curl -X GET "http://localhost:8080/optics/payment/orders/550e8400-e29b-41d4-a716-446655440000/history" \
  -H "Authorization: Bearer <token>" \
  -H "Accept: application/json"
```

**Response (200 OK):**
```json
{
  "message": "Success",
  "code": 1000,
  "result": [
    {
      "id": "payment-uuid-1",
      "paymentMethod": "VNPAY",
      "paymentPurpose": "DEPOSIT",
      "amount": 500000,
      "percentage": 0.5,
      "status": "PAID",
      "paymentDate": "2024-01-15T10:30:00",
      "description": "Payment for deposit",
      "transactionReference": "vnpay-txn-123"
    },
    {
      "id": "payment-uuid-2",
      "paymentMethod": "VNPAY",
      "paymentPurpose": "REMAINING",
      "amount": 500000,
      "percentage": 0.5,
      "status": "PAID",
      "paymentDate": "2024-01-16T14:45:00",
      "description": "Payment for remaining",
      "transactionReference": "vnpay-txn-124"
    }
  ]
}
```

**Response Fields (Per Payment):**
| Field | Type | Description |
|-------|------|-------------|
| id | String | UUID của payment record |
| paymentMethod | String | COD / VNPAY |
| paymentPurpose | String | DEPOSIT / REMAINING / FULL |
| amount | BigDecimal | Số tiền thanh toán (VND) |
| percentage | BigDecimal | Tỷ lệ phần trăm |
| status | String | UNPAID / PAID / FAILED / REFUNDED |
| paymentDate | LocalDateTime | Thời gian thanh toán (ISO 8601) |
| description | String | Mô tả chi tiết |
| transactionReference | String | Mã giao dịch từ gateway |

**Sorting:** Kết quả được sắp xếp theo `paymentDate` giảm dần (mới nhất trước)

**Error Responses:**

**404 - ORDER_NOT_FOUND:**
```json
{
  "message": "Order not found",
  "code": 404,
  "result": null
}
```

---

## 2. Test Cases

### 2.1 Test Case: COD Order - Normal Item

**Test ID:** TC_PAYMENT_001

**Title:** Lấy PaymentRequirement cho đơn hàng bình thường - COD

**Precondition:**
- Đơn hàng tồn tại với ID: `order-001`
- Order items: [NORMAL: 1,000,000 VND]
- Payment Method: COD
- Authenticated user là customer của order

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-001/requirement`

**Expected Result:**
```json
{
  "depositPercentage": 0.0,
  "requiredAmount": 0,
  "allowCOD": true,
  "message": "Đơn hàng có thể thanh toán khi nhận hàng (COD)"
}
```

**Pass Criteria:** Response trả về `allowCOD = true` và `requiredAmount = 0`

---

### 2.2 Test Case: NORMAL + VNPAY Order

**Test ID:** TC_PAYMENT_002

**Title:** Lấy PaymentRequirement cho đơn hàng NORMAL - VNPAY

**Precondition:**
- Đơn hàng tồn tại với ID: `order-002`
- Order items: [NORMAL: 1,000,000 VND]
- Payment Method: VNPAY
- Authenticated user

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-002/requirement`

**Expected Result:**
```json
{
  "depositPercentage": 1.0,
  "requiredAmount": 1000000,
  "allowCOD": false,
  "message": "Thanh toán trước 100% với phương thức VNPAY"
}
```

**Pass Criteria:** 
- `depositPercentage = 1.0`
- `requiredAmount = 1000000`
- `allowCOD = false`

---

### 2.3 Test Case: Pre-Order - Lần 1 (Tiền Cọc)

**Test ID:** TC_PAYMENT_003

**Title:** Thanh toán Deposit cho đơn Pre-Order

**Precondition:**
- Đơn hàng tồn tại với ID: `order-003`
- Order items: [PRE_ORDER: 2,000,000 VND]
- depositAmount: 1,000,000 VND
- remainingAmount: 1,000,000 VND
- Authenticated user
- PaymentRequirement cho biết: `depositPercentage = 0.5`, `requiredAmount = 1000000`

**Test Steps:**
1. Gọi API: `POST /payment/checkout?orderId=order-003`
2. Verify response trả về URL VNPay
3. Simulate VNPay callback (success): `GET /payment/vnpay-callback?vnp_ResponseCode=00&vnp_TxnRef=payment-uuid&...`

**Expected Result:**
- Step 1: Trả về VNPay payment URL
- Step 3: 
  - Payment record: `status = PAID`, `paymentPurpose = DEPOSIT`
  - Order record: `preOrderStatus = DEPOSIT_PAID`, `status = PROCESSING`
  - Transaction created: `type = DEPOSIT`, `amount = 1000000`
  - Redirect to: `/checkout/success?orderId=order-003&email=customer@example.com`

**Pass Criteria:** Order status cập nhật thành PROCESSING, PreOrderStatus = DEPOSIT_PAID

---

### 2.4 Test Case: Pre-Order - Lần 2 (Thanh Toán Phần Còn Lại)

**Test ID:** TC_PAYMENT_004

**Title:** Thanh toán Remaining cho đơn Pre-Order

**Precondition:**
- Đơn hàng từ TC_PAYMENT_003 (order-003)
- Đã thanh toán 50% tiền cọc thành công
- PreOrderStatus: DEPOSIT_PAID
- Order Status: PROCESSING

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-003/requirement`
   - Verify message: "Đã cọc 50%, vui lòng thanh toán 50% còn lại"
2. Gọi API: `POST /payment/checkout?orderId=order-003`
3. Verify response trả về URL VNPay (Remaining 50%)
4. Simulate VNPay callback (success): `GET /payment/vnpay-callback?vnp_ResponseCode=00&vnp_TxnRef=payment-uuid-2&...`

**Expected Result:**
- Step 1: `depositPercentage = 0.5`, `requiredAmount = 1000000` (phần còn lại)
- Step 2: Trả về VNPay URL với amount = 1000000
- Step 4:
  - Payment record: `status = PAID`, `paymentPurpose = REMAINING`
  - Order record: `preOrderStatus = REMAINING_PAID`, `status = COMPLETED`
  - Transaction created: `type = CHARGE`, `amount = 1000000`

**Pass Criteria:** Order status cập nhật thành COMPLETED, PreOrderStatus = REMAINING_PAID

---

### 2.5 Test Case: Pre-Order - Lịch Sử Thanh Toán

**Test ID:** TC_PAYMENT_005

**Title:** Lấy lịch sử thanh toán cho đơn Pre-Order (đã cọc 2 lần)

**Precondition:**
- Đơn hàng từ TC_PAYMENT_004 (order-003)
- Đã thanh toán 2 lần: DEPOSIT (1,000,000) + REMAINING (1,000,000)

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-003/history`

**Expected Result:**
```json
{
  "result": [
    {
      "id": "payment-uuid-2",
      "paymentMethod": "VNPAY",
      "paymentPurpose": "REMAINING",
      "amount": 1000000,
      "percentage": 0.5,
      "status": "PAID",
      "paymentDate": "2024-01-16T14:45:00"
    },
    {
      "id": "payment-uuid-1",
      "paymentMethod": "VNPAY",
      "paymentPurpose": "DEPOSIT",
      "amount": 1000000,
      "percentage": 0.5,
      "status": "PAID",
      "paymentDate": "2024-01-15T10:30:00"
    }
  ]
}
```

**Pass Criteria:** 
- Trả về 2 payment records
- Sắp xếp theo thứ tự mới nhất trước (REMAINING trước DEPOSIT)
- Cả hai đều có status = PAID

---

### 2.6 Test Case: Prescription Order - 100% VNPay

**Test ID:** TC_PAYMENT_006

**Title:** Thanh toán đơn hàng kê đơn (bắt buộc 100% VNPAY)

**Precondition:**
- Đơn hàng tồn tại với ID: `order-006`
- Order items: [PRESCRIPTION: 2,500,000 VND]
- Order Status: PENDING
- Authenticated user

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-006/requirement`
2. Verify: `allowCOD = false`, `depositPercentage = 1.0`
3. Gọi API: `POST /payment/checkout?orderId=order-006`
4. Verify response trả về URL VNPay
5. Simulate VNPay callback (success)

**Expected Result:**
- Step 1: 
  ```json
  {
    "depositPercentage": 1.0,
    "requiredAmount": 2500000,
    "allowCOD": false,
    "message": "Đơn hàng có sản phẩm kê đơn, bắt buộc thanh toán trước 100%"
  }
  ```
- Step 3: VNPay URL với amount = 2,500,000
- Step 5:
  - Payment: `status = PAID`, `paymentPurpose = FULL`
  - Order: `status = PROCESSING` (cần xử lý kê đơn)
  - Transaction: `type = CHARGE`

**Pass Criteria:** 
- Order Status = PROCESSING (không COMPLETED)
- allowCOD = false (chặn COD)

---

### 2.7 Test Case: Error - COD cho Prescription (Invalid)

**Test ID:** TC_PAYMENT_007

**Title:** Cố gắng thanh toán Prescription bằng COD → Lỗi

**Precondition:**
- Đơn hàng tồn tại với ID: `order-007`
- Order items: [PRESCRIPTION: 2,500,000 VND]
- Payment Method được chọn: COD (INVALID cho PRESCRIPTION)
- Order Status: PENDING

**Test Steps:**
1. Attempt to create order với PRESCRIPTION + COD
2. OR: Gọi API: `POST /payment/checkout?orderId=order-007`

**Expected Result:**
```json
{
  "message": "Invalid payment method for this order",
  "code": 400,
  "result": null
}
```

**Pass Criteria:** Hệ thống từ chối COD cho PRESCRIPTION

---

### 2.8 Test Case: Error - Đơn Hàng Không Tồn Tại

**Test ID:** TC_PAYMENT_008

**Title:** Lấy PaymentRequirement cho đơn hàng không tồn tại

**Precondition:**
- Order ID không tồn tại: `non-existent-order-id`
- Authenticated user

**Test Steps:**
1. Gọi API: `GET /payment/orders/non-existent-order-id/requirement`

**Expected Result:**
```json
{
  "message": "Order not found",
  "code": 404,
  "result": null
}
```

**Pass Criteria:** HTTP Status = 404, error code = ORDER_NOT_FOUND

---

### 2.9 Test Case: Error - Order Đã Hoàn Tất

**Test ID:** TC_PAYMENT_009

**Title:** Cố gắng thanh toán cho đơn hàng đã hoàn tất (COMPLETED)

**Precondition:**
- Đơn hàng có ID: `order-009`
- Order Status: COMPLETED
- Authenticated user

**Test Steps:**
1. Gọi API: `POST /payment/checkout?orderId=order-009`

**Expected Result:**
```json
{
  "message": "Order has already been processed",
  "code": 400,
  "result": null
}
```

**Pass Criteria:** Hệ thống ngăn thanh toán cho order đã COMPLETED

---

### 2.10 Test Case: VNPay Callback - Hash Validation Failed

**Test ID:** TC_PAYMENT_010

**Title:** VNPay callback với chữ ký không hợp lệ

**Precondition:**
- VNPay gửi callback
- vnp_SecureHash không hợp lệ (giả mạo / sai)

**Test Steps:**
1. Simulate VNPay callback với invalid hash
2. Verify hệ thống xác minh hash

**Expected Result:**
- Callback bị từ chối
- Payment record không cập nhật
- Không redirect đến success page
- Log lỗi security

**Pass Criteria:** Hệ thống phát hiện hash không hợp lệ và từ chối

---

### 2.11 Test Case: Authorization - Only Own Order

**Test ID:** TC_PAYMENT_011

**Title:** User không thể xem payment history của đơn hàng người khác

**Precondition:**
- User A (customer) 
- Order tạo bởi User B
- Authenticated as User A

**Test Steps:**
1. Gọi API: `GET /payment/orders/{order-of-user-b}/history`

**Expected Result:**
```json
{
  "message": "User must be the order's customer",
  "code": 403,
  "result": null
}
```

**Pass Criteria:** HTTP Status = 403 Forbidden

---

### 2.12 Test Case: Payment with Mixed Items

**Test ID:** TC_PAYMENT_012

**Title:** Đơn hàng có cả NORMAL và PRE_ORDER → Yêu cầu DEPOSIT

**Precondition:**
- Đơn hàng ID: `order-012`
- Items:
  - NORMAL: 500,000 VND
  - PRE_ORDER: 1,000,000 VND
  - Total: 1,500,000 VND
- depositAmount: 750,000 (50%)
- remainingAmount: 750,000 (50%)

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-012/requirement`

**Expected Result:**
```json
{
  "depositPercentage": 0.5,
  "requiredAmount": 750000,
  "allowCOD": false,
  "message": "Bắt buộc cọc 50% (pre-order)"
}
```

**Pass Criteria:**
- System xác định PRE_ORDER → yêu cầu DEPOSIT 50%
- NORMAL item bị ảnh hưởng bởi PRE_ORDER logic

---

### 2.13 Test Case: Payment with PRESCRIPTION + PRE_ORDER

**Test ID:** TC_PAYMENT_013

**Title:** Đơn hàng có cả PRESCRIPTION và PRE_ORDER → 100% (PRESCRIPTION priority)

**Precondition:**
- Đơn hàng ID: `order-013`
- Items:
  - PRESCRIPTION: 2,000,000 VND
  - PRE_ORDER: 1,000,000 VND
  - Total: 3,000,000 VND

**Test Steps:**
1. Gọi API: `GET /payment/orders/order-013/requirement`

**Expected Result:**
```json
{
  "depositPercentage": 1.0,
  "requiredAmount": 3000000,
  "allowCOD": false,
  "message": "Đơn hàng có sản phẩm kê đơn, bắt buộc thanh toán trước 100%"
}
```

**Pass Criteria:**
- System ưu tiên PRESCRIPTION logic (100%)
- Bỏ qua PRE_ORDER logic (50%)
- Yêu cầu 100% ngay lần 1

---

## 3. Performance Test Cases

### 3.1 Load Test - Payment Checkout

**Test ID:** TC_PERF_001

**Title:** 1000 concurrent checkout requests

**Expected Result:**
- All requests complete within 2 seconds
- No timeout errors
- No database connection errors

---

### 3.2 Stress Test - VNPay Callback

**Test ID:** TC_PERF_002

**Title:** 500 VNPay callback requests per minute

**Expected Result:**
- All callbacks processed within 2 seconds
- Hash validation successful
- Database updates consistent

---

## 4. Security Test Cases

### 4.1 XSS Test

**Test ID:** TC_SEC_001

**Title:** Test XSS injection in payment description

**Test Data:**
```
orderId = "order-123<script>alert('XSS')</script>"
```

**Expected Result:** Input sanitized or rejected

---

### 4.2 SQL Injection Test

**Test ID:** TC_SEC_002

**Title:** Test SQL injection in orderId parameter

**Test Data:**
```
orderId = "order-123'; DROP TABLE payments; --"
```

**Expected Result:** Query fails safely, no data loss

---

### 4.3 Hash Tampering Test

**Test ID:** TC_SEC_003

**Title:** VNPay callback with tampered amount

**Test Data:**
```
vnp_Amount=50000000 (1000x original)
vnp_SecureHash=<recalculated>
```

**Expected Result:** Hash validation fails, callback rejected

---

## 5. Integration Test Scenarios

### 5.1 Complete Flow - Normal + VNPAY

```
1. CreateOrder (NORMAL, VNPAY)
2. GetPaymentRequirement
3. Checkout (khởi tạo Payment)
4. VNPay callback (success)
5. GetPaymentHistory
6. Verify Order status = COMPLETED
```

---

### 5.2 Complete Flow - Pre-Order (2 lần)

```
1. CreateOrder (PRE_ORDER, VNPAY)
2. GetPaymentRequirement (lần 1)
3. Checkout (Deposit 50%)
4. VNPay callback #1 (success)
5. GetPaymentHistory (showing 1 payment)
6. Verify Order status = PROCESSING
7. Verify PreOrderStatus = DEPOSIT_PAID
8. GetPaymentRequirement (lần 2)
9. Checkout (Remaining 50%)
10. VNPay callback #2 (success)
11. GetPaymentHistory (showing 2 payments)
12. Verify Order status = COMPLETED
13. Verify PreOrderStatus = REMAINING_PAID
```

---

### 5.3 Complete Flow - Prescription

```
1. CreateOrder (PRESCRIPTION, VNPAY forced)
2. GetPaymentRequirement
3. Verify allowCOD = false
4. Checkout (100%)
5. VNPay callback (success)
6. Verify Order status = PROCESSING (not COMPLETED)
7. Admin processes prescription (may đo, confirm)
8. Admin updates Order status = COMPLETED
```

---

**Test Execution Guide:**
1. Setup test data (mock orders)
2. Execute test cases theo thứ tự
3. Log kết quả
4. Báo cáo bất kỳ failures
5. Retest failed cases
