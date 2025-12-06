package com.oregonMarkets.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRegisteredEventTest {

  @Test
  void from() {
    UUID userId = UUID.randomUUID();
    UserRegisteredEvent event =
        UserRegisteredEvent.from(userId, "test@test.com", "0x123", "uda123", "ref", null);
    assertEquals(userId, event.getUserId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void constructor() {
    UserRegisteredEvent event = new UserRegisteredEvent();
    event.setUserId(UUID.randomUUID());
    assertNotNull(event.getUserId());
  }
}
