package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;
import lombok.Getter;

/** Exception thrown when response serialization fails */
@Getter
public class ResponseSerializationException extends BusinessException {

  public ResponseSerializationException(String message) {
    super(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to serialize response: " + message);
  }

  public ResponseSerializationException(String message, Throwable cause) {
    super(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to serialize response: " + message, cause);
  }
}
