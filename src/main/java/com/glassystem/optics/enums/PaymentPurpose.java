package com.glassystem.optics.enums;

public enum PaymentPurpose {
    DEPOSIT,    // 50% thanh toán trước (dùng cho pre-order)
    FULL,       // 100% thanh toán hoàn toàn
    REMAINING   // 50% thanh toán còn lại (sau khi DEPOSIT)
}
