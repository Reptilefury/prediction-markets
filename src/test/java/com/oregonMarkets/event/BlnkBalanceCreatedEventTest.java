package com.oregonmarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlnkBalanceCreatedEventTest {

  @Test
  void builder() {
    BlnkBalanceCreatedEvent event =
        BlnkBalanceCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("0x123")
            .proxyWalletAddress("0xproxy")
            .enclaveUdaAddress("uda")
            .blnkIdentityId("blnk1")
            .blnkBalanceId("bal1")
            .email("test@test.com")
            .magicUserId("magic")
            .didToken("did")
            .timestamp(Instant.now())
            .build();
    assertNotNull(event.getUserId());
  }
}
