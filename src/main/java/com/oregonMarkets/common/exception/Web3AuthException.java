package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class Web3AuthException extends BusinessException {

  public Web3AuthException(String message) {
    super(ResponseCode.WEB3_AUTH_FAILED, message);
  }

  public Web3AuthException(String message, Throwable cause) {
    super(ResponseCode.WEB3_AUTH_FAILED, message, cause);
  }
}
