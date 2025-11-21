package com.oregonMarkets.integration.keycloak;

import com.oregonMarkets.event.KeycloakProvisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakProvisionListener {

    private final KeycloakAdminClient adminClient;

    @EventListener
    public void onProvisionRequested(KeycloakProvisionEvent event) {
        log.info("Received KeycloakProvisionEvent for user {} ({})", event.getUserId(), event.getEmail());
        // Fire-and-forget to avoid blocking registration request path
        adminClient.createUserIfAbsent(event.getEmail(), event.getInitialPassword())
                .onErrorResume(e -> {
                    log.error("Failed to provision Keycloak user {}: {}", event.getEmail(), e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
