package com.oregonmarkets.common.exception;

import com.oregonmarkets.common.response.ResponseCode;

public class EnclaveApiException extends BusinessException {

  public EnclaveApiException(String message) {
    super(ResponseCode.ENCLAVE_API_ERROR, message);
  }

  public EnclaveApiException(String message, Throwable cause) {
    super(ResponseCode.ENCLAVE_API_ERROR, message, cause);
  }
}
