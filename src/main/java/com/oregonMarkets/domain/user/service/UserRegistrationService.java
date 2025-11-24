package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.event.UserRegisteredEvent;
import com.oregonMarkets.integration.blnk.BlnkClient;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonMarkets.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final EnclaveClient enclaveClient;
    private final BlnkClient blnkClient;
    private final ProxyWalletOnboardingService proxyWalletService;
    private final CacheService cacheService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Value("${app.enclave.destination-token-address:0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174}")
    private String destinationTokenAddress;

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
        return checkUserExists(magicUser)
                .then(createUser(magicUser, request))
                .flatMap(userRepository::save)  // Save first to generate UUID
                .flatMap(user -> setupExternalIntegrationsWithUDA(user)  // Get both user and UDA response
                        .flatMap(pair -> {
                            User updatedUser = pair.getT1();
                            EnclaveClient.EnclaveUDAResponse udaResponse = pair.getT2();
                            return userRepository.save(updatedUser)
                                    .flatMap(savedUser -> publishUserRegisteredEvent(savedUser)
                                            .then(publishKeycloakProvisionEvent(savedUser, magicUser.getUserId(), didToken))
                                            .then(cacheUserData(savedUser))
                                            .thenReturn(buildResponse(savedUser, udaResponse))
                                    );
                        })
                )
                .doOnSuccess(response -> log.info("Successfully registered user: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to register user: {}", error.getMessage()));
    }

    private Mono<User> createUser(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
        User user = User.builder()
                .email(request.getEmail())
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

    private Mono<Void> checkUserExists(MagicDIDValidator.MagicUserInfo magicUser) {
        return userRepository.existsByEmail(magicUser.getEmail())
                .flatMap(exists -> exists ?
                        Mono.error(new UserAlreadyExistsException(magicUser.getEmail())) :
                        Mono.empty())
                .then(userRepository.existsByMagicUserId(magicUser.getIssuer())
                        .flatMap(exists -> exists ?
                                Mono.error(new UserAlreadyExistsException("Magic ID", magicUser.getIssuer())) :
                                Mono.empty()));
    }

    private Mono<reactor.util.context.ContextView> setupExternalIntegrations(User user) {
        // Placeholder to maintain compatibility - will be replaced
        return setupExternalIntegrationsWithUDA(user).then(Mono.empty());
    }

    private Mono<reactor.util.function.Tuple2<User, EnclaveClient.EnclaveUDAResponse>> setupExternalIntegrationsWithUDA(User user) {
        // Use email as identifier since UUID may not be generated yet
        String userId = user.getId() != null ? user.getId().toString() : user.getEmail();

        // First create proxy wallet, then use it as destinationTokenAddress for UDA
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
                .flatMap(u -> {
                    // Use proxy wallet address as destinationAddress (not the magic wallet which is in wrong format)
                    // Use configured token address for destinationTokenAddress
                    String proxyWalletOrMagic = u.getProxyWalletAddress() != null ?
                        u.getProxyWalletAddress() : u.getMagicWalletAddress();
                    return enclaveClient.createUDA(
                            userId,
                            u.getEmail(),
                            proxyWalletOrMagic,
                            destinationTokenAddress
                    )
                    .map(udaResponse -> {
                        u.setEnclaveUserId(udaResponse.getUserId());
                        u.setEnclaveUdaAddress(udaResponse.getUdaAddress());
                        u.setEnclaveUdaTag(udaResponse.getTag());
                        u.setEnclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE);
                        if (udaResponse.getCreatedAt() > 0) {
                            u.setEnclaveUdaCreatedAt(java.time.Instant.ofEpochMilli(udaResponse.getCreatedAt()));
                        }
                        // Serialize and store deposit addresses from Enclave API response
                        if (udaResponse.getDepositAddresses() != null) {
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                String depositAddressesJson = mapper.writeValueAsString(udaResponse.getDepositAddresses());
                                u.setEnclaveDepositAddresses(depositAddressesJson);
                            } catch (Exception e) {
                                log.warn("Failed to serialize Enclave deposit addresses: {}", e.getMessage());
                            }
                        }
                        return reactor.util.function.Tuples.of(u, udaResponse);
                    })
                    .onErrorResume(error -> {
                        log.warn("Failed to create UDA, continuing without it: {}", error.getMessage());
                        return Mono.just(reactor.util.function.Tuples.of(u, (EnclaveClient.EnclaveUDAResponse) null))
                            .switchIfEmpty(Mono.just(reactor.util.function.Tuples.of(u, (EnclaveClient.EnclaveUDAResponse) null)));
                    });
                })
                .flatMap(pair -> {
                    User u = pair.getT1();
                    String blnkUserId = u.getId() != null ? u.getId().toString() : u.getEmail();
                    String username = u.getUsername() != null ? u.getUsername() : "user_" + (u.getId() != null ? u.getId() : u.getEmail());
                    return blnkClient.createIdentity(
                                        blnkUserId,
                                        u.getEmail(),
                                        Map.of(
                                                "username", username,
                                                "magic_wallet", u.getMagicWalletAddress()
                                        )
                                ).flatMap(identityId -> blnkClient.createAccount(
                                                        identityId,
                                                        "USDC",
                                                        "Oregon Markets Balance - " + u.getEmail()
                                                )
                                                .map(accountId -> {
                                                    u.setBlnkIdentityId(identityId);
                                                    u.setBlnkAccountId(accountId);
                                                    u.setBlnkCreatedAt(Instant.now());
                                                    return reactor.util.function.Tuples.of(u, pair.getT2());
                                                })
                                        )
                                .onErrorResume(error -> {
                                    log.warn("Failed to create Blnk identity/account: {}", error.getMessage());
                                    return Mono.just(reactor.util.function.Tuples.of(u, pair.getT2()));
                                });
                });
    }

    private UserRegistrationResponse buildResponse(User user, EnclaveClient.EnclaveUDAResponse udaResponse) {
        UserRegistrationResponse.DepositAddresses cleanDepositAddresses = null;
        if (udaResponse != null && udaResponse.getDepositAddresses() != null) {
            cleanDepositAddresses = com.oregonMarkets.domain.user.dto.response.DepositAddressTransformer
                    .transform(udaResponse.getDepositAddresses());
        }

        return UserRegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .magicWalletAddress(user.getMagicWalletAddress())
                .enclaveUdaAddress(user.getEnclaveUdaAddress())
                .proxyWalletAddress(user.getProxyWalletAddress())
                .depositAddresses(cleanDepositAddresses)
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