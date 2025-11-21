package com.oregonMarkets.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class KeycloakProvisionEvent {
    UUID userId;
    String email;
    String initialPassword; // DID token at registration time
    Instant timestamp;

    public static KeycloakProvisionEvent of(UUID userId, String email, String initialPassword) {
        return KeycloakProvisionEvent.builder()
                .userId(userId)
                .email(email)
                .initialPassword(initialPassword)
                .timestamp(Instant.now())
                .build();
    }
}
