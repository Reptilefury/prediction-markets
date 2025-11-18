package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.UserRegisteredEvent;
import com.oregonMarkets.integration.blnk.BlnkClient;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.magic.MagicClient;
import com.oregonMarkets.service.CacheService;
import com.oregonMarkets.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {
    
    private final UserRepository userRepository;
    private final MagicClient magicClient;
    private final EnclaveClient enclaveClient;
    private final BlnkClient blnkClient;
    private final CacheService cacheService;
    private final EventPublisher eventPublisher;
    
    public Mono<UserRegistrationResponse> registerUser(UserRegistrationRequest request) {
        log.info("Starting user registration for email: {}", request.getEmail());
        
        return magicClient.validateDIDToken(request.getDidToken())
            .flatMap(magicUser -> checkUserExists(magicUser)
                .then(createUser(magicUser, request))
                .flatMap(user -> setupExternalIntegrations(user)
                    .then(userRepository.save(user))
                    .flatMap(savedUser -> publishUserRegisteredEvent(savedUser)
                        .then(cacheUserData(savedUser))
                        .thenReturn(buildResponse(savedUser))
                    )
                )
            )
            .doOnSuccess(response -> log.info("Successfully registered user: {}", response.getUserId()))
            .doOnError(error -> log.error("Failed to register user: {}", error.getMessage()));
    }
    
    private Mono<User> createUser(MagicClient.MagicUserInfo magicUser, UserRegistrationRequest request) {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(magicUser.getEmail())
            .magicUserId(magicUser.getIssuer())
            .magicWalletAddress(magicUser.getPublicAddress())
            .magicIssuer(magicUser.getIssuer())
            .emailVerified(true)
            .emailVerifiedAt(Instant.now())
            .countryCode(request.getCountryCode())
            .kycStatus(User.KycStatus.NOT_STARTED)
            .kycLevel(0)
            .enclaveUdaStatus(User.EnclaveUdaStatus.PENDING)
            .referralCode(generateReferralCode())
            .utmSource(request.getUtmSource())
            .utmMedium(request.getUtmMedium())
            .utmCampaign(request.getUtmCampaign())
            .build();
        
        Mono<User> userMono = Mono.just(user);
        
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            userMono = userRepository.findByReferralCode(request.getReferralCode())
                .map(referrer -> {
                    user.setReferredByUserId(referrer.getId());
                    return user;
                })
                .switchIfEmpty(Mono.just(user));
        }
        
        return userMono;
    }
    
    private Mono<Void> checkUserExists(MagicClient.MagicUserInfo magicUser) {
        return userRepository.existsByEmail(magicUser.getEmail())
            .flatMap(exists -> exists ? 
                Mono.error(new UserAlreadyExistsException(magicUser.getEmail())) : 
                Mono.empty())
            .then(userRepository.existsByMagicUserId(magicUser.getIssuer())
                .flatMap(exists -> exists ? 
                    Mono.error(new UserAlreadyExistsException("Magic ID", magicUser.getIssuer())) : 
                    Mono.empty()));
    }
    
    private Mono<User> setupExternalIntegrations(User user) {
        return enclaveClient.createUDA(
                user.getId().toString(),
                user.getEmail(),
                user.getMagicWalletAddress()
            )
            .map(udaResponse -> {
                user.setEnclaveUserId(udaResponse.getUserId());
                user.setEnclaveUdaAddress(udaResponse.getUdaAddress());
                user.setEnclaveUdaTag(udaResponse.getTag());
                user.setEnclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE);
                user.setEnclaveUdaCreatedAt(udaResponse.getCreatedAt());
                return user;
            })
            .onErrorReturn(user)
            .flatMap(u -> blnkClient.createIdentity(
                    u.getId().toString(),
                    u.getEmail(),
                    Map.of(
                        "username", u.getUsername() != null ? u.getUsername() : "user_" + u.getId(),
                        "magic_wallet", u.getMagicWalletAddress()
                    )
                )
                .flatMap(identityId -> blnkClient.createAccount(
                        identityId,
                        "USDC",
                        "Oregon Markets Balance - " + u.getEmail()
                    )
                    .map(accountId -> {
                        u.setBlnkIdentityId(identityId);
                        u.setBlnkAccountId(accountId);
                        u.setBlnkCreatedAt(Instant.now());
                        return u;
                    })
                )
                .onErrorReturn(u)
            );
    }
    
    private UserRegistrationResponse buildResponse(User user) {
        return UserRegistrationResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .magicWalletAddress(user.getMagicWalletAddress())
            .enclaveUdaAddress(user.getEnclaveUdaAddress())
            .referralCode(user.getReferralCode())
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .createdAt(user.getCreatedAt())
            .build();
    }
    
    private String generateReferralCode() {
        return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private Mono<Void> publishUserRegisteredEvent(User user) {
        return Mono.fromRunnable(() -> {
            UserRegisteredEvent event = UserRegisteredEvent.from(
                user.getId(),
                user.getEmail(),
                user.getMagicWalletAddress(),
                user.getEnclaveUdaAddress(),
                user.getReferralCode(),
                user.getReferredByUserId()
            );
            eventPublisher.publishEvent("user.registered", user.getId().toString(), event);
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
}