package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MagicAuthException extends BusinessException {

  public MagicAuthException(String message) {
    super(ResponseCode.MAGIC_AUTH_FAILED, message);
  }

  public MagicAuthException(String message, Throwable cause) {
    super(ResponseCode.MAGIC_AUTH_FAILED, message, cause);
  }
}
