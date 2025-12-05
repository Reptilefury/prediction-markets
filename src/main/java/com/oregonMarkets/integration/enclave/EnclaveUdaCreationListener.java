package com.oregonMarkets.integration.enclave;

import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.EnclaveUdaCreatedEvent;
import com.oregonMarkets.event.ProxyWalletCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

/**
 * Listens for ProxyWalletCreatedEvent and asynchronously creates Enclave UDA
 * Once UDA is created, publishes EnclaveUdaCreatedEvent to trigger Blnk account creation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnclaveUdaCreationListener {

    private final EnclaveClient enclaveClient;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final com.oregonMarkets.service.QrCodeAvatarService qrCodeAvatarService;

    @Value("${app.enclave.destination-token-address:}")
    private String destinationTokenAddress;

    @EventListener
    public void onProxyWalletCreated(ProxyWalletCreatedEvent event) {
        // Fire-and-forget pattern using async scheduling
        enclaveClient.createUDA(
                event.getUserId().toString(),
                event.getEmail(),
                event.getProxyWalletAddress(),
                destinationTokenAddress
        )
        .flatMap(udaResponse -> {
            // Update user with Enclave data
            return userRepository.findById(event.getUserId())
                    .flatMap(user -> {
                        try {
                            user.setEnclaveUdaAddress(udaResponse.getUdaAddress());
                            user.setEnclaveUdaStatus(com.oregonMarkets.domain.user.model.User.EnclaveUdaStatus.ACTIVE);
                            user.setEnclaveUdaCreatedAt(Instant.now());
                            
                            // Save deposit addresses as JSON
                            if (udaResponse.getDepositAddresses() != null) {
                                String depositAddressesJson = objectMapper.writeValueAsString(udaResponse.getDepositAddresses());
                                user.setEnclaveDepositAddresses(depositAddressesJson);
                                
                                // Generate QR codes for deposit addresses
                                user.setEvmDepositQrCodes(qrCodeAvatarService.generateDepositQrCodesJson(udaResponse.getDepositAddresses()));
                                
                                // Generate Solana QR code
                                user.setSolanaDepositQrCodeUrl(qrCodeAvatarService.generateSolanaDepositQrCode(depositAddressesJson));
                                
                                // Generate Bitcoin QR codes
                                user.setBitcoinDepositQrCodes(qrCodeAvatarService.generateBitcoinDepositQrCodes(depositAddressesJson));
                            }
                            
                            // Generate avatar and QR codes
                            if (user.getAvatarUrl() == null) {
                                user.setAvatarUrl(qrCodeAvatarService.generateAvatarUrl(user.getId().toString()));
                            }
                            
                            if (user.getEnclaveUdaQrCodeUrl() == null) {
                                user.setEnclaveUdaQrCodeUrl(qrCodeAvatarService.generateUdaQrCode(udaResponse.getUdaAddress()));
                            }
                            
                            if (user.getProxyWalletQrCodeUrl() == null && user.getProxyWalletAddress() != null) {
                                user.setProxyWalletQrCodeUrl(qrCodeAvatarService.generateProxyWalletQrCode(user.getProxyWalletAddress()));
                            }
                            
                            return userRepository.save(user);
                        } catch (Exception e) {
                            log.error("Failed to serialize deposit addresses for user {}: {}", event.getUserId(), e.getMessage());
                            return userRepository.save(user);
                        }
                    })
                    .thenReturn(udaResponse);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            udaResponse -> {
                log.info("Enclave UDA created successfully for user: {}, udaAddress: {}",
                        event.getUserId(), udaResponse.getUdaAddress());

                // Publish EnclaveUdaCreatedEvent to trigger Blnk account creation
                EnclaveUdaCreatedEvent udaCreatedEvent = EnclaveUdaCreatedEvent.builder()
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
                log.error("Failed to create Enclave UDA for user {}: {}",
                        event.getUserId(), error.getMessage(), error);
                // In production, you might want to publish a failure event or send alerts
            }
        );
    }
}
