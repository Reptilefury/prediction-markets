package com.oregonmarkets.domain.admin.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class AdminUserNotFoundException extends RuntimeException {
    private final ResponseCode responseCode;

    public AdminUserNotFoundException(String identifier) {
        super("Admin user not found: " + identifier);
        this.responseCode = ResponseCode.ADMIN_USER_NOT_FOUND;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
