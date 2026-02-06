package com.glassystem.optics.enums;

public enum TransactionType {
    DEPOSIT,           // 50% đặt cọc
    CHARGE,            // 100% hoặc 50% còn lại
    REFUND,            // Hoàn 100%
    PARTIAL_REFUND     // Hoàn 50%
}
