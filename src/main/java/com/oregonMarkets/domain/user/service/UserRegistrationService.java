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
        log.info("=== REGISTER USER START === email: {}, magicUserId: {}", request.getEmail(), magicUser.getUserId());

        // Step 1: Check for duplicates FIRST before creating the user object
        log.info("STEP 1: About to call checkUserExists");
        return checkUserExists(magicUser)
                .doOnSuccess(v -> log.info("STEP 1 COMPLETE: checkUserExists returned successfully"))
                .doOnError(e -> log.error("STEP 1 ERROR: checkUserExists failed with: {}", e.getMessage(), e))
                // Step 2: Create user object if no duplicates found
                .then(Mono.defer(() -> {
                    log.info("STEP 2: Creating user object");
                    return createUser(magicUser, request);
                }))
                .doOnNext(user -> log.info("STEP 2 COMPLETE: User object created with email: {}", user.getEmail()))
                .doOnError(e -> log.error("STEP 2 ERROR: createUser failed with: {}", e.getMessage(), e))
                // Step 3: Save user to database
                .flatMap(user -> {
                    log.info("STEP 3: About to save user with email: {}", user.getEmail());
                    return userRepository.save(user)
                            .doOnSuccess(savedUser -> log.info("STEP 3 COMPLETE: User saved with ID: {}", savedUser.getId()))
                            .doOnError(e -> {
                                log.error("STEP 3 ERROR: Failed to save user: {}", e.getMessage());
                                if (e instanceof org.springframework.dao.DuplicateKeyException) {
                                    log.error("DETECTED DUPLICATE KEY EXCEPTION from database");
                                }
                            });
                })
                .onErrorMap(throwable -> {
                    log.error("CONVERTING ERROR: Caught {} - {}", throwable.getClass().getSimpleName(), throwable.getMessage());
                    // Catch database duplicate key exceptions and convert to proper error
                    if (throwable instanceof org.springframework.dao.DuplicateKeyException) {
                        String message = throwable.getMessage();
                        log.error("CONVERTING DUPLICATE KEY: message contains={}", message);
                        if (message != null && message.contains("users_magic_user_id_key")) {
                            log.error("RETURNING: UserAlreadyExistsException for Magic ID");
                            return new UserAlreadyExistsException("Magic ID", magicUser.getUserId());
                        }
                        log.error("RETURNING: UserAlreadyExistsException for email");
                        return new UserAlreadyExistsException(request.getEmail());
                    }
                    return throwable;
                })
                // Step 4: Setup external integrations
                .flatMap(user -> {
                    log.info("STEP 4: Setting up external integrations for user: {}", user.getId());
                    return setupExternalIntegrationsAsyncViawEvents(user, magicUser.getUserId(), didToken)
                            .doOnSuccess(u -> log.info("STEP 4 COMPLETE: External integrations setup"))
                            .doOnError(e -> log.error("STEP 4 ERROR: {}", e.getMessage(), e));
                })
                // Step 5: Publish events and cache
                .flatMap(savedUser -> {
                    log.info("STEP 5: Publishing user registered event");
                    return publishUserRegisteredEvent(savedUser)
                            .then(cacheUserData(savedUser))
                            .thenReturn(buildResponse(savedUser))
                            .doOnSuccess(response -> log.info("STEP 5 COMPLETE: Events published and cached"))
                            .doOnError(e -> log.error("STEP 5 ERROR: {}", e.getMessage(), e));
                })
                .doOnSuccess(response -> log.info("=== REGISTER USER COMPLETE === userId: {}", response.getUserId()))
                .doOnError(error -> log.error("=== REGISTER USER FAILED === Error: {}", error.getMessage(), error));
    }

    private Mono<User> createUser(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
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

    private Mono<Void> checkUserExists(MagicDIDValidator.MagicUserInfo magicUser) {
        log.info("ENTERING checkUserExists for email: {}, magicUserId: {}", magicUser.getEmail(), magicUser.getUserId());

        return Mono.zip(
                userRepository.existsByEmail(magicUser.getEmail())
                        .doOnNext(exists -> log.info("Email check result for {}: {}", magicUser.getEmail(), exists)),
                userRepository.existsByMagicUserId(magicUser.getUserId())
                        .doOnNext(exists -> log.info("MagicId check result for {}: {}", magicUser.getUserId(), exists))
        )
        .doOnNext(tuple -> log.info("Zip completed with emailExists: {}, magicIdExists: {}", tuple.getT1(), tuple.getT2()))
        .flatMap(tuple -> {
            Boolean emailExists = tuple.getT1();
            Boolean magicIdExists = tuple.getT2();

            log.info("CHECKING user existence - Email: {}, EmailExists: {}, MagicId: {}, MagicIdExists: {}",
                    magicUser.getEmail(), emailExists, magicUser.getUserId(), magicIdExists);

            if (emailExists) {
                log.warn("*** DUPLICATE FOUND: User with email {} already exists ***", magicUser.getEmail());
                return Mono.error(new UserAlreadyExistsException(magicUser.getEmail()));
            }
            if (magicIdExists) {
                log.warn("*** DUPLICATE FOUND: User with Magic ID {} already exists ***", magicUser.getUserId());
                return Mono.error(new UserAlreadyExistsException("Magic ID", magicUser.getUserId()));
            }
            log.info("No duplicate found, proceeding with registration");
            return Mono.empty();
        })
        .doOnError(error -> log.error("checkUserExists error: {}", error.getMessage()));
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