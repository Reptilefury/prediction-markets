package com.oregonMarkets.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionTests {

  @Test
  void userAlreadyExistsException() {
    UserAlreadyExistsException ex = new UserAlreadyExistsException("test@test.com");
    assertNotNull(ex.getMessage());
  }

  @Test
  void userAlreadyExistsException_WithField() {
    UserAlreadyExistsException ex = new UserAlreadyExistsException("email", "test@test.com");
    assertNotNull(ex.getMessage());
  }

  @Test
  void blockchainException() {
    BlockchainException ex = new BlockchainException("error");
    assertNotNull(ex.getMessage());
  }

  @Test
  void blockchainException_WithCause() {
    BlockchainException ex = new BlockchainException("error", new RuntimeException());
    assertNotNull(ex.getCause());
  }

  @Test
  void enclaveApiException() {
    EnclaveApiException ex = new EnclaveApiException("error");
    assertNotNull(ex.getMessage());
  }

  @Test
  void enclaveApiException_WithCause() {
    EnclaveApiException ex = new EnclaveApiException("error", new RuntimeException());
    assertNotNull(ex.getCause());
  }

  @Test
  void externalServiceException() {
    ExternalServiceException ex = ExternalServiceException.blockchain("error");
    assertNotNull(ex.getMessage());
  }

  @Test
  void externalServiceException_Blnk() {
    ExternalServiceException ex = ExternalServiceException.blnk("error");
    assertNotNull(ex.getMessage());
  }

  @Test
  void externalServiceException_Polymarket() {
    ExternalServiceException ex = ExternalServiceException.polymarket("error");
    assertNotNull(ex.getMessage());
  }
}
