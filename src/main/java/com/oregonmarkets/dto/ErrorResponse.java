package com.oregonmarkets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Legacy error response format Deprecated: Use ApiResponse instead */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
  @JsonProperty("status")
  private String status;

  @JsonProperty("message")
  private String errorMessage;

  @JsonProperty("code")
  private String statusCode;

  @JsonProperty("error")
  private String errorCode;

  @JsonProperty("timestamp")
  private Instant timestamp;

  /** Create an ErrorResponse from ErrorType */
  public static ErrorResponse from(ErrorType errorType, String message) {
    return ErrorResponse.builder()
        .errorCode(errorType.getCode())
        .errorMessage(message != null ? message : errorType.getDefaultMessage())
        .statusCode(String.valueOf(errorType.getStatusCode()))
        .status("FAILED")
        .timestamp(Instant.now())
        .build();
  }
}
