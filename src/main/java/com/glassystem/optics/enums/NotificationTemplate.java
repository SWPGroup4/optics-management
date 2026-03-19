package com.glassystem.optics.enums;

public enum NotificationTemplate {
    ORDER_CREATED(
            "Dat hang thanh cong",
            "Don hang %s da duoc tao thanh cong. Trang thai hien tai: %s."
    ),
    FULL_PAID_SUCCESS(
            "Thanh toan thanh cong",
            "Ban da thanh toan %s cho don %s. Hinh thuc: %s."
    ),
    DEPOSIT_PAID_SUCCESS(
            "Thanh toan thanh cong",
            "Ban da thanh toan %s cho don %s. Hinh thuc: %s."
    ),
    REMAINING_PAID_SUCCESS(
            "Thanh toan thanh cong",
            "Ban da thanh toan %s cho don %s. Hinh thuc: %s."
    ),
    PAYMENT_FAILED(
            "Thanh toan chua thanh cong",
            "Thanh toan cho don %s that bai. Vui long thu lai."
    ),
    ORDER_AWAITING_VERIFICATION(
            "Don hang dang cho xac minh",
            "Don %s da thanh toan va dang cho nhan vien xac minh thong tin."
    ),
    ORDER_VERIFIED_APPROVED(
            "Don hang da duoc xac minh",
            "Don %s da duoc nhan vien xac minh thanh cong."
    ),
    ORDER_VERIFIED_REJECTED(
            "Don hang can bo sung thong tin",
            "Don %s can bo sung them thong tin truoc khi tiep tuc xu ly."
    ),
    ORDER_ON_HOLD(
            "Don hang can dien lai thong tin don thuoc",
            "Don %s dang tam dung va can dien lai thong tin don thuoc."
    ),
    REMAINING_PAYMENT_DUE(
            "Den han thanh toan phan con lai",
            "Don %s da san sang cho buoc tiep theo. Vui long thanh toan %s."
    );

    private final String titleTemplate;
    private final String contentTemplate;

    NotificationTemplate(String titleTemplate, String contentTemplate) {
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
    }

    public String getTitle(Object... args) {
        return format(titleTemplate, args);
    }

    public String getContent(Object... args) {
        return format(contentTemplate, args);
    }

    private String format(String template, Object... args) {
        return args == null || args.length == 0 ? template : String.format(template, args);
    }
}
