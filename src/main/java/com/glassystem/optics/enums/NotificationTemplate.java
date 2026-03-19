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
    ),
    ORDER_SHIPPED(
            "Don hang da duoc ban giao cho van chuyen",
            "Don %s da duoc ban giao cho don vi van chuyen."
    ),
    ORDER_DELIVERING(
            "Don hang dang duoc giao",
            "Don %s dang tren duong giao den ban."
    ),
    ORDER_DELIVERED(
            "Don hang da giao thanh cong",
            "Don %s da duoc giao thanh cong."
    ),
    ORDER_COMPLETED(
            "Don hang da hoan tat",
            "Cam on ban! Don %s da hoan tat."
    ),
    ORDER_CANCELLED(
            "Don hang da bi huy",
            "Don %s da bi huy. %s"
    ),
    REFUND_CREATED(
            "Yeu cau hoan tien da duoc tao",
            "Yeu cau hoan tien cho don %s da duoc tao thanh cong."
    ),
    REFUND_COMPLETED(
            "Hoan tien thanh cong",
            "Khoan hoan tien cho don %s da hoan tat."
    ),
    REFUND_FAILED(
            "Hoan tien chua thanh cong",
            "Khoan hoan tien cho don %s that bai. Vui long thu lai."
    ),
    STAFF_ORDER_AWAITING_VERIFICATION(
            "Co don moi cho xac minh",
            "Don %s dang o trang thai AWAITING_VERIFICATION."
    ),
    STAFF_ORDER_ON_HOLD(
            "Don hang can bo sung thong tin",
            "Don %s dang o trang thai ON_HOLD va can duoc theo doi voi khach hang."
    ),
    STAFF_CANCELLED_PAID_ORDER(
            "Co don huy can hoan tien",
            "Don %s da huy nhung da phat sinh thanh toan."
    ),
    STAFF_REFUND_READY(
            "Co refund san sang xu ly",
            "Refund cho don %s da san sang de xu ly."
    ),
    STAFF_ORDER_READY_TO_SHIP(
            "Co don cho nhan giao",
            "Don %s da san sang cho van chuyen."
    ),
    SHIPPER_ORDER_ASSIGNED(
            "Ban vua duoc giao don moi",
            "Don %s da duoc assign cho ban."
    ),
    ORDER_PRODUCTION_STARTED(
            "Don hang bat dau san xuat",
            "Don %s da bat dau duoc san xuat."
    ),
    STAFF_ORDER_PRODUCTION_STARTED(
            "Don hang da bat dau san xuat",
            "Don %s da chuyen sang giai doan san xuat."
    ),
    ORDER_PRODUCTION_COMPLETED(
            "Don hang da san xuat xong",
            "Don %s da san xuat xong va san sang cho buoc tiep theo."
    ),
    STAFF_ORDER_PRODUCTION_COMPLETED(
            "Don hang da san xuat xong",
            "Don %s da hoan tat san xuat."
    ),
    SHIPPER_ORDER_READY_AFTER_PRODUCTION(
            "Co don san sang nhan giao",
            "Don %s da san xuat xong va co the nhan giao."
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
