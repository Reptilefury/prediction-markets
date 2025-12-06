package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeycloakProvisionEventTest {

  @Test
  void of() {
    UUID userId = UUID.randomUUID();
    KeycloakProvisionEvent event = KeycloakProvisionEvent.of(userId, "user", "pass");
    assertEquals(userId, event.getUserId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void builder() {
    KeycloakProvisionEvent event =
        KeycloakProvisionEvent.builder()
            .userId(UUID.randomUUID())
            .username("user")
            .initialPassword("pass")
            .timestamp(java.time.Instant.now())
            .build();
    assertNotNull(event.getUserId());
  }
}
