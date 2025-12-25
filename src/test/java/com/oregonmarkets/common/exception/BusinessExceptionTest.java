package com.oregonmarkets.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonmarkets.common.response.ResponseCode;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

  @Test
  void constructor_WithResponseCodeAndMessage() {
    BusinessException exception =
        new BusinessException(ResponseCode.VALIDATION_ERROR, "Test message");

    assertEquals(ResponseCode.VALIDATION_ERROR, exception.getResponseCode());
    assertEquals("Test message", exception.getMessage());
  }

  @Test
  void constructor_WithResponseCodeMessageAndCause() {
    RuntimeException cause = new RuntimeException("Root cause");
    BusinessException exception =
        new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "Test message", cause);

    assertEquals(ResponseCode.INTERNAL_SERVER_ERROR, exception.getResponseCode());
    assertEquals("Test message", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void constructor_WithResponseCodeOnly() {
    BusinessException exception = new BusinessException(ResponseCode.UNAUTHORIZED);

    assertEquals(ResponseCode.UNAUTHORIZED, exception.getResponseCode());
    assertEquals(ResponseCode.UNAUTHORIZED.getMessage(), exception.getMessage());
  }
}
