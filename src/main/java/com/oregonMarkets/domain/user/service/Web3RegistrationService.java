package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.common.exception.Web3AuthException;
import com.oregonMarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.UserRegisteredEvent;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.web3.Web3AuthService;
import com.oregonMarkets.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class Web3RegistrationService {
    
    private final UserRepository userRepository;
    private final Web3AuthService web3AuthService;
    private final EnclaveClient enclaveClient;
    private final CacheService cacheService;

    public Mono<UserRegistrationResponse> registerUser(Web3RegistrationRequest request) {
        log.info("Starting Web3 user registration for wallet: {}", request.getWalletAddress());
        
        return web3AuthService.verifySignature(
                request.getWalletAddress(), 
                request.getMessage(), 
                request.getSignature()
            )
            .flatMap(isValid -> {
                if (!isValid) {
                    return Mono.error(new Web3AuthException("Invalid wallet signature"));
                }
                return checkWalletExists(request.getWalletAddress())
                    .then(createWeb3User(request))
                    .flatMap(user -> setupEnclaveUDA(user)
                        .then(userRepository.save(user))
                        .flatMap(savedUser -> publishUserRegisteredEvent(savedUser)
                            .then(cacheUserData(savedUser))
                            .thenReturn(buildResponse(savedUser))
                        )
                    );
            })
            .doOnSuccess(response -> log.info("Successfully registered Web3 user: {}", response.getUserId()))
            .doOnError(error -> log.error("Failed to register Web3 user: {}", error.getMessage()));
    }
    
    private Mono<Void> checkWalletExists(String walletAddress) {
        return userRepository.existsByWeb3WalletAddress(walletAddress)
            .flatMap(exists -> exists ? 
                Mono.error(new UserAlreadyExistsException("wallet address", walletAddress)) : 
                Mono.empty());
    }
    
    private Mono<User> createWeb3User(Web3RegistrationRequest request) {
        User user = User.builder()
            .id(UUID.randomUUID())
            .web3WalletAddress(request.getWalletAddress())
            .authMethod(User.AuthMethod.WEB3_WALLET)
            .walletVerifiedAt(Instant.now())
            .countryCode(request.getCountryCode())
            .kycStatus(User.KycStatus.NOT_STARTED)
            .kycLevel(0)
            .enclaveUdaStatus(User.EnclaveUdaStatus.PENDING)
            .referralCode(generateReferralCode())
            .utmSource(request.getUtmSource())
            .utmMedium(request.getUtmMedium())
            .utmCampaign(request.getUtmCampaign())
            .build();
        
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            return userRepository.findByReferralCode(request.getReferralCode())
                .map(referrer -> {
                    user.setReferredByUserId(referrer.getId());
                    return user;
                })
                .switchIfEmpty(Mono.just(user));
        }
        
        return Mono.just(user);
    }
    
    private Mono<User> setupEnclaveUDA(User user) {
        return enclaveClient.createUDA(
                user.getId().toString(),
                user.getWeb3WalletAddress(), // Use wallet as identifier
                user.getWeb3WalletAddress()
            )
            .map(udaResponse -> {
                user.setEnclaveUserId(udaResponse.getUserId());
                user.setEnclaveUdaAddress(udaResponse.getUdaAddress());
                user.setEnclaveUdaTag(udaResponse.getTag());
                user.setEnclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE);
                user.setEnclaveUdaCreatedAt(udaResponse.getCreatedAt());
                return user;
            })
            .onErrorReturn(user);
    }
    
    private UserRegistrationResponse buildResponse(User user) {
        return UserRegistrationResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .magicWalletAddress(user.getWeb3WalletAddress()) // Use Web3 wallet
            .enclaveUdaAddress(user.getEnclaveUdaAddress())
            .referralCode(user.getReferralCode())
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .createdAt(user.getCreatedAt())
            .build();
    }
    
    private Mono<Void> publishUserRegisteredEvent(User user) {
        return Mono.fromRunnable(() -> {
            UserRegisteredEvent event = UserRegisteredEvent.from(
                user.getId(),
                user.getEmail(),
                user.getWeb3WalletAddress(),
                user.getEnclaveUdaAddress(),
                user.getReferralCode(),
                user.getReferredByUserId()
            );
           // eventPublisher.publishEvent("user.registered", user.getId().toString(), event);
        });
    }
    
    private Mono<Void> cacheUserData(User user) {
        String cacheKey = "user:" + user.getId();
        return cacheService.set(cacheKey, user, java.time.Duration.ofHours(1))
            .onErrorResume(error -> {
                log.warn("Failed to cache user data: {}", error.getMessage());
                return Mono.empty();
            });
    }
    
    private String generateReferralCode() {
        return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}