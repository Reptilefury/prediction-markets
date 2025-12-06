package com.oregonMarkets.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.AssetsGenerationEvent;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AssetsGenerationListenerTest {

  @Mock private AvatarGenerationService avatarService;

  @Mock private QRCodeGenerationService qrCodeService;

  @Mock private UserRepository userRepository;

  private AssetsGenerationListener listener;

  @BeforeEach
  void setUp() {
    listener = new AssetsGenerationListener(avatarService, qrCodeService, userRepository);
  }

  @Test
  void onAssetsGenerationRequested_Success() {
    UUID userId = UUID.randomUUID();
    AssetsGenerationEvent event =
        AssetsGenerationEvent.builder()
            .userId(userId)
            .proxyWalletAddress("proxy-wallet-address")
            .enclaveUdaAddress("enclave-uda-address")
            .depositAddresses(Map.of("1", Map.of("address", "eth-address")))
            .build();

    when(avatarService.generateAndUploadAvatar(userId)).thenReturn(Mono.just("avatar-url"));
    when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "qr-url")));

    listener.onAssetsGenerationRequested(event);

    // Test completes without error
  }

  @Test
  void onAssetsGenerationRequested_AvatarGenerationFails() {
    UUID userId = UUID.randomUUID();
    AssetsGenerationEvent event =
        AssetsGenerationEvent.builder()
            .userId(userId)
            .proxyWalletAddress("proxy-wallet-address")
            .enclaveUdaAddress("enclave-uda-address")
            .depositAddresses(Map.of())
            .build();

    when(avatarService.generateAndUploadAvatar(userId))
        .thenReturn(Mono.error(new RuntimeException("Avatar failed")));
    when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(Map.of()));

    listener.onAssetsGenerationRequested(event);

    // Test completes without error despite avatar failure
  }
}
