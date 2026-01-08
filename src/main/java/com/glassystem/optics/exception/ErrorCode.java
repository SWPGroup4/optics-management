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
