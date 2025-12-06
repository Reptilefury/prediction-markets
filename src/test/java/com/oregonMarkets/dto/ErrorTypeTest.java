package com.oregonMarkets.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ErrorTypeTest {

  @Test
  void getCode() {
    assertEquals("MAGIC_AUTH_FAILED", ErrorType.MAGIC_AUTH_FAILED.getCode());
  }

  @Test
  void getHttpStatus() {
    assertNotNull(ErrorType.MAGIC_AUTH_FAILED.getHttpStatus());
  }

  @Test
  void getStatusCode() {
    assertTrue(ErrorType.MAGIC_AUTH_FAILED.getStatusCode() > 0);
  }

  @Test
  void getDefaultMessage() {
    assertNotNull(ErrorType.MAGIC_AUTH_FAILED.getDefaultMessage());
  }

  @Test
  void allErrorTypes() {
    for (ErrorType type : ErrorType.values()) {
      assertNotNull(type.getCode());
      assertNotNull(type.getHttpStatus());
      assertNotNull(type.getDefaultMessage());
    }
  }
}
