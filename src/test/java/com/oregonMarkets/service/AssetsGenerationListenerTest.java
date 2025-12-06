package com.oregonMarkets.service;

import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.AssetsGenerationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetsGenerationListenerTest {

    @Mock
    private AvatarGenerationService avatarService;

    @Mock
    private QRCodeGenerationService qrCodeService;

    @Mock
    private UserRepository userRepository;

    private AssetsGenerationListener listener;

    @BeforeEach
    void setUp() {
        listener = new AssetsGenerationListener(avatarService, qrCodeService, userRepository);
    }

    @Test
    void onAssetsGenerationRequested_Success() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .timestamp(Instant.now())
                .build();

        when(avatarService.generateAndUploadAvatar(userId))
                .thenReturn(Mono.just("https://avatar.url"));
        when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(avatarService, timeout(2000)).generateAndUploadAvatar(userId);
        verify(qrCodeService, timeout(2000)).generateAndUploadQRCodes(any(), any(), any(), any(), any(), any());
    }

    @Test
    void onAssetsGenerationRequested_AvatarFails_ContinuesWithQRCodes() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .timestamp(Instant.now())
                .build();

        when(avatarService.generateAndUploadAvatar(userId))
                .thenReturn(Mono.error(new RuntimeException("Avatar generation failed")));
        when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(qrCodeService, timeout(2000)).generateAndUploadQRCodes(any(), any(), any(), any(), any(), any());
    }

    @Test
    void onAssetsGenerationRequested_QRCodesFail_ContinuesWithAvatar() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .timestamp(Instant.now())
                .build();

        when(avatarService.generateAndUploadAvatar(userId))
                .thenReturn(Mono.just("https://avatar.url"));
        when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("QR generation failed")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(avatarService, timeout(2000)).generateAndUploadAvatar(userId);
    }

    @Test
    void onAssetsGenerationRequested_WithDepositAddresses() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Map<String, Object> depositAddresses = Map.of(
                "1", Map.of("address", "0xeth"),
                "137", Map.of("address", "0xpolygon"),
                "8453", Map.of("address", "0xbase"),
                "solana", Map.of("address", "solana123"),
                "bitcoin", Map.of("address", "bc1bitcoin")
        );

        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .depositAddresses(depositAddresses)
                .timestamp(Instant.now())
                .build();

        when(avatarService.generateAndUploadAvatar(userId))
                .thenReturn(Mono.just("https://avatar.url"));
        when(qrCodeService.generateAndUploadQRCodes(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(Map.of(
                        "proxyWalletQrCode", "https://proxy.qr",
                        "enclaveUdaQrCode", "https://enclave.qr",
                        "evmDepositQrCodes", "https://evm.qr",
                        "solanaDepositQrCode", "https://solana.qr",
                        "bitcoinDepositQrCodes", "https://btc.qr"
                )));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(userRepository, timeout(2000)).save(any(User.class));
    }
}
