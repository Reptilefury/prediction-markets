package com.oregonmarkets.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

  @Test
  void from() {
    ErrorResponse response = ErrorResponse.from(ErrorType.MAGIC_AUTH_FAILED, "test");
    assertNotNull(response);
    assertEquals("test", response.getErrorMessage());
  }

  @Test
  void builder() {
    ErrorResponse response =
        ErrorResponse.builder()
            .status("FAILED")
            .errorMessage("msg")
            .statusCode("401")
            .errorCode("ERR")
            .timestamp(Instant.now())
            .build();
    assertNotNull(response);
  }

  @Test
  void setters() {
    ErrorResponse response = new ErrorResponse();
    response.setStatus("FAILED");
    response.setErrorMessage("msg");
    response.setStatusCode("401");
    response.setErrorCode("ERR");
    response.setTimestamp(Instant.now());
    assertEquals("FAILED", response.getStatus());
  }
}
