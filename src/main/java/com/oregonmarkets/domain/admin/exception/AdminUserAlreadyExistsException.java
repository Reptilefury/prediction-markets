package com.oregonmarkets.domain.admin.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class AdminUserAlreadyExistsException extends RuntimeException {
    private final ResponseCode responseCode;

    public AdminUserAlreadyExistsException(String email) {
        super("Admin user with email " + email + " already exists");
        this.responseCode = ResponseCode.ADMIN_USER_EMAIL_EXISTS;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
