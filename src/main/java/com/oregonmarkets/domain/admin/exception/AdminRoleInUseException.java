package com.oregonmarkets.domain.admin.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class AdminRoleInUseException extends RuntimeException {
    private final ResponseCode responseCode;

    public AdminRoleInUseException(String roleName) {
        super("Admin role is currently in use and cannot be deleted: " + roleName);
        this.responseCode = ResponseCode.ADMIN_ROLE_IN_USE;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
