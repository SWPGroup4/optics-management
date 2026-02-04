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

    PASSWORD_XXX(1008, "Password xxx", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(2001, "Product not found!", HttpStatus.NOT_FOUND),
    PRODUCT_NAME_REQUIRED(2002, "Product name is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_REQUIRED(2003, "Product category is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_BASE_PRICE_REQUIRED(2004, "Product base price is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_BASE_PRICE_INVALID(2005, "Product base price must be greater than or equal to 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_PRESCRIPTION_REQUIRED(2006, "Product prescription flag is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_WEIGHT_INVALID(2007, "Product weight must be greater than or equal to 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_STATUS_REQUIRED(2008, "Product status is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_PRODUCT_REQUIRED(2010, "Product id is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_PRICE_REQUIRED(2011, "Product variant price is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_PRICE_INVALID(2012, "Product variant price must be greater than or equal to 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_STATUS_REQUIRED(2013, "Product variant status is required!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_LENS_WIDTH_INVALID(2014, "Lens width must be greater than 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_BRIDGE_WIDTH_INVALID(2015, "Bridge width must be greater than 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_TEMPLE_LENGTH_INVALID(2016, "Temple length must be greater than 0!", HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_EXISTED(2017, "Product already existed!", HttpStatus.NOT_FOUND),
    IMAGE_LIMIT_EXCEEDED(2017, "Max 5 images per product!", HttpStatus.NOT_FOUND),
    IMAGE_NOT_FOUND(2017, "Image not found!", HttpStatus.NOT_FOUND),

    INVALID_GMAIL(1009, "Invalid gmail!", HttpStatus.BAD_REQUEST),
    INVALID_VNPHONE(1010, "Invalid Vietnam phone!", HttpStatus.BAD_REQUEST),
    CANNOT_UPLOAD_IMAGE(1011, "Could not upload image!", HttpStatus.BAD_REQUEST),
    FAILED_DELETE_IMAGE_S3(1012, "Failed to delete old file image from S3!", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_NOT_FOUND(1013, "Product variant not found!", HttpStatus.BAD_REQUEST),
    INVENTORY_NOT_FOUND(1014, "Product variant not found in inventory!", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(1015, "Product variant is out of stock!", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1016, "Order not found!", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS(1017, "Invalid order status", HttpStatus.BAD_REQUEST),

    INVALID_ORDER_ITEM_TYPE(1018, "Invalid order type", HttpStatus.BAD_REQUEST),
    ORDER_ITEM_NOT_FOUND(1019, "Order item not found!", HttpStatus.BAD_REQUEST),
    PRESCRIPTION_REQUIRED(1020, "Prescription information is required for this item type!", HttpStatus.BAD_REQUEST),

    ORDER_ALREADY_PROCESSED(1021, "Order already processed!", HttpStatus.BAD_REQUEST),

    INVALID_QUANTITY(1022, "Quantity at least 1 !", HttpStatus.BAD_REQUEST),
    FIELD_MISSING(1023, "This information cannot null", HttpStatus.BAD_REQUEST),
    LIST_EMPTY(1024, "Product list cannot null", HttpStatus.BAD_REQUEST),
    CANNOT_REVERT_STATUS(1025, "Order status has progressed too far to be reverted", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1026, "Role not found", HttpStatus.BAD_REQUEST),

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
