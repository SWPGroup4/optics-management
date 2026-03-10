package com.glassystem.optics.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class VnPayDateUtil {

    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid VNPay date");
        }

        return LocalDateTime.parse(raw, FORMATTER);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalStateException("Transaction dateTime is null");
        }

        return dateTime
                .atZone(VNPAY_ZONE)
                .format(FORMATTER);
    }


}
