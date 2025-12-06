package com.oregonMarkets.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonMarkets.common.response.ResponseCode;
import org.junit.jupiter.api.Test;

class ExternalServiceExceptionTest {

  @Test
  void constructor_WithResponseCodeServiceNameAndMessage() {
    ExternalServiceException exception =
        new ExternalServiceException(
            ResponseCode.EXTERNAL_SERVICE_ERROR, "TestService", "Service unavailable");

    assertEquals("Service unavailable", exception.getMessage());
    assertEquals("TestService", exception.getServiceName());
    assertEquals(ResponseCode.EXTERNAL_SERVICE_ERROR, exception.getResponseCode());
  }

  @Test
  void constructor_WithResponseCodeServiceNameMessageAndCause() {
    RuntimeException cause = new RuntimeException("Network error");
    ExternalServiceException exception =
        new ExternalServiceException(
            ResponseCode.EXTERNAL_SERVICE_ERROR, "PaymentService", "Service failed", cause);

    assertEquals("Service failed", exception.getMessage());
    assertEquals("PaymentService", exception.getServiceName());
    assertEquals(ResponseCode.EXTERNAL_SERVICE_ERROR, exception.getResponseCode());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void getServiceName_ReturnsCorrectValue() {
    ExternalServiceException exception =
        new ExternalServiceException(ResponseCode.EXTERNAL_SERVICE_ERROR, "AuthService", "Error");

    assertEquals("AuthService", exception.getServiceName());
  }

  @Test
  void staticFactoryMethods() {
    ExternalServiceException blockchainEx = ExternalServiceException.blockchain("Blockchain error");
    assertEquals("Blockchain", blockchainEx.getServiceName());
    assertEquals(ResponseCode.BLOCKCHAIN_ERROR, blockchainEx.getResponseCode());

    ExternalServiceException blnkEx = ExternalServiceException.blnk("Blnk error");
    assertEquals("Blnk Core", blnkEx.getServiceName());
    assertEquals(ResponseCode.EXTERNAL_SERVICE_ERROR, blnkEx.getResponseCode());

    ExternalServiceException polymarketEx = ExternalServiceException.polymarket("Polymarket error");
    assertEquals("Polymarket", polymarketEx.getServiceName());
    assertEquals(ResponseCode.EXTERNAL_SERVICE_ERROR, polymarketEx.getResponseCode());
  }
}
