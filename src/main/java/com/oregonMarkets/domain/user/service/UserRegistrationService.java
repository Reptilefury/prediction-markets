package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.ProxyWalletCreatedEvent;
import com.oregonMarkets.event.UserRegisteredEvent;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonMarkets.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final ProxyWalletOnboardingService proxyWalletService;
    private final CacheService cacheService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * Deprecated path: registration should no longer receive DID token in body.
     * Kept for tests/mocks compatibility.
     */
    @Deprecated
    public Mono<UserRegistrationResponse> registerUser(@Valid UserRegistrationRequest request) {
        return Mono.error(new IllegalStateException("Deprecated registration path; DID token must come from Magic header"));
    }

    public Mono<UserRegistrationResponse> registerUser(@Valid UserRegistrationRequest request,
                                                       MagicDIDValidator.MagicUserInfo magicUser,
                                                       String didToken) {
        log.info("Starting user registration for email: {}", request.getEmail());
        return checkUserExists(magicUser, request)
                .then(createUser(magicUser, request))
                .flatMap(userRepository::save)  // Save first to generate UUID
                .flatMap(user -> setupExternalIntegrationsAsyncViawEvents(user, magicUser.getUserId(), didToken)  // Setup proxy wallet and publish event for async chain
                        .flatMap(savedUser -> publishUserRegisteredEvent(savedUser)
                                .then(cacheUserData(savedUser))
                                .thenReturn(buildResponse(savedUser))  // Return immediately, rest happens async
                        )
                )
                .doOnSuccess(response -> log.info("Successfully registered user: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to register user: {}", error.getMessage()));
    }

    private Mono<User> createUser(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
        log.info("Creating user ");
        // Generate username from email prefix
        String username = request.getEmail().split("@")[0];

        User user = User.builder()
                .email(request.getEmail())
                .username(username)  // Set username from email
                .magicUserId(magicUser.getUserId())  // Use getUserId() for the actual Magic User ID
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

        private Mono<Void> checkUserExists(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
            String emailToCheck = request.getEmail(); // Use email from request, not magicUser
            log.info("Checking user exists : {}", emailToCheck);
            return userRepository.existsByEmail(emailToCheck)
                    .flatMap(exists -> {
                        log.info("Exists : {}", exists);
                        return Boolean.TRUE.equals(exists) ?
                                Mono.error(new UserAlreadyExistsException(emailToCheck)) :
                                Mono.empty();
                    })
                    .then(userRepository.existsByMagicUserId(magicUser.getIssuer())
                            .flatMap(exists -> Boolean.TRUE.equals(exists) ?
                                    Mono.error(new UserAlreadyExistsException("Magic ID", magicUser.getIssuer())) :
                                    Mono.empty()));
        }

    /**
     * New async-first approach: Create proxy wallet and publish event for async processing chain
     * ProxyWalletCreatedEvent → EnclaveUdaCreationListener → EnclaveUdaCreatedEvent → BlnkBalanceCreationListener → BlnkBalanceCreatedEvent → KeycloakProvisionListener
     */
    private Mono<User> setupExternalIntegrationsAsyncViawEvents(User user, String magicUserId, String didToken) {
        // Create proxy wallet synchronously (needed for event data)
        return proxyWalletService.createUserProxyWallet(user.getMagicWalletAddress())
                .map(proxyWalletAddress -> {
                    user.setProxyWalletAddress(proxyWalletAddress);
                    user.setProxyWalletStatus(User.ProxyWalletStatus.ACTIVE);
                    user.setProxyWalletCreatedAt(Instant.now());
                    return user;
                })
                .onErrorResume(error -> {
                    log.warn("Failed to create proxy wallet, continuing without it: {}", error.getMessage());
                    return Mono.just(user);
                })
                .flatMap(u -> userRepository.save(u))  // Save updated user with proxy wallet
                .doOnSuccess(u -> {
                    // Publish ProxyWalletCreatedEvent to kickoff the async event chain
                    ProxyWalletCreatedEvent event = ProxyWalletCreatedEvent.builder()
                            .userId(u.getId())
                            .magicWalletAddress(u.getMagicWalletAddress())
                            .proxyWalletAddress(u.getProxyWalletAddress())
                            .email(u.getEmail())
                            .magicUserId(magicUserId)
                            .didToken(didToken)
                            .timestamp(Instant.now())
                            .build();
                    eventPublisher.publishEvent(event);
                    log.info("Published ProxyWalletCreatedEvent for user: {}, starting async chain", u.getId());
                })
                .doOnError(error -> log.error("Error setting up external integrations async: {}", error.getMessage()));
    }

    private UserRegistrationResponse buildResponse(User user) {
        return UserRegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .magicWalletAddress(user.getMagicWalletAddress())
                .enclaveUdaAddress(user.getEnclaveUdaAddress())
                .proxyWalletAddress(user.getProxyWalletAddress())
                .referralCode(user.getReferralCode())
                .avatarUrl(user.getAvatarUrl())
                .proxyWalletQrCodeUrl(user.getProxyWalletQrCodeUrl())
                .enclaveUdaQrCodeUrl(user.getEnclaveUdaQrCodeUrl())
                .evmDepositQrCodes(user.getEvmDepositQrCodes())
                .solanaDepositQrCodeUrl(user.getSolanaDepositQrCodeUrl())
                .bitcoinDepositQrCodes(user.getBitcoinDepositQrCodes())
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
            eventPublisher.publishEvent(event);
        });
    }

    private Mono<Void> publishKeycloakProvisionEvent(User user, String magicUserId, String didToken) {
        return Mono.fromRunnable(() -> {
            com.oregonMarkets.event.KeycloakProvisionEvent event = com.oregonMarkets.event.KeycloakProvisionEvent.of(
                    user.getId(),
                    magicUserId,
                    didToken
            );
            eventPublisher.publishEvent(event);
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