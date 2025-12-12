package com.oregonMarkets.service;

import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.AssetsGenerationEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    Mono<String> avatarMono =
        avatarService
            .generateAndUploadAvatar(event.getUserId())
            .doOnSuccess(
                url -> log.info("Avatar generated for user {}: {}", event.getUserId(), url))
            .onErrorResume(
                e -> {
                  log.warn(
                      "Avatar generation failed for user {}: {}",
                      event.getUserId(),
                      e.getMessage());
                  return Mono.just(""); // Continue with empty avatar URL
                });

    Mono<Map<String, String>> qrCodesMono =
        qrCodeService
            .generateAndUploadQRCodes(
                event.getUserId(),
                event.getProxyWalletAddress(),
                event.getEnclaveUdaAddress(),
                extractEvmAddresses(event.getDepositAddresses()),
                extractSolanaAddress(event.getDepositAddresses()),
                extractBitcoinAddresses(event.getDepositAddresses()))
            .doOnSuccess(
                urls ->
                    log.info(
                        "QR codes generated for user {}: {} codes", event.getUserId(), urls.size()))
            .onErrorResume(
                e -> {
                  log.warn(
                      "QR code generation failed for user {}: {}",
                      event.getUserId(),
                      e.getMessage());
                  return Mono.just(Map.of()); // Continue with empty QR codes
                });

    // Execute in parallel and update user
    Mono.zip(avatarMono, qrCodesMono)
        .flatMap(
            tuple -> {
              String avatarUrl = tuple.getT1();
              Map<String, String> qrCodes = tuple.getT2();

              return userRepository
                  .findById(event.getUserId())
                  .flatMap(
                      user -> {
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
            error ->
                log.error(
                    "Assets generation failed for user {}: {}",
                    event.getUserId(),
                    error.getMessage()));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractEvmAddresses(Map<String, Object> depositAddresses) {
    if (depositAddresses == null) return java.util.Collections.emptyMap();

    Map<String, String> evmAddresses = new java.util.HashMap<>();

    // Extract from evm_deposit_address array
    Object evmData = depositAddresses.get("evm_deposit_address");
    if (evmData instanceof java.util.List) {
      java.util.List<?> evmList = (java.util.List<?>) evmData;
      for (Object item : evmList) {
        if (item instanceof Map) {
          Map<String, Object> evmItem = (Map<String, Object>) item;
          Object chainIdObj = evmItem.get("chainId");
          Object contractAddress = evmItem.get("contractAddress");

          if (chainIdObj != null && contractAddress != null) {
            int chainId = chainIdObj instanceof Number ?
                ((Number) chainIdObj).intValue() :
                Integer.parseInt(chainIdObj.toString());

            // Map chainId to network name
            String networkName = getNetworkName(chainId);
            if (networkName != null) {
              evmAddresses.put(networkName, contractAddress.toString());
            }
          }
        }
      }
    }

    return evmAddresses.isEmpty() ? null : evmAddresses;
  }

  private String getNetworkName(int chainId) {
    return switch (chainId) {
      case 1 -> "ethereum";
      case 137 -> "polygon";
      case 8453 -> "base";
      case 42161 -> "arbitrum";
      case 10 -> "optimism";
      case 56 -> "bsc";
      case 43114 -> "avalanche";
      case 130 -> "worldchain";
      case 146 -> "sonic";
      case 480 -> "worldchain-sepolia";
      default -> null;
    };
  }

  @SuppressWarnings("unchecked")
  private String extractSolanaAddress(Map<String, Object> depositAddresses) {
    if (depositAddresses == null) return null;

    // Extract from solana_deposit_address object
    Object solanaData = depositAddresses.get("solana_deposit_address");
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

    // Extract from bitcoin_deposit_address object
    Object btcData = depositAddresses.get("bitcoin_deposit_address");
    if (btcData instanceof Map) {
      Map<String, Object> btcMap = (Map<String, Object>) btcData;

      // Extract all Bitcoin address formats
      Object legacyAddress = btcMap.get("legacy_address");
      if (legacyAddress != null) {
        btcAddresses.put("legacy", legacyAddress.toString());
      }

      Object segwitAddress = btcMap.get("segwit_address");
      if (segwitAddress != null) {
        btcAddresses.put("segwit", segwitAddress.toString());
      }

      Object nativeSegwitAddress = btcMap.get("native_segwit_address");
      if (nativeSegwitAddress != null) {
        btcAddresses.put("native_segwit", nativeSegwitAddress.toString());
      }

      Object taprootAddress = btcMap.get("taproot_address");
      if (taprootAddress != null) {
        btcAddresses.put("taproot", taprootAddress.toString());
      }
    }

    return btcAddresses.isEmpty() ? null : btcAddresses;
  }
}
