package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class KeycloakAuthException extends BusinessException {

  public KeycloakAuthException(String message) {
    super(ResponseCode.UNAUTHORIZED, message);
  }

  public KeycloakAuthException(String message, ResponseCode responseCode) {
    super(responseCode, message);
  }

  public KeycloakAuthException(String message, Throwable cause) {
    super(ResponseCode.UNAUTHORIZED, message, cause);
  }
}
