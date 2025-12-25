package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class KeycloakAuthException extends BusinessException {

  public KeycloakAuthException(String message) {
    super(ResponseCode.KEYCLOAK_AUTH_FAILED, message);
  }

  public KeycloakAuthException(String message, Throwable cause) {
    super(ResponseCode.KEYCLOAK_AUTH_FAILED, message, cause);
  }
}
