package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnclaveUdaCreatedEventTest {

  @Test
  void builder() {
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("0x123")
            .proxyWalletAddress("0xproxy")
            .enclaveUdaAddress("uda")
            .email("test@test.com")
            .magicUserId("magic")
            .didToken("did")
            .timestamp(Instant.now())
            .build();
    assertNotNull(event.getUserId());
  }
}
