package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ResponseCode;

public class KeycloakAuthException extends BusinessException {

    public KeycloakAuthException(String message) {
        super(ResponseCode.KEYCLOAK_AUTH_FAILED, message);
    }

    public KeycloakAuthException(String message, Throwable cause) {
        super(ResponseCode.KEYCLOAK_AUTH_FAILED, message, cause);
    }
}
