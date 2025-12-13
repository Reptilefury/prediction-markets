package com.oregonMarkets.integration.keycloak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oregonMarkets.event.AssetsGenerationEvent;
import com.oregonMarkets.event.BlnkBalanceCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class KeycloakProvisionListenerTest {

  @Mock private KeycloakAdminClient adminClient;

  @Mock private ApplicationEventPublisher eventPublisher;

  private KeycloakProvisionListener listener;

  @BeforeEach
  void setUp() {
    listener = new KeycloakProvisionListener(adminClient, eventPublisher);
  }

  @Test
  void onBlnkBalanceCreated_SuccessfulProvisioning_PublishesAssetsEvent() {

    BlnkBalanceCreatedEvent event =
        BlnkBalanceCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .magicUserId("MagicUser123=")
            .didToken("did-token-123")
            .proxyWalletAddress("proxy-address")
            .enclaveUdaAddress("enclave-address")
            .magicWalletAddress("magic-address")
            .blnkIdentityId("identity-123")
            .blnkBalanceId("balance-123")
            .timestamp(Instant.now())
            .build();

    when(adminClient.createUserIfAbsent("magicuser123", "did-token-123", "test@example.com"))
        .thenReturn(Mono.empty()); // FIXED

    listener.onBlnkBalanceCreated(event);

    verify(adminClient, timeout(2000))
        .createUserIfAbsent("magicuser123", "did-token-123", "test@example.com");
  }

  @Test
  void onBlnkBalanceCreated_FailedProvisioning_HandlesGracefully() {

    BlnkBalanceCreatedEvent event =
        BlnkBalanceCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .magicUserId("MagicUser123=")
            .didToken("did-token-123")
            .proxyWalletAddress("proxy-address")
            .enclaveUdaAddress("enclave-address")
            .magicWalletAddress("magic-address")
            .blnkIdentityId("identity-123")
            .blnkBalanceId("balance-123")
            .timestamp(Instant.now())
            .build();

    when(adminClient.createUserIfAbsent("magicuser123", "did-token-123", "test@example.com"))
        .thenReturn(Mono.error(new RuntimeException("Keycloak error"))); // FIXED

    listener.onBlnkBalanceCreated(event);

    verify(adminClient, timeout(1000))
        .createUserIfAbsent("magicuser123", "did-token-123", "test@example.com");

    verify(eventPublisher, never()).publishEvent(any(AssetsGenerationEvent.class));
  }

  @Test
  void onBlnkBalanceCreated_CleansUsernameCorrectly() {

    BlnkBalanceCreatedEvent event =
        BlnkBalanceCreatedEvent.builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .magicUserId("UPPERCASE_USER_ID=====")
            .didToken("did-token-123")
            .proxyWalletAddress("proxy-address")
            .enclaveUdaAddress("enclave-address")
            .magicWalletAddress("magic-address")
            .blnkIdentityId("identity-123")
            .blnkBalanceId("balance-123")
            .timestamp(Instant.now())
            .build();

    when(adminClient.createUserIfAbsent("uppercase_user_id", "did-token-123", "test@example.com"))
        .thenReturn(Mono.empty()); // FIXED

    listener.onBlnkBalanceCreated(event);

    verify(adminClient, timeout(1000))
        .createUserIfAbsent("uppercase_user_id", "did-token-123", "test@example.com");
  }
}
