package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ResponseCode;

public class BlnkApiException extends BusinessException {

  public BlnkApiException(String message) {
    super(ResponseCode.BLNK_API_ERROR, message);
  }

  public BlnkApiException(String message, Throwable cause) {
    super(ResponseCode.BLNK_API_ERROR, message, cause);
  }
}
