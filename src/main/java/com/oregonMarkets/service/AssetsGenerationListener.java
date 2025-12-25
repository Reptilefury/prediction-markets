package com.oregonmarkets.service;

import com.oregonmarkets.domain.user.repository.UserRepository;
import com.oregonmarkets.event.AssetsGenerationEvent;
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

  /**
   * Redacts sensitive data by showing only first 4 and last 4 characters
   *
   * @param sensitive The sensitive string to redact
   * @return Redacted string in format "xxxx...yyyy"
   */
  private String redact(String sensitive) {
    if (sensitive == null || sensitive.length() <= 8) {
      return "****";
    }
    return sensitive.substring(0, 4) + "..." + sensitive.substring(sensitive.length() - 4);
  }

  @EventListener
  public void onAssetsGenerationRequested(AssetsGenerationEvent event) {
    log.info("[ASSETS-GEN] Starting async assets generation for user: {}", event.getUserId());
    log.info(
        "[ASSETS-GEN] Event details - proxyWallet: {}, enclaveUda: {}",
        event.getProxyWalletAddress(),
        event.getEnclaveUdaAddress());
    log.debug("[ASSETS-GEN] Raw deposit addresses: {}", event.getDepositAddresses());

    // Extract addresses with detailed logging
    Map<String, String> evmAddresses = extractEvmAddresses(event.getDepositAddresses());
    String solanaAddress = extractSolanaAddress(event.getDepositAddresses());
    Map<String, String> bitcoinAddresses = extractBitcoinAddresses(event.getDepositAddresses());

    log.info(
        "[ASSETS-GEN] Extracted addresses - EVM: {}, Solana: {}, Bitcoin: {}",
        evmAddresses != null ? evmAddresses.size() + " chains" : "none",
        solanaAddress != null ? "present" : "none",
        bitcoinAddresses != null ? bitcoinAddresses.size() + " formats" : "none");

    if (evmAddresses != null) {
      evmAddresses.forEach(
          (network, address) -> log.debug("[ASSETS-GEN] EVM {}: {}", network, redact(address)));
    }
    if (solanaAddress != null) {
      log.debug("[ASSETS-GEN] Solana address: {}", redact(solanaAddress));
    }
    if (bitcoinAddresses != null) {
      bitcoinAddresses.forEach(
          (format, address) -> log.debug("[ASSETS-GEN] Bitcoin {}: {}", format, redact(address)));
    }

    // Generate avatar and QR codes in parallel
    Mono<String> avatarMono =
        avatarService
            .generateAndUploadAvatar(event.getUserId())
            .doOnNext(
                url ->
                    log.info(
                        "[ASSETS-GEN] ✓ Avatar generated for user {}: {}", event.getUserId(), url))
            .onErrorResume(
                e -> {
                  log.error(
                      "[ASSETS-GEN] ✗ Avatar generation failed for user {}: {}",
                      event.getUserId(),
                      e.getMessage(),
                      e);
                  return Mono.just(""); // Continue with empty avatar URL
                });

    Mono<Map<String, String>> qrCodesMono =
        qrCodeService
            .generateAndUploadQRCodes(
                event.getUserId(),
                event.getProxyWalletAddress(),
                event.getEnclaveUdaAddress(),
                evmAddresses,
                solanaAddress,
                bitcoinAddresses)
            .doOnNext(
                urls -> {
                  log.info(
                      "[ASSETS-GEN] ✓ QR codes generated for user {}: {} codes",
                      event.getUserId(),
                      urls.size());
                  urls.forEach(
                      (key, url) -> log.debug("[ASSETS-GEN] QR code {}: {}", key, redact(url)));
                })
            .onErrorResume(
                e -> {
                  log.error(
                      "[ASSETS-GEN] ✗ QR code generation failed for user {}: {}",
                      event.getUserId(),
                      e.getMessage(),
                      e);
                  return Mono.just(Map.of()); // Continue with empty QR codes
                });

    // Execute in parallel and update user
    Mono.zip(avatarMono, qrCodesMono)
        .flatMap(
            tuple -> {
              String avatarUrl = tuple.getT1();
              Map<String, String> qrCodes = tuple.getT2();

              log.info(
                  "[ASSETS-GEN] Both avatar and QR codes generation completed for user: {}",
                  event.getUserId());
              log.info("[ASSETS-GEN] Avatar URL: {}", avatarUrl.isEmpty() ? "none" : avatarUrl);
              log.info("[ASSETS-GEN] QR codes count: {}", qrCodes.size());

              return userRepository
                  .findById(event.getUserId())
                  .flatMap(
                      user -> {
                        log.info(
                            "[ASSETS-GEN] Updating user {} with generated assets",
                            event.getUserId());
                        int updatedFields = 0;

                        // Update user with generated assets
                        if (!avatarUrl.isEmpty()) {
                          user.setAvatarUrl(avatarUrl);
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set avatar URL");
                        }
                        if (qrCodes.containsKey("proxyWalletQrCode")) {
                          user.setProxyWalletQrCodeUrl(qrCodes.get("proxyWalletQrCode"));
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set proxy wallet QR code");
                        }
                        if (qrCodes.containsKey("enclaveUdaQrCode")) {
                          user.setEnclaveUdaQrCodeUrl(qrCodes.get("enclaveUdaQrCode"));
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set Enclave UDA QR code");
                        }
                        if (qrCodes.containsKey("evmDepositQrCodes")) {
                          user.setEvmDepositQrCodes(qrCodes.get("evmDepositQrCodes"));
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set EVM deposit QR codes");
                        }
                        if (qrCodes.containsKey("solanaDepositQrCode")) {
                          user.setSolanaDepositQrCodeUrl(qrCodes.get("solanaDepositQrCode"));
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set Solana deposit QR code");
                        }
                        if (qrCodes.containsKey("bitcoinDepositQrCodes")) {
                          user.setBitcoinDepositQrCodes(qrCodes.get("bitcoinDepositQrCodes"));
                          updatedFields++;
                          log.debug("[ASSETS-GEN] Set Bitcoin deposit QR codes");
                        }

                        log.info(
                            "[ASSETS-GEN] Saving user with {} updated asset fields", updatedFields);
                        return userRepository
                            .save(user)
                            .doOnSuccess(
                                u ->
                                    log.info("[ASSETS-GEN] ✓ User saved successfully with assets"));
                      });
            })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            user ->
                log.info(
                    "[ASSETS-GEN] ✓ Assets generation completed successfully for user: {}",
                    event.getUserId()),
            error ->
                log.error(
                    "[ASSETS-GEN] ✗ Assets generation failed for user {}: {}",
                    event.getUserId(),
                    error.getMessage(),
                    error));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractEvmAddresses(Map<String, Object> depositAddresses) {
    if (depositAddresses == null) {
      log.warn("[ASSETS-GEN] No deposit addresses provided for EVM extraction");
      return java.util.Collections.emptyMap();
    }

    Map<String, String> evmAddresses = new java.util.HashMap<>();

    // Extract from evm_deposit_address array
    Object evmData = depositAddresses.get("evm_deposit_address");
    if (evmData == null) {
      log.warn("[ASSETS-GEN] No 'evm_deposit_address' key found in deposit addresses");
      return java.util.Collections.emptyMap();
    }

    if (evmData instanceof java.util.List) {
      java.util.List<?> evmList = (java.util.List<?>) evmData;
      log.debug("[ASSETS-GEN] Processing {} EVM deposit addresses", evmList.size());

      for (Object item : evmList) {
        if (item instanceof Map) {
          Map<String, Object> evmItem = (Map<String, Object>) item;
          Object chainIdObj = evmItem.get("chainId");
          Object contractAddress = evmItem.get("contractAddress");

          if (chainIdObj != null && contractAddress != null) {
            // Validate contractAddress is not empty
            String addressStr = contractAddress.toString().trim();
            if (addressStr.isEmpty()) {
              log.warn("[ASSETS-GEN] EVM item has empty contractAddress, skipping");
              continue;
            }

            try {
              int chainId =
                  chainIdObj instanceof Number
                      ? ((Number) chainIdObj).intValue()
                      : Integer.parseInt(chainIdObj.toString());

              // Map chainId to network name
              String networkName = getNetworkName(chainId);
              if (networkName != null) {
                evmAddresses.put(networkName, addressStr);
                log.debug(
                    "[ASSETS-GEN] Mapped chainId {} to {}: {}",
                    chainId,
                    networkName,
                    redact(addressStr));
              } else {
                // Store with generic key for unknown chains instead of dropping
                String genericKey = "evm-" + chainId;
                evmAddresses.put(genericKey, addressStr);
                log.warn(
                    "[ASSETS-GEN] Unknown chainId: {}, storing with generic key: {}",
                    chainId,
                    genericKey);
              }
            } catch (NumberFormatException e) {
              log.warn(
                  "[ASSETS-GEN] Invalid chainId format '{}', skipping: {}",
                  chainIdObj,
                  e.getMessage());
            }
          } else {
            log.warn("[ASSETS-GEN] EVM item missing chainId or contractAddress: {}", evmItem);
          }
        }
      }
    } else {
      log.warn(
          "[ASSETS-GEN] 'evm_deposit_address' is not a List, found: {}",
          evmData.getClass().getSimpleName());
    }

    log.debug("[ASSETS-GEN] Extracted {} EVM addresses", evmAddresses.size());
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
    if (depositAddresses == null) {
      log.warn("[ASSETS-GEN] No deposit addresses provided for Solana extraction");
      return null;
    }

    // Extract from solana_deposit_address object
    Object solanaData = depositAddresses.get("solana_deposit_address");
    if (solanaData == null) {
      log.debug("[ASSETS-GEN] No 'solana_deposit_address' key found");
      return null;
    }

    if (solanaData instanceof Map) {
      Object address = ((Map<String, Object>) solanaData).get(ADDRESS_KEY);
      if (address != null) {
        String addressStr = address.toString().trim();

        // Validate Solana address: base58 format, length 32-64 characters
        if (addressStr.isEmpty()) {
          log.warn("[ASSETS-GEN] Solana address is empty");
          return null;
        }
        if (addressStr.length() < 32 || addressStr.length() > 64) {
          log.warn("[ASSETS-GEN] Solana address has invalid length: {}", addressStr.length());
          return null;
        }
        // Basic base58 character set validation (1-9, A-H, J-N, P-Z, a-k, m-z)
        if (!addressStr.matches("^[1-9A-HJ-NP-Za-km-z]+$")) {
          log.warn("[ASSETS-GEN] Solana address contains invalid characters");
          return null;
        }

        log.debug("[ASSETS-GEN] Extracted Solana address: {}", redact(addressStr));
        return addressStr;
      } else {
        log.warn("[ASSETS-GEN] Solana data exists but no 'address' key found");
        return null;
      }
    } else {
      log.warn(
          "[ASSETS-GEN] 'solana_deposit_address' is not a Map, found: {}",
          solanaData.getClass().getSimpleName());
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractBitcoinAddresses(Map<String, Object> depositAddresses) {
    if (depositAddresses == null) {
      log.warn("[ASSETS-GEN] No deposit addresses provided for Bitcoin extraction");
      return java.util.Collections.emptyMap();
    }

    Map<String, String> btcAddresses = new java.util.HashMap<>();

    // Extract from bitcoin_deposit_address object
    Object btcData = depositAddresses.get("bitcoin_deposit_address");
    if (btcData == null) {
      log.debug("[ASSETS-GEN] No 'bitcoin_deposit_address' key found");
      return java.util.Collections.emptyMap();
    }

    if (btcData instanceof Map) {
      Map<String, Object> btcMap = (Map<String, Object>) btcData;
      log.debug("[ASSETS-GEN] Processing Bitcoin deposit addresses");

      // Extract all Bitcoin address formats with validation
      Object legacyAddress = btcMap.get("legacy_address");
      if (legacyAddress != null && !legacyAddress.toString().trim().isEmpty()) {
        String addr = legacyAddress.toString().trim();
        btcAddresses.put("legacy", addr);
        log.debug("[ASSETS-GEN] Found Bitcoin legacy address: {}", redact(addr));
      }

      Object segwitAddress = btcMap.get("segwit_address");
      if (segwitAddress != null && !segwitAddress.toString().trim().isEmpty()) {
        String addr = segwitAddress.toString().trim();
        btcAddresses.put("segwit", addr);
        log.debug("[ASSETS-GEN] Found Bitcoin segwit address: {}", redact(addr));
      }

      Object nativeSegwitAddress = btcMap.get("native_segwit_address");
      if (nativeSegwitAddress != null && !nativeSegwitAddress.toString().trim().isEmpty()) {
        String addr = nativeSegwitAddress.toString().trim();
        btcAddresses.put("native_segwit", addr);
        log.debug("[ASSETS-GEN] Found Bitcoin native segwit address: {}", redact(addr));
      }

      Object taprootAddress = btcMap.get("taproot_address");
      if (taprootAddress != null && !taprootAddress.toString().trim().isEmpty()) {
        String addr = taprootAddress.toString().trim();
        btcAddresses.put("taproot", addr);
        log.debug("[ASSETS-GEN] Found Bitcoin taproot address: {}", redact(addr));
      }

      if (btcAddresses.isEmpty()) {
        log.warn("[ASSETS-GEN] Bitcoin deposit address object exists but no addresses found");
      }
    } else {
      log.warn(
          "[ASSETS-GEN] 'bitcoin_deposit_address' is not a Map, found: {}",
          btcData.getClass().getSimpleName());
    }

    log.debug("[ASSETS-GEN] Extracted {} Bitcoin address formats", btcAddresses.size());
    return btcAddresses.isEmpty() ? null : btcAddresses;
  }
}
