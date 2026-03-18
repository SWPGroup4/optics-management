package com.glassystem.optics.enums;

public enum NotificationTemplate {
    ORDER_CREATED(
            "Dat hang thanh cong",
            "Don hang %s da duoc tao thanh cong. Trang thai hien tai: %s."
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
