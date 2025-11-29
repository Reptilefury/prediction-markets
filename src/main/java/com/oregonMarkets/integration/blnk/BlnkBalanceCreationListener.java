package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.BlnkBalanceCreatedEvent;
import com.oregonMarkets.event.EnclaveUdaCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

/**
 * Listens for EnclaveUdaCreatedEvent and asynchronously creates Blnk identity and balance
 * Once Blnk balance is created, publishes BlnkBalanceCreatedEvent to trigger Keycloak provisioning
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlnkBalanceCreationListener {

    private final BlnkClient blnkClient;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @EventListener
    public void onEnclaveUdaCreated(EnclaveUdaCreatedEvent event) {
        userRepository.findById(event.getUserId())
                .flatMap(user -> {
                    // Check if user already has identity
                    if (user.getBlnkIdentityId() != null) {
                        log.info("User {} has existing Blnk identity: {}, checking balances", 
                                event.getUserId(), user.getBlnkIdentityId());
                        
                        // Query Blnk for existing balances
                        return blnkClient.getBalancesByIdentity(user.getBlnkIdentityId())
                                .flatMap(balances -> {
                                    // Check if USDC balance exists
                                    boolean hasUsdcBalance = balances.stream()
                                            .anyMatch(balance -> "USDC".equals(balance.get("currency")));
                                    
                                    if (hasUsdcBalance) {
                                        log.info("User {} already has USDC balance, skipping creation", event.getUserId());
                                        return Mono.empty(); // Skip creation
                                    } else {
                                        log.info("User {} has identity but no USDC balance, creating USDC balance", event.getUserId());
                                        return createUsdcBalance(user.getBlnkIdentityId(), user, event);
                                    }
                                });
                    } else {
                        log.info("User {} has no Blnk identity, creating identity and USDC balance", event.getUserId());
                        // Create identity first, then USDC balance
                        return blnkClient.createIdentity(
                                event.getUserId().toString(),
                                event.getEmail(),
                                java.util.Map.of(
                                    "enclaveUdaAddress", event.getEnclaveUdaAddress(),
                                    "proxyWalletAddress", event.getProxyWalletAddress(),
                                    "magicWalletAddress", event.getMagicWalletAddress()
                                )
                        )
                        .flatMap(identityId -> {
                            log.info("Created Blnk identity for user: {}, identityId: {}", event.getUserId(), identityId);
                            user.setBlnkIdentityId(identityId);
                            return userRepository.save(user)
                                    .then(createUsdcBalance(identityId, user, event));
                        });
                    }
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .subscribe(
                    blnkDetails -> {
                        if (blnkDetails != null) {
                            log.info("Blnk USDC balance ready for user: {}, identityId: {}, balanceId: {}",
                                    event.getUserId(), blnkDetails.identityId, blnkDetails.balanceId);

                            // Publish BlnkBalanceCreatedEvent
                            BlnkBalanceCreatedEvent blnkEvent = BlnkBalanceCreatedEvent.builder()
                                    .userId(event.getUserId())
                                    .magicWalletAddress(event.getMagicWalletAddress())
                                    .proxyWalletAddress(event.getProxyWalletAddress())
                                    .enclaveUdaAddress(event.getEnclaveUdaAddress())
                                    .blnkIdentityId(blnkDetails.identityId)
                                    .blnkBalanceId(blnkDetails.balanceId)
                                    .email(event.getEmail())
                                    .magicUserId(event.getMagicUserId())
                                    .didToken(event.getDidToken())
                                    .timestamp(java.time.Instant.now())
                                    .build();

                            eventPublisher.publishEvent(blnkEvent);
                            log.info("Published BlnkBalanceCreatedEvent for user: {}", event.getUserId());
                        }
                    },
                    error -> {
                        log.error("Failed to process Blnk balance for user {}: {}",
                                event.getUserId(), error.getMessage(), error);
                    }
                );
    }

    private Mono<BlnkBalanceDetails> createUsdcBalance(String identityId, com.oregonMarkets.domain.user.model.User user, EnclaveUdaCreatedEvent event) {
        return blnkClient.createBalance("USDC")
                .flatMap(balanceId -> {
                    user.setBlnkBalanceId(balanceId);
                    user.setBlnkCreatedAt(java.time.Instant.now());
                    return userRepository.save(user)
                            .thenReturn(new BlnkBalanceDetails(identityId, balanceId));
                });
    }

    /**
     * Helper class to hold Blnk identity and balance IDs
     */
    private static class BlnkBalanceDetails {
        final String identityId;
        final String balanceId;

        BlnkBalanceDetails(String identityId, String balanceId) {
            this.identityId = identityId;
            this.balanceId = balanceId;
        }
    }
}
