package com.oregonMarkets.common.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ResponseCodeTest {

  @Test
  void fromCode_ExistingCode() {
    assertEquals(ResponseCode.SUCCESS, ResponseCode.fromCode(2000));
    assertEquals(ResponseCode.USER_ALREADY_EXISTS, ResponseCode.fromCode(3320));
  }

  @Test
  void fromCode_NonExistingCode() {
    assertEquals(ResponseCode.UNEXPECTED_ERROR, ResponseCode.fromCode(9999));
  }

  @Test
  void isSuccess() {
    assertTrue(ResponseCode.SUCCESS.isSuccess());
    assertTrue(ResponseCode.USER_REGISTERED.isSuccess());
    assertFalse(ResponseCode.VALIDATION_ERROR.isSuccess());
  }

  @Test
  void isClientError() {
    assertTrue(ResponseCode.VALIDATION_ERROR.isClientError());
    assertTrue(ResponseCode.USER_ALREADY_EXISTS.isClientError());
    assertFalse(ResponseCode.SUCCESS.isClientError());
  }

  @Test
  void isServerError() {
    assertTrue(ResponseCode.INTERNAL_SERVER_ERROR.isServerError());
    assertTrue(ResponseCode.MAGIC_AUTH_FAILED.isServerError());
    assertFalse(ResponseCode.SUCCESS.isServerError());
  }

  @Test
  void isCritical() {
    assertTrue(ResponseCode.CRITICAL_PAYMENT_ERROR.isCritical());
    assertFalse(ResponseCode.SUCCESS.isCritical());
  }

  @Test
  void getters() {
    assertEquals(2000, ResponseCode.SUCCESS.getCode());
    assertEquals("Operation completed successfully", ResponseCode.SUCCESS.getMessage());
    assertEquals(HttpStatus.OK, ResponseCode.SUCCESS.getHttpStatus());
  }
}
