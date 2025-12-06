package com.oregonMarkets.integration.enclave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.EnclaveUdaCreatedEvent;
import com.oregonMarkets.event.ProxyWalletCreatedEvent;
import com.oregonMarkets.service.AvatarGenerationService;
import com.oregonMarkets.service.QRCodeGenerationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

/**
 * Listens for ProxyWalletCreatedEvent and asynchronously creates Enclave UDA Once UDA is created,
 * publishes EnclaveUdaCreatedEvent to trigger Blnk account creation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnclaveUdaCreationListener {

  private static final String ADDRESS_KEY = "address";

  private final EnclaveClient enclaveClient;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final AvatarGenerationService avatarGenerationService;
  private final QRCodeGenerationService qrCodeGenerationService;

  @Value("${app.enclave.destination-token-address:}")
  private String destinationTokenAddress;

  @EventListener
  public void onProxyWalletCreated(ProxyWalletCreatedEvent event) {
    // Fire-and-forget pattern using async scheduling
    enclaveClient
        .createUDA(
            event.getUserId().toString(),
            event.getEmail(),
            event.getProxyWalletAddress(),
            destinationTokenAddress)
        .flatMap(
            udaResponse -> {
              // Update user with Enclave data
              return userRepository
                  .findById(event.getUserId())
                  .flatMap(
                      user -> {
                        try {
                          user.setEnclaveUdaAddress(udaResponse.getUdaAddress());
                          user.setEnclaveUdaStatus(
                              com.oregonMarkets.domain.user.model.User.EnclaveUdaStatus.ACTIVE);
                          user.setEnclaveUdaCreatedAt(Instant.now());

                          // Save deposit addresses as JSON
                          if (udaResponse.getDepositAddresses() != null) {
                            String depositAddressesJson =
                                objectMapper.writeValueAsString(udaResponse.getDepositAddresses());
                            user.setEnclaveDepositAddresses(depositAddressesJson);
                          }

                          return userRepository
                              .save(user)
                              .flatMap(
                                  savedUser ->
                                      generateAssetsAsync(
                                          savedUser, udaResponse.getDepositAddresses()));
                        } catch (Exception e) {
                          log.error(
                              "Failed to serialize deposit addresses for user {}: {}",
                              event.getUserId(),
                              e.getMessage());
                          return userRepository
                              .save(user)
                              .flatMap(savedUser -> generateAssetsAsync(savedUser, null));
                        }
                      })
                  .thenReturn(udaResponse);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            udaResponse -> {
              log.info(
                  "Enclave UDA created successfully for user: {}, udaAddress: {}",
                  event.getUserId(),
                  udaResponse.getUdaAddress());

              // Publish EnclaveUdaCreatedEvent to trigger Blnk account creation
              EnclaveUdaCreatedEvent udaCreatedEvent =
                  EnclaveUdaCreatedEvent.builder()
                      .userId(event.getUserId())
                      .magicWalletAddress(event.getMagicWalletAddress())
                      .proxyWalletAddress(event.getProxyWalletAddress())
                      .enclaveUdaAddress(udaResponse.getUdaAddress())
                      .email(event.getEmail())
                      .magicUserId(event.getMagicUserId())
                      .didToken(event.getDidToken())
                      .timestamp(Instant.now())
                      .build();

              eventPublisher.publishEvent(udaCreatedEvent);
              log.info("Published EnclaveUdaCreatedEvent for user: {}", event.getUserId());
            },
            error -> {
              log.error(
                  "Failed to create Enclave UDA for user {}: {}",
                  event.getUserId(),
                  error.getMessage(),
                  error);
              // In production, you might want to publish a failure event or send alerts
            });
  }

  private reactor.core.publisher.Mono<com.oregonMarkets.domain.user.model.User> generateAssetsAsync(
      com.oregonMarkets.domain.user.model.User user,
      java.util.Map<String, Object> depositAddresses) {
    // Generate avatar and QR codes asynchronously and upload to GCS
    reactor.core.publisher.Mono<String> avatarMono =
        avatarGenerationService
            .generateAndUploadAvatar(user.getId())
            .onErrorResume(
                e -> {
                  log.warn(
                      "Avatar generation failed for user {}: {}", user.getId(), e.getMessage());
                  return reactor.core.publisher.Mono.just("");
                });

    reactor.core.publisher.Mono<java.util.Map<String, String>> qrCodesMono =
        qrCodeGenerationService
            .generateAndUploadQRCodes(
                user.getId(),
                user.getProxyWalletAddress(),
                user.getEnclaveUdaAddress(),
                extractEvmAddresses(depositAddresses),
                extractSolanaAddress(depositAddresses),
                extractBitcoinAddresses(depositAddresses))
            .onErrorResume(
                e -> {
                  log.warn(
                      "QR code generation failed for user {}: {}", user.getId(), e.getMessage());
                  return reactor.core.publisher.Mono.just(java.util.Map.of());
                });

    // Execute in parallel and update user
    return reactor.core.publisher.Mono.zip(avatarMono, qrCodesMono)
        .flatMap(
            tuple -> {
              String avatarUrl = tuple.getT1();
              java.util.Map<String, String> qrCodes = tuple.getT2();

              // Use the user passed in instead of fetching again
              if (!avatarUrl.isEmpty()) user.setAvatarUrl(avatarUrl);
              if (qrCodes.containsKey("proxyWalletQrCode"))
                user.setProxyWalletQrCodeUrl(qrCodes.get("proxyWalletQrCode"));
              if (qrCodes.containsKey("enclaveUdaQrCode"))
                user.setEnclaveUdaQrCodeUrl(qrCodes.get("enclaveUdaQrCode"));
              if (qrCodes.containsKey("evmDepositQrCodes"))
                user.setEvmDepositQrCodes(qrCodes.get("evmDepositQrCodes"));
              if (qrCodes.containsKey("solanaDepositQrCode"))
                user.setSolanaDepositQrCodeUrl(qrCodes.get("solanaDepositQrCode"));
              if (qrCodes.containsKey("bitcoinDepositQrCodes"))
                user.setBitcoinDepositQrCodes(qrCodes.get("bitcoinDepositQrCodes"));
              return userRepository.save(user);
            })
        .doOnSuccess(u -> log.info("Assets generated and uploaded for user: {}", user.getId()))
        .doOnError(
            error ->
                log.error(
                    "Assets generation failed for user {}: {}", user.getId(), error.getMessage()))
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  @SuppressWarnings("unchecked")
  private java.util.Map<String, String> extractEvmAddresses(
      java.util.Map<String, Object> depositAddresses) {
    if (depositAddresses == null) return java.util.Collections.emptyMap();
    java.util.Map<String, String> evmAddresses = new java.util.HashMap<>();

    // Extract addresses by chain ID
    for (String chainId : java.util.Arrays.asList("1", "137", "8453")) {
      Object chainData = depositAddresses.get(chainId);
      if (chainData instanceof java.util.Map) {
        Object address = ((java.util.Map<String, Object>) chainData).get(ADDRESS_KEY);
        if (address != null) {
          String chainName =
              chainId.equals("1") ? "ethereum" : chainId.equals("137") ? "polygon" : "base";
          evmAddresses.put(chainName, address.toString());
        }
      }
    }
    return java.util.Collections.unmodifiableMap(evmAddresses);
  }

  @SuppressWarnings("unchecked")
  private String extractSolanaAddress(java.util.Map<String, Object> depositAddresses) {
    if (depositAddresses == null) return null;
    Object solanaData = depositAddresses.get("solana");
    if (solanaData instanceof java.util.Map) {
      Object address = ((java.util.Map<String, Object>) solanaData).get(ADDRESS_KEY);
      return address != null ? address.toString() : null;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private java.util.Map<String, String> extractBitcoinAddresses(
      java.util.Map<String, Object> depositAddresses) {
    if (depositAddresses == null) return java.util.Collections.emptyMap();
    java.util.Map<String, String> btcAddresses = new java.util.HashMap<>();
    Object btcData = depositAddresses.get("bitcoin");
    if (btcData instanceof java.util.Map) {
      Object address = ((java.util.Map<String, Object>) btcData).get(ADDRESS_KEY);
      if (address != null) {
        btcAddresses.put("bitcoin", address.toString());
      }
    }
    return btcAddresses.isEmpty() ? null : btcAddresses;
  }
}
