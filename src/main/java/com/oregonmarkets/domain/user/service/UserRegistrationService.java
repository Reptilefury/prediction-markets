package com.oregonmarkets.domain.user.service;

import com.oregonmarkets.common.exception.UserAlreadyExistsException;
import com.oregonmarkets.common.exception.UserNotFoundException;
import com.oregonmarkets.common.util.DataMaskingUtil;
import com.oregonmarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonmarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonmarkets.domain.user.dto.response.UserProfileMapper;
import com.oregonmarkets.domain.user.model.User;
import com.oregonmarkets.domain.user.repository.UserRepository;
import com.oregonmarkets.event.ProxyWalletCreatedEvent;
import com.oregonmarkets.event.UserRegisteredEvent;
import com.oregonmarkets.integration.magic.MagicDIDValidator;
import com.oregonmarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonmarkets.service.CacheService;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService implements IUserRegistrationService {

    private final UserRepository userRepository;
    private final ProxyWalletOnboardingService proxyWalletService;
    private final CacheService cacheService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.oregonmarkets.service.UsernameGenerationService usernameGenerationService;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Mono<UserRegistrationResponse> registerUser(@Valid UserRegistrationRequest request, MagicDIDValidator.MagicUserInfo magicUser, String didToken) {
        log.info("Starting user registration for email: {}", request.getEmail());
        return checkUserExists(magicUser, request)
                .then(createUser(magicUser, request))
                .flatMap(userRepository::save)
                .flatMap(user -> {
                    usernameGenerationService.applyUsernameAndDisplayName(user);
                    return userRepository.save(user);
                })
                .flatMap(user -> setupExternalIntegrationsAsyncViawEvents(user, magicUser.getUserId(), didToken)
                        .flatMap(savedUser ->
                                publishUserRegisteredEvent(savedUser)
                                        .then(cacheUserData(savedUser))
                                        .thenReturn(userProfileMapper.toResponse(savedUser))))
                .doOnSuccess(response -> log.info("Successfully registered user: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to register user: {}", error.getMessage()));
    }

    @Override
    public Mono<UserRegistrationResponse> getUserProfile(MagicDIDValidator.MagicUserInfo magicUser) {
        log.info("Getting user profile for Magic user: {}", DataMaskingUtil.maskEmail(magicUser.getEmail()));

        return userRepository.findByMagicUserId(magicUser.getUserId())
                .switchIfEmpty(Mono.error(new UserNotFoundException("User profile not found. Please register first to create your account.")))
                .map(userProfileMapper::toResponse)
                .doOnSuccess(response -> log.info("Successfully retrieved user profile: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to get user profile: {}", error.getMessage()));
    }

    private Mono<User> createUser(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
        log.info("Creating user ");
        User user = User.builder()
                .email(request.getEmail())
                .magicUserId(magicUser.getUserId())
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
                    }).switchIfEmpty(Mono.just(user));
        }
        return userMono;
    }

    private Mono<Void> checkUserExists(MagicDIDValidator.MagicUserInfo magicUser, UserRegistrationRequest request) {
        String emailToCheck = request.getEmail();
        log.info("Checking user exists : {}", emailToCheck);
        return userRepository.existsByEmail(emailToCheck)
                .flatMap(exists -> {
                    log.info("Exists : {}", exists);
                    return Boolean.TRUE.equals(exists)
                            ? Mono.error(new UserAlreadyExistsException(emailToCheck))
                            : Mono.empty();
                })
                .then(userRepository.existsByMagicUserId(magicUser.getIssuer())
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(
                                new UserAlreadyExistsException("Magic ID", magicUser.getIssuer()))
                                : Mono.empty()));
    }

    /**
     * New async-first approach: Create Biconomy smart account and publish event for async processing
     * chain ProxyWalletCreatedEvent → EnclaveUdaCreationListener → EnclaveUdaCreatedEvent →
     * BlnkBalanceCreationListener → BlnkBalanceCreatedEvent → KeycloakProvisionListener
     *
     * <p>IMPORTANT: This method will fail user registration if smart account creation fails
     */
    private Mono<User> setupExternalIntegrationsAsyncViawEvents(
            User user, String magicUserId, String didToken) {
        log.info("[USER-REGISTRATION] Setting up external integrations for user: {}", DataMaskingUtil.maskUserId(user.getId().toString()));
        log.info("[USER-REGISTRATION] Magic wallet address: {}", DataMaskingUtil.maskWalletAddress(user.getMagicWalletAddress()));
        return proxyWalletService.createUserProxyWallet(user.getMagicWalletAddress(), didToken)
                .map(walletData -> {
                    log.info("[USER-REGISTRATION] Processing smart account creation response");
                    user.setProxyWalletAddress(walletData.getSmartAccount().getSmartAccountAddress());
                    user.setProxyWalletStatus(User.ProxyWalletStatus.ACTIVE);
                    user.setProxyWalletCreatedAt(Instant.now());
                    user.setBiconomySmartAccountAddress(walletData.getSmartAccount().getSmartAccountAddress());
                    user.setBiconomyDeployed(walletData.getSmartAccount().getDeployed());
                    user.setBiconomyChainId(walletData.getSmartAccount().getChainId());
                    user.setBiconomyBundlerUrl(walletData.getSmartAccount().getBundlerUrl());
                    user.setBiconomyPaymasterUrl(walletData.getSmartAccount().getPaymasterUrl());
                    user.setBiconomyCreatedAt(Instant.now());

                    log.info("[USER-REGISTRATION] ✓ Smart account data saved to user entity");
                    log.info("[USER-REGISTRATION] Smart Account: {}", DataMaskingUtil.maskWalletAddress(
                            walletData.getSmartAccount().getSmartAccountAddress()));
                    log.info("[USER-REGISTRATION] Deployed: {}", walletData.getSmartAccount().getDeployed());
                    return user;
                })
                .doOnError(error -> {
                            log.error("[USER-REGISTRATION] ✗ CRITICAL: Smart account creation failed - user registration will be aborted");
                            log.error("[USER-REGISTRATION] User ID: {}", DataMaskingUtil.maskUserId(user.getId().toString()));
                            log.error("[USER-REGISTRATION] Email: {}", DataMaskingUtil.maskEmail(user.getEmail()));
                            log.error("[USER-REGISTRATION] Magic Wallet: {}", DataMaskingUtil.maskWalletAddress(user.getMagicWalletAddress()));
                            log.error("[USER-REGISTRATION] Error: {}", error.getMessage(), error);
                        })
                .flatMap(u -> {
                            log.info("[USER-REGISTRATION] Saving user with smart account data to database");
                            return userRepository.save(u);
                        }).doOnSuccess(u -> {
                            log.info("[USER-REGISTRATION] User saved successfully with smart account");
                            if (u.getProxyWalletAddress() != null) {
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
                                log.info("[USER-REGISTRATION] ✓ Published ProxyWalletCreatedEvent for user: {}, starting async chain", DataMaskingUtil.maskUserId(u.getId().toString()));
                            } else {
                                log.warn("[USER-REGISTRATION] No proxy wallet address - skipping event publication");
                            }
                        });
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
                                    user.getReferredByUserId());
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
