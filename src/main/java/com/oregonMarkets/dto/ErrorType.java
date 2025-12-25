package com.oregonmarkets.dto;

import com.oregonmarkets.common.response.ResponseCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Error type mapping to ResponseCode system Deprecated: Use ResponseCode directly */
@Getter
public enum ErrorType {
  USER_ALREADY_EXISTS(ResponseCode.DUPLICATE_USER, "User already exists"),
  MAGIC_AUTH_FAILED(ResponseCode.MAGIC_AUTH_FAILED, "Magic authentication failed"),
  ENCLAVE_API_ERROR(ResponseCode.ENCLAVE_API_ERROR, "Enclave API error"),
  BLNK_API_ERROR(ResponseCode.BLNK_API_ERROR, "Blnk API error"),
  WEB3_AUTH_FAILED(ResponseCode.WEB3_AUTH_FAILED, "Web3 authentication failed"),
  KEYCLOAK_AUTH_FAILED(ResponseCode.KEYCLOAK_AUTH_FAILED, "Keycloak authentication failed"),
  BLOCKCHAIN_ERROR(ResponseCode.BLOCKCHAIN_ERROR, "Blockchain error"),
  BUSINESS_ERROR(ResponseCode.PROCESSING_ERROR, "Business error"),
  VALIDATION_ERROR(ResponseCode.VALIDATION_ERROR, "Validation failed"),
  INTERNAL_ERROR(ResponseCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

  private final ResponseCode responseCode;
  private final String defaultMessage;

  ErrorType(ResponseCode responseCode, String defaultMessage) {
    this.responseCode = responseCode;
    this.defaultMessage = defaultMessage;
  }

  public String getCode() {
    return this.name();
  }

  public HttpStatus getHttpStatus() {
    return this.responseCode.getHttpStatus();
  }

  public int getStatusCode() {
    return this.responseCode.getCode();
  }
}
