package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ResponseCode;
import lombok.Getter;

/** Base exception for business logic errors */
@Getter
public class BusinessException extends RuntimeException {
  private final ResponseCode responseCode;
  private final String details;

  public BusinessException(ResponseCode responseCode) {
    super(responseCode.getMessage());
    this.responseCode = responseCode;
    this.details = null;
  }

  public BusinessException(ResponseCode responseCode, String message) {
    super(message);
    this.responseCode = responseCode;
    this.details = null;
  }

  public BusinessException(ResponseCode responseCode, String message, String details) {
    super(message);
    this.responseCode = responseCode;
    this.details = details;
  }

  public BusinessException(ResponseCode responseCode, String message, Throwable cause) {
    super(message, cause);
    this.responseCode = responseCode;
    this.details = null;
  }
}
