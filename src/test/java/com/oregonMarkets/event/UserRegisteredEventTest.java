package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRegisteredEventTest {

  @Test
  void from_CreatesEventWithTimestamp() {
    UUID userId = UUID.randomUUID();
    UUID referredBy = UUID.randomUUID();

    UserRegisteredEvent event =
        UserRegisteredEvent.from(
            userId, "test@example.com", "0x123", "uda-address", "REF123", referredBy);

    assertEquals(userId, event.getUserId());
    assertEquals("test@example.com", event.getEmail());
    assertEquals("0x123", event.getMagicWalletAddress());
    assertEquals("uda-address", event.getEnclaveUdaAddress());
    assertEquals("REF123", event.getReferralCode());
    assertEquals(referredBy, event.getReferredByUserId());
    assertNotNull(event.getTimestamp());
  }
}
