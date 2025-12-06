package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProxyWalletCreatedEventTest {

  @Test
  void builder() {
    ProxyWalletCreatedEvent event =
        ProxyWalletCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .magicWalletAddress("0x123")
            .proxyWalletAddress("0xproxy")
            .email("test@test.com")
            .magicUserId("magic123")
            .didToken("did:token")
            .timestamp(Instant.now())
            .build();
    assertNotNull(event.getUserId());
  }

  @Test
  void constructor() {
    ProxyWalletCreatedEvent event = new ProxyWalletCreatedEvent();
    event.setUserId(UUID.randomUUID());
    assertNotNull(event.getUserId());
  }
}
