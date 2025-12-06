package com.oregonMarkets.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KeycloakProvisionEvent {
  UUID userId;
  String username; // Magic user ID (sub field from DID token) - used as Keycloak username
  String initialPassword; // DID token at registration time
  Instant timestamp;

  public static KeycloakProvisionEvent of(UUID userId, String username, String initialPassword) {
    return KeycloakProvisionEvent.builder()
        .userId(userId)
        .username(username)
        .initialPassword(initialPassword)
        .timestamp(Instant.now())
        .build();
  }
}
