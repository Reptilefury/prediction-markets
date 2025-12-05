package com.oregonMarkets.service;

import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.AssetsGenerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetsGenerationListener {

    private static final String ADDRESS_KEY = "address";

    private final AvatarGenerationService avatarService;
    private final QRCodeGenerationService qrCodeService;
    private final UserRepository userRepository;

    @EventListener
    public void onAssetsGenerationRequested(AssetsGenerationEvent event) {
        log.info("Starting async assets generation for user: {}", event.getUserId());
        
        // Generate avatar and QR codes in parallel
        Mono<String> avatarMono = avatarService.generateAndUploadAvatar(event.getUserId())
                .doOnSuccess(url -> log.info("Avatar generated for user {}: {}", event.getUserId(), url))
                .onErrorResume(e -> {
                    log.warn("Avatar generation failed for user {}: {}", event.getUserId(), e.getMessage());
                    return Mono.just(""); // Continue with empty avatar URL
                });

        Mono<Map<String, String>> qrCodesMono = qrCodeService.generateAndUploadQRCodes(
                event.getUserId(),
                event.getProxyWalletAddress(),
                event.getEnclaveUdaAddress(),
                extractEvmAddresses(event.getDepositAddresses()),
                extractSolanaAddress(event.getDepositAddresses()),
                extractBitcoinAddresses(event.getDepositAddresses())
        )
                .doOnSuccess(urls -> log.info("QR codes generated for user {}: {} codes", event.getUserId(), urls.size()))
                .onErrorResume(e -> {
                    log.warn("QR code generation failed for user {}: {}", event.getUserId(), e.getMessage());
                    return Mono.just(Map.of()); // Continue with empty QR codes
                });

        // Execute in parallel and update user
        Mono.zip(avatarMono, qrCodesMono)
                .flatMap(tuple -> {
                    String avatarUrl = tuple.getT1();
                    Map<String, String> qrCodes = tuple.getT2();
                    
                    return userRepository.findById(event.getUserId())
                            .flatMap(user -> {
                                // Update user with generated assets
                                if (!avatarUrl.isEmpty()) {
                                    user.setAvatarUrl(avatarUrl);
                                }
                                if (qrCodes.containsKey("proxyWalletQrCode")) {
                                    user.setProxyWalletQrCodeUrl(qrCodes.get("proxyWalletQrCode"));
                                }
                                if (qrCodes.containsKey("enclaveUdaQrCode")) {
                                    user.setEnclaveUdaQrCodeUrl(qrCodes.get("enclaveUdaQrCode"));
                                }
                                if (qrCodes.containsKey("evmDepositQrCodes")) {
                                    user.setEvmDepositQrCodes(qrCodes.get("evmDepositQrCodes"));
                                }
                                if (qrCodes.containsKey("solanaDepositQrCode")) {
                                    user.setSolanaDepositQrCodeUrl(qrCodes.get("solanaDepositQrCode"));
                                }
                                if (qrCodes.containsKey("bitcoinDepositQrCodes")) {
                                    user.setBitcoinDepositQrCodes(qrCodes.get("bitcoinDepositQrCodes"));
                                }
                                return userRepository.save(user);
                            });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    user -> log.info("Assets generation completed for user: {}", event.getUserId()),
                    error -> log.error("Assets generation failed for user {}: {}", event.getUserId(), error.getMessage())
                );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractEvmAddresses(Map<String, Object> depositAddresses) {
        if (depositAddresses == null) return java.util.Collections.emptyMap();
        
        Map<String, String> evmAddresses = new java.util.HashMap<>();
        
        // Extract Ethereum addresses (chainId 1)
        Object ethData = depositAddresses.get("1");
        if (ethData instanceof Map) {
            Object ethAddress = ((Map<String, Object>) ethData).get(ADDRESS_KEY);
            if (ethAddress != null) {
                evmAddresses.put("ethereum", ethAddress.toString());
            }
        }
        
        // Extract Polygon addresses (chainId 137)
        Object polygonData = depositAddresses.get("137");
        if (polygonData instanceof Map) {
            Object polygonAddress = ((Map<String, Object>) polygonData).get(ADDRESS_KEY);
            if (polygonAddress != null) {
                evmAddresses.put("polygon", polygonAddress.toString());
            }
        }
        
        // Extract Base addresses (chainId 8453)
        Object baseData = depositAddresses.get("8453");
        if (baseData instanceof Map) {
            Object baseAddress = ((Map<String, Object>) baseData).get(ADDRESS_KEY);
            if (baseAddress != null) {
                evmAddresses.put("base", baseAddress.toString());
            }
        }
        
        return evmAddresses.isEmpty() ? null : evmAddresses;
    }
    
    @SuppressWarnings("unchecked")
    private String extractSolanaAddress(Map<String, Object> depositAddresses) {
        if (depositAddresses == null) return null;
        
        // Solana typically uses a different key format
        Object solanaData = depositAddresses.get("solana");
        if (solanaData instanceof Map) {
            Object address = ((Map<String, Object>) solanaData).get(ADDRESS_KEY);
            return address != null ? address.toString() : null;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> extractBitcoinAddresses(Map<String, Object> depositAddresses) {
        if (depositAddresses == null) return java.util.Collections.emptyMap();
        
        Map<String, String> btcAddresses = new java.util.HashMap<>();
        
        // Bitcoin mainnet
        Object btcData = depositAddresses.get("bitcoin");
        if (btcData instanceof Map) {
            Object btcAddress = ((Map<String, Object>) btcData).get(ADDRESS_KEY);
            if (btcAddress != null) {
                btcAddresses.put("bitcoin", btcAddress.toString());
            }
        }
        
        return btcAddresses.isEmpty() ? null : btcAddresses;
    }
}