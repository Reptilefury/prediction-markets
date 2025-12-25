package com.oregonmarkets.domain.payment.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DepositTest {

  @Test
  void builder_CreatesDepositWithAllFields() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    Deposit deposit =
        Deposit.builder()
            .id(id)
            .userId(userId)
            .amount(new BigDecimal("100.50"))
            .currency("USDC")
            .txHash("0x123")
            .blockNumber(12345L)
            .method(Deposit.DepositMethod.CRYPTO)
            .status(Deposit.DepositStatus.CONFIRMED)
            .processingStatus(Deposit.ProcessingStatus.COMPLETED)
            .build();

    assertEquals(id, deposit.getId());
    assertEquals(userId, deposit.getUserId());
    assertEquals(new BigDecimal("100.50"), deposit.getAmount());
    assertEquals("USDC", deposit.getCurrency());
    assertEquals("0x123", deposit.getTxHash());
    assertEquals(12345L, deposit.getBlockNumber());
    assertEquals(Deposit.DepositMethod.CRYPTO, deposit.getMethod());
    assertEquals(Deposit.DepositStatus.CONFIRMED, deposit.getStatus());
    assertEquals(Deposit.ProcessingStatus.COMPLETED, deposit.getProcessingStatus());
    // timestamps removed for security - no longer tested
  }

  @Test
  void depositMethod_EnumValues() {
    assertEquals("CRYPTO", Deposit.DepositMethod.CRYPTO.name());
    assertEquals("MPESA", Deposit.DepositMethod.MPESA.name());
    assertEquals("AIRTEL_MONEY", Deposit.DepositMethod.AIRTEL_MONEY.name());
  }

  @Test
  void depositStatus_EnumValues() {
    assertEquals("PENDING", Deposit.DepositStatus.PENDING.name());
    assertEquals("CONFIRMED", Deposit.DepositStatus.CONFIRMED.name());
    assertEquals("COMPLETED", Deposit.DepositStatus.COMPLETED.name());
    assertEquals("FAILED", Deposit.DepositStatus.FAILED.name());
  }

  @Test
  void processingStatus_EnumValues() {
    assertEquals("DETECTED", Deposit.ProcessingStatus.DETECTED.name());
    assertEquals("CONFIRMING", Deposit.ProcessingStatus.CONFIRMING.name());
    assertEquals("CONFIRMED", Deposit.ProcessingStatus.CONFIRMED.name());
    assertEquals("PROCESSING", Deposit.ProcessingStatus.PROCESSING.name());
    assertEquals("CREDITED", Deposit.ProcessingStatus.CREDITED.name());
    assertEquals("COMPLETED", Deposit.ProcessingStatus.COMPLETED.name());
    assertEquals("FAILED", Deposit.ProcessingStatus.FAILED.name());
  }
}
