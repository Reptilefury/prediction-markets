package com.oregonMarkets.integration.keycloak;

import com.oregonMarkets.event.AssetsGenerationEvent;
import com.oregonMarkets.event.BlnkBalanceCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakProvisionListener {

  private final KeycloakAdminClient adminClient;
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  /**
   * Listen for BlnkBalanceCreatedEvent (new async flow) This is the final step in the async
   * registration orchestration chain
   */
  @EventListener
  public void onBlnkBalanceCreated(BlnkBalanceCreatedEvent event) {
    log.info(
        "Received BlnkBalanceCreatedEvent for user {}, provisioning Keycloak user",
        event.getUserId());
    String username = event.getMagicUserId().toLowerCase().replace("=", "");
    String password = event.getDidToken();
    String email = event.getEmail();
    adminClient
        .createUserIfAbsent(username, password, email)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            v -> {
              log.info(
                  "Successfully provisioned Keycloak user {} for userId {}",
                  username,
                  event.getUserId());
              AssetsGenerationEvent assetsEvent =
                  AssetsGenerationEvent.builder()
                      .userId(event.getUserId())
                      .email(event.getEmail())
                      .proxyWalletAddress(event.getProxyWalletAddress())
                      .enclaveUdaAddress(event.getEnclaveUdaAddress())
                      .magicWalletAddress(event.getMagicWalletAddress())
                      .depositAddresses(event.getDepositAddresses())
                      .timestamp(java.time.Instant.now())
                      .build();
              eventPublisher.publishEvent(assetsEvent);
              log.info(
                  "Triggered assets generation for user: {} with deposit addresses",
                  event.getUserId());
            },
            e ->
                log.error(
                    "Failed to provision Keycloak user {} for userId {}: {}",
                    username,
                    event.getUserId(),
                    e.getMessage()));
  }
}
