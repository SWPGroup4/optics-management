package com.glassystem.optics.enums;

public enum OrderStatus {
    PENDING,                    // Mới tạo
    AWAITING_VERIFICATION,      // Đã thanh toán, chờ staff xác minh
    ON_HOLD,                    // Bị tạm dừng (do đơn thuốc chưa valid)
    CONFIRMED,                  // Đã xác nhận đơn
    PROCESSING,                 // Có ít nhất 1 item đang sản xuất
    PRODUCED,                   // Tất cả item đã xong
    SHIPPED,                    // Đã giao cho vận chuyển
    COMPLETED,                  // Thành công
    CANCELLED                   // Hủy đơn
}
