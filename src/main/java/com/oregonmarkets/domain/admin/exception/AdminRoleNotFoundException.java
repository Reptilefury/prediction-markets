package com.oregonmarkets.domain.admin.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class AdminRoleNotFoundException extends RuntimeException {
    private final ResponseCode responseCode;

    public AdminRoleNotFoundException(String identifier) {
        super("Admin role not found: " + identifier);
        this.responseCode = ResponseCode.ADMIN_ROLE_NOT_FOUND;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
