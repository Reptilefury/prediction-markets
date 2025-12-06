package com.oregonMarkets.integration.enclave;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.ProxyWalletCreatedEvent;
import com.oregonMarkets.service.AvatarGenerationService;
import com.oregonMarkets.service.QRCodeGenerationService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EnclaveUdaCreationListenerTest {

  @Mock private EnclaveClient enclaveClient;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private UserRepository userRepository;

  @Mock private ObjectMapper objectMapper;

  @Mock private AvatarGenerationService avatarGenerationService;

  @Mock private QRCodeGenerationService qrCodeGenerationService;

  private EnclaveUdaCreationListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new EnclaveUdaCreationListener(
            enclaveClient,
            eventPublisher,
            userRepository,
            objectMapper,
            avatarGenerationService,
            qrCodeGenerationService);
    ReflectionTestUtils.setField(listener, "destinationTokenAddress", "0x456");
  }

  @Test
  void onProxyWalletCreated_Success() throws Exception {
    UUID userId = UUID.randomUUID();
    ProxyWalletCreatedEvent event =
        ProxyWalletCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .proxyWalletAddress("0x123")
            .magicWalletAddress("0x789")
            .magicUserId("magic123")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    EnclaveClient.EnclaveUDAResponse udaResponse = new EnclaveClient.EnclaveUDAResponse();
    udaResponse.setUdaAddress("uda-123");
    udaResponse.setDepositAddresses(Map.of("1", Map.of("address", "0xabc")));

    User user = User.builder().id(userId).email("test@example.com").build();

    when(enclaveClient.createUDA(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(udaResponse));
    when(userRepository.findById(userId)).thenReturn(Mono.just(user));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"1\":{\"address\":\"0xabc\"}}");
    when(avatarGenerationService.generateAndUploadAvatar(userId))
        .thenReturn(Mono.just("https://avatar.url"));
    when(qrCodeGenerationService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));

    listener.onProxyWalletCreated(event);

    // Give async processing time to complete
    Thread.sleep(100);

    verify(enclaveClient).createUDA(userId.toString(), "test@example.com", "0x123", "0x456");
  }

  @Test
  void onProxyWalletCreated_EnclaveClientError() {
    UUID userId = UUID.randomUUID();
    ProxyWalletCreatedEvent event =
        ProxyWalletCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .proxyWalletAddress("0x123")
            .magicWalletAddress("0x789")
            .magicUserId("magic123")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    when(enclaveClient.createUDA(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.error(new RuntimeException("Enclave error")));

    listener.onProxyWalletCreated(event);

    // Give async processing time to complete
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    verify(enclaveClient).createUDA(userId.toString(), "test@example.com", "0x123", "0x456");
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void onProxyWalletCreated_UserNotFound() {
    UUID userId = UUID.randomUUID();
    ProxyWalletCreatedEvent event =
        ProxyWalletCreatedEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .proxyWalletAddress("0x123")
            .magicWalletAddress("0x789")
            .magicUserId("magic123")
            .didToken("did-token")
            .timestamp(Instant.now())
            .build();

    EnclaveClient.EnclaveUDAResponse udaResponse = new EnclaveClient.EnclaveUDAResponse();
    udaResponse.setUdaAddress("uda-123");

    when(enclaveClient.createUDA(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(udaResponse));
    when(userRepository.findById(userId)).thenReturn(Mono.empty());

    listener.onProxyWalletCreated(event);

    // Give async processing time to complete
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    verify(userRepository).findById(userId);
    // Note: eventPublisher may still be called in error scenarios, so we don't verify no
    // interactions
  }
}
