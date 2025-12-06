package com.oregonMarkets.integration.blnk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.BlnkBalanceCreatedEvent;
import com.oregonMarkets.event.EnclaveUdaCreatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class BlnkBalanceCreationListenerTest {

  @Mock private BlnkClient blnkClient;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private UserRepository userRepository;

  private BlnkBalanceCreationListener listener;

  @BeforeEach
  void setUp() {
    listener = new BlnkBalanceCreationListener(blnkClient, eventPublisher, userRepository);
  }

  @Test
  void onEnclaveUdaCreated_UserWithoutBlnkIdentity_CreatesIdentityAndBalance() {
    UUID userId = UUID.randomUUID();
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .enclaveUdaAddress("enclave-address")
            .proxyWalletAddress("proxy-address")
            .magicWalletAddress("magic-address")
            .magicUserId("magic-user-id")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    User user = User.builder().id(userId).email("test@example.com").build();

    when(userRepository.findById(userId)).thenReturn(Mono.just(user));
    when(blnkClient.createIdentity(anyString(), anyString(), any(Map.class)))
        .thenReturn(Mono.just("identity-123"));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(blnkClient.createBalance("USDC")).thenReturn(Mono.just("balance-123"));

    listener.onEnclaveUdaCreated(event);

    verify(eventPublisher, timeout(1000)).publishEvent(any(BlnkBalanceCreatedEvent.class));
  }

  @Test
  void onEnclaveUdaCreated_UserWithExistingIdentityAndUsdcBalance_SkipsCreation() {
    UUID userId = UUID.randomUUID();
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .enclaveUdaAddress("enclave-address")
            .proxyWalletAddress("proxy-address")
            .magicWalletAddress("magic-address")
            .magicUserId("magic-user-id")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    User user =
        User.builder()
            .id(userId)
            .email("test@example.com")
            .blnkIdentityId("existing-identity")
            .build();

    List<Map<String, Object>> balances = List.of(Map.of("currency", "USDC", "balance", "100.00"));

    when(userRepository.findById(userId)).thenReturn(Mono.just(user));
    when(blnkClient.getBalancesByIdentity("existing-identity")).thenReturn(Mono.just(balances));

    listener.onEnclaveUdaCreated(event);

    verify(blnkClient, after(1000).never()).createBalance(anyString());
    verify(eventPublisher, after(1000).never()).publishEvent(any(BlnkBalanceCreatedEvent.class));
  }

  @Test
  void onEnclaveUdaCreated_UserWithIdentityButNoUsdcBalance_CreatesUsdcBalance() {
    UUID userId = UUID.randomUUID();
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .enclaveUdaAddress("enclave-address")
            .proxyWalletAddress("proxy-address")
            .magicWalletAddress("magic-address")
            .magicUserId("magic-user-id")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    User user =
        User.builder()
            .id(userId)
            .email("test@example.com")
            .blnkIdentityId("existing-identity")
            .build();

    List<Map<String, Object>> balances = List.of(Map.of("currency", "ETH", "balance", "1.0"));

    when(userRepository.findById(userId)).thenReturn(Mono.just(user));
    when(blnkClient.getBalancesByIdentity("existing-identity")).thenReturn(Mono.just(balances));
    when(blnkClient.createBalance("USDC")).thenReturn(Mono.just("balance-123"));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

    listener.onEnclaveUdaCreated(event);

    verify(blnkClient, timeout(2000)).createBalance("USDC");
    verify(eventPublisher, timeout(2000)).publishEvent(any(BlnkBalanceCreatedEvent.class));
  }

  @Test
  void onEnclaveUdaCreated_UserNotFound_HandlesGracefully() {
    UUID userId = UUID.randomUUID();
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .enclaveUdaAddress("enclave-address")
            .proxyWalletAddress("proxy-address")
            .magicWalletAddress("magic-address")
            .magicUserId("magic-user-id")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    listener.onEnclaveUdaCreated(event);

    verify(blnkClient, never()).createIdentity(anyString(), anyString(), any(Map.class));
    verify(eventPublisher, never()).publishEvent(any(BlnkBalanceCreatedEvent.class));
  }

  @Test
  void onEnclaveUdaCreated_BlnkClientError_HandlesGracefully() {
    UUID userId = UUID.randomUUID();
    EnclaveUdaCreatedEvent event =
        EnclaveUdaCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .enclaveUdaAddress("enclave-address")
            .proxyWalletAddress("proxy-address")
            .magicWalletAddress("magic-address")
            .magicUserId("magic-user-id")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    User user = User.builder().id(userId).email("test@example.com").build();

    when(userRepository.findById(userId)).thenReturn(Mono.just(user));
    when(blnkClient.createIdentity(anyString(), anyString(), any(Map.class)))
        .thenReturn(Mono.error(new RuntimeException("Blnk API error")));

    listener.onEnclaveUdaCreated(event);

    verify(eventPublisher, after(1000).never()).publishEvent(any(BlnkBalanceCreatedEvent.class));
  }
}
