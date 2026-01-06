package com.oregonmarkets.domain.admin.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class AdminPermissionNotFoundException extends RuntimeException {
    private final ResponseCode responseCode;

    public AdminPermissionNotFoundException(String identifier) {
        super("Admin permission not found: " + identifier);
        this.responseCode = ResponseCode.ADMIN_PERMISSION_NOT_FOUND;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
