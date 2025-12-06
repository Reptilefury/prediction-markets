package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeycloakProvisionEventTest {

  @Test
  void builder_CreatesEventWithAllFields() {
    UUID userId = UUID.randomUUID();
    Instant timestamp = Instant.now();

    KeycloakProvisionEvent event =
        KeycloakProvisionEvent.builder()
            .userId(userId)
            .username("testuser")
            .initialPassword("password123")
            .timestamp(timestamp)
            .build();

    assertEquals(userId, event.getUserId());
    assertEquals("testuser", event.getUsername());
    assertEquals("password123", event.getInitialPassword());
    assertEquals(timestamp, event.getTimestamp());
  }

  @Test
  void builder_CreatesEventWithMinimalFields() {
    UUID userId = UUID.randomUUID();

    KeycloakProvisionEvent event =
        KeycloakProvisionEvent.builder().userId(userId).username("testuser").build();

    assertEquals(userId, event.getUserId());
    assertEquals("testuser", event.getUsername());
    assertNull(event.getInitialPassword());
    assertNull(event.getTimestamp());
  }

  @Test
  void of_CreatesEventWithTimestamp() {
    UUID userId = UUID.randomUUID();

    KeycloakProvisionEvent event = KeycloakProvisionEvent.of(userId, "testuser", "password123");

    assertEquals(userId, event.getUserId());
    assertEquals("testuser", event.getUsername());
    assertEquals("password123", event.getInitialPassword());
    assertNotNull(event.getTimestamp());
  }
}
