package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.integration.blnk.BlnkClient;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonMarkets.service.CacheService;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnclaveClient enclaveClient;

    @Mock
    private BlnkClient blnkClient;

    @Mock
    private ProxyWalletOnboardingService proxyWalletOnboardingService;

    @Mock
    private CacheService cacheService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(
            userRepository, proxyWalletOnboardingService, cacheService, eventPublisher
        );
    }

    @Test
    void registerUser_Success() {
        // Given
        UserRegistrationRequest request = createValidRequest();
        MagicDIDValidator.MagicUserInfo magicUser = createMagicUserInfo();
        String didToken = "test-did-token";
        EnclaveClient.EnclaveUDAResponse udaResponse = createUdaResponse();
        User savedUser = createSavedUser();

        // When
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));
        when(proxyWalletOnboardingService.createUserProxyWallet(anyString())).thenReturn(Mono.just("0x456"));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(cacheService.set(anyString(), any(), any())).thenReturn(Mono.empty());

        // Then
        StepVerifier.create(userRegistrationService.registerUser(request, magicUser, didToken))
            .expectNextMatches(response ->
                response.getEmail().equals("test@example.com") &&
                response.getMagicWalletAddress().equals("0x123") &&
                response.getEnclaveUdaAddress().equals("0xuda123")
            )
            .verifyComplete();
    }

    @Test
    void registerUser_UserAlreadyExists() {
        // Given
        UserRegistrationRequest request = createValidRequest();
        MagicDIDValidator.MagicUserInfo magicUser = createMagicUserInfo();
        String didToken = "test-did-token";

        // When
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));
        when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));

        // Then
        StepVerifier.create(userRegistrationService.registerUser(request, magicUser, didToken))
            .expectErrorMatches(throwable -> 
                throwable instanceof UserAlreadyExistsException &&
                throwable.getMessage().contains("test@example.com")
            )
            .verify();
    }

    private UserRegistrationRequest createValidRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setCountryCode("US");
        return request;
    }

    private MagicDIDValidator.MagicUserInfo createMagicUserInfo() {
        MagicDIDValidator.MagicUserInfo magicUser = new MagicDIDValidator.MagicUserInfo();
        magicUser.setEmail("test@example.com");
        magicUser.setIssuer("test-issuer");
        magicUser.setPublicAddress("0x123");
        magicUser.setUserId("test-user-id");
        return magicUser;
    }

    private EnclaveClient.EnclaveUDAResponse createUdaResponse() {
        EnclaveClient.EnclaveUDAResponse udaResponse = new EnclaveClient.EnclaveUDAResponse();
        udaResponse.setUserId("enclave-user-id");
        udaResponse.setUdaAddress("0xuda123");
        udaResponse.setTag("tag123");
        udaResponse.setCreatedAt(System.currentTimeMillis());
        return udaResponse;
    }

    private User createSavedUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .magicUserId("test-issuer")
            .magicWalletAddress("0x123")
            .enclaveUdaAddress("0xuda123")
            .referralCode("REF12345678")
            .createdAt(Instant.now())
            .build();
    }
}