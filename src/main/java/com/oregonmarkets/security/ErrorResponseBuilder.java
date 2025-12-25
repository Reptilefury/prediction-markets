package com.oregonmarkets.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonmarkets.common.exception.ResponseSerializationException;
import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.dto.ErrorResponse;
import com.oregonmarkets.dto.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorResponseBuilder {

  private final ObjectMapper objectMapper;

  /**
   * Build error response using the legacy ErrorType system Returns format: { "error": "ERROR_CODE",
   * "message": "...", "code": 401, "status": "FAILED", "timestamp": "..." }
   */
  public byte[] buildErrorResponse(ErrorType errorType, String message) {
    ErrorResponse errorResponse = ErrorResponse.from(errorType, message);

    try {
      return objectMapper.writeValueAsBytes(errorResponse);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize error response", e);
      throw new ResponseSerializationException("ErrorType response serialization failed", e);
    }
  }

  /** Build error response using the standardized ApiResponse system */
  public byte[] buildApiErrorResponse(ResponseCode responseCode, String message) {
    ApiResponse<Void> errorResponse = ApiResponse.error(responseCode, message);

    try {
      return objectMapper.writeValueAsBytes(errorResponse);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize error response", e);
      throw new ResponseSerializationException("ApiResponse serialization failed", e);
    }
  }
}
