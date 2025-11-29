package com.oregonMarkets.integration.keycloak;

import com.oregonMarkets.event.BlnkBalanceCreatedEvent;
import com.oregonMarkets.event.KeycloakProvisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakProvisionListener {

    private final KeycloakAdminClient adminClient;

    /**
     * Listen for BlnkBalanceCreatedEvent (new async flow)
     * This is the final step in the async registration orchestration chain
     */
    @EventListener
    public void onBlnkBalanceCreated(BlnkBalanceCreatedEvent event) {
        log.info("Received BlnkBalanceCreatedEvent for user {}, provisioning Keycloak user", event.getUserId());
        // Fire-and-forget pattern using async scheduling
        // Clean Magic User ID: lowercase and remove padding (=)
        String username = event.getMagicUserId().toLowerCase().replace("=", "");
        String password = event.getDidToken(); // Full DID token as password
        String email = event.getEmail(); // User's email

        adminClient.createUserIfAbsent(username, password, email)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Failed to provision Keycloak user {} for userId {}: {}",
                            username, event.getUserId(), e.getMessage());
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("Successfully provisioned Keycloak user {} for userId {}",
                        username, event.getUserId()))
                .subscribe();
    }

    /**
     * Legacy support: Listen for KeycloakProvisionEvent (old synchronous flow)
     * This can be removed once all code paths are migrated to the async flow
     */
    @EventListener
    public void onProvisionRequested(KeycloakProvisionEvent event) {
        log.info("Received KeycloakProvisionEvent for user {} (Magic user ID: {})", event.getUserId(), event.getUsername());
        // Fire-and-forget to avoid blocking registration request path
        adminClient.createUserIfAbsent(event.getUsername(), event.getInitialPassword())
                .onErrorResume(e -> {
                    log.error("Failed to provision Keycloak user {}: {}", event.getUsername(), e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
