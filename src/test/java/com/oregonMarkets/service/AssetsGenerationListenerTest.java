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

        // Use the actual Enclave deposit address format
        Map<String, Object> depositAddresses = Map.of(
                "evm_deposit_address", java.util.List.of(
                        Map.of("chainId", 1, "contractAddress", "0xeth"),
                        Map.of("chainId", 137, "contractAddress", "0xpolygon"),
                        Map.of("chainId", 8453, "contractAddress", "0xbase")
                ),
                "solana_deposit_address", Map.of("address", "solana123"),
                "bitcoin_deposit_address", Map.of(
                        "legacy_address", "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
                        "segwit_address", "3J98t1WpEZ73CNmYviecrnyiWrnqRhWNLy",
                        "native_segwit_address", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
                        "taproot_address", "bc1p5d7rjq7g6rdk2yhzks9smlaqtedr4dekq08ge8ztwac72sfr9rusxg3297"
                )
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

    @Test
    void onAssetsGenerationRequested_WithNullDepositAddresses() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .depositAddresses(null)
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

        verify(userRepository, timeout(2000)).save(any(User.class));
    }

    @Test
    void onAssetsGenerationRequested_WithMalformedEVMAddresses() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        // Test with malformed EVM address structure
        Map<String, Object> depositAddresses = Map.of(
                "evm_deposit_address", "not-a-list", // Wrong type
                "solana_deposit_address", Map.of("address", "solana123")
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
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(userRepository, timeout(2000)).save(any(User.class));
    }

    @Test
    void onAssetsGenerationRequested_WithUnknownChainId() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        // Test with unknown chainId
        Map<String, Object> depositAddresses = Map.of(
                "evm_deposit_address", java.util.List.of(
                        Map.of("chainId", 999999, "contractAddress", "0xunknown")
                )
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
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(userRepository, timeout(2000)).save(any(User.class));
    }

    @Test
    void onAssetsGenerationRequested_WithMissingSolanaAddress() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        // Solana data without 'address' key
        Map<String, Object> depositAddresses = Map.of(
                "solana_deposit_address", Map.of("wrong_key", "solana123")
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
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(userRepository, timeout(2000)).save(any(User.class));
    }

    @Test
    void onAssetsGenerationRequested_WithEmptyBitcoinAddresses() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        // Bitcoin data without any address keys
        Map<String, Object> depositAddresses = Map.of(
                "bitcoin_deposit_address", Map.of("unrelated_key", "value")
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
                .thenReturn(Mono.just(Map.of("proxyWalletQrCode", "https://qr.url")));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        listener.onAssetsGenerationRequested(event);

        Thread.sleep(500);

        verify(userRepository, timeout(2000)).save(any(User.class));
    }
}
