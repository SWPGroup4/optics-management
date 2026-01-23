package com.glassystem.optics.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_KEY(9998, "Invalid message key", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already existed!", HttpStatus.BAD_REQUEST),
    UNCAUGHT_EXCEPTION(9999, "Uncaught Exception!", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1002, "Username must be at least {min} characters!", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1003, "Password must be at least {min} characters!", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed!", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated!", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission!", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_GMAIL(1009, "Invalid gmail!", HttpStatus.BAD_REQUEST),
    INVALID_VNPHONE(1010, "Invalid Vietnam phone!", HttpStatus.BAD_REQUEST),
    CANNOT_UPLOAD_IMAGE(1011, "Could not upload image!", HttpStatus.BAD_REQUEST),
    FAILED_DELETE_IMAGE_S3(1012, "Failed to delete old file image from S3!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_NOT_FOUND(1013, "Product variant not found!", HttpStatus.BAD_REQUEST),
    INVENTORY_NOT_FOUND(1014, "Product variant not found in inventory!", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(1015, "Product variant is out of stock!", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1016, "Order not found!", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS(1017, "Invalid order status", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_TYPE(1018, "Invalid order type", HttpStatus.BAD_REQUEST),
    ORDER_ITEM_NOT_FOUND(1017, "Order item not found!", HttpStatus.BAD_REQUEST),
    ;

    private ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
