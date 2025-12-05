package com.oregonMarkets.integration.keycloak;

import com.oregonMarkets.event.AssetsGenerationEvent;
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
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

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
                .subscribe(
                    v -> {
                        log.info("Successfully provisioned Keycloak user {} for userId {}",
                                username, event.getUserId());
                        
                        // Trigger assets generation as final step
                        AssetsGenerationEvent assetsEvent = AssetsGenerationEvent.builder()
                                .userId(event.getUserId())
                                .email(event.getEmail())
                                .proxyWalletAddress(event.getProxyWalletAddress())
                                .enclaveUdaAddress(event.getEnclaveUdaAddress())
                                .magicWalletAddress(event.getMagicWalletAddress())
                                .timestamp(java.time.Instant.now())
                                .build();
                        eventPublisher.publishEvent(assetsEvent);
                        log.info("Triggered assets generation for user: {}", event.getUserId());
                    },
                    e -> log.error("Failed to provision Keycloak user {} for userId {}: {}",
                            username, event.getUserId(), e.getMessage())
                );
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
                .subscribe(
                    v -> log.info("Successfully provisioned Keycloak user {}", event.getUsername()),
                    e -> log.error("Failed to provision Keycloak user {}: {}", event.getUsername(), e.getMessage())
                );
    }
}
