package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.integration.blnk.BlnkClient;
import com.oregonMarkets.integration.enclave.EnclaveClient;
import com.oregonMarkets.integration.magic.MagicClient;
import com.oregonMarkets.service.CacheService;
import com.oregonMarkets.service.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MagicClient magicClient;
    
    @Mock
    private EnclaveClient enclaveClient;
    
    @Mock
    private BlnkClient blnkClient;

    @Mock
    private CacheService cacheService;

    @Mock
    private EventPublisher eventPublisher;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(
            userRepository, magicClient, enclaveClient, blnkClient, cacheService, eventPublisher
        );
    }

    @Test
    void registerUser_Success() {
        // Given
        UserRegistrationRequest request = createValidRequest();
        MagicClient.MagicUserInfo magicUser = createMagicUserInfo();
        EnclaveClient.EnclaveUDAResponse udaResponse = createUdaResponse();
        User savedUser = createSavedUser();

        // When
        when(magicClient.validateDIDToken(anyString())).thenReturn(Mono.just(magicUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));
        when(enclaveClient.createUDA(anyString(), anyString(), anyString())).thenReturn(Mono.just(udaResponse));
        when(blnkClient.createIdentity(anyString(), anyString(), any())).thenReturn(Mono.just("blnk-identity-id"));
        when(blnkClient.createAccount(anyString(), anyString(), anyString())).thenReturn(Mono.just("blnk-account-id"));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        doNothing().when(eventPublisher).publishEvent(anyString(), anyString(), any());
        when(cacheService.set(anyString(), any(), any())).thenReturn(Mono.empty());

        // Then
        StepVerifier.create(userRegistrationService.registerUser(request))
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
        MagicClient.MagicUserInfo magicUser = createMagicUserInfo();

        // When
        when(magicClient.validateDIDToken(anyString())).thenReturn(Mono.just(magicUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));
        when(userRepository.existsByMagicUserId(anyString())).thenReturn(Mono.just(false));

        // Then
        StepVerifier.create(userRegistrationService.registerUser(request))
            .expectErrorMatches(throwable -> 
                throwable instanceof UserAlreadyExistsException &&
                throwable.getMessage().contains("test@example.com")
            )
            .verify();
    }

    private UserRegistrationRequest createValidRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setDidToken("test-did-token");
        request.setEmail("test@example.com");
        request.setCountryCode("US");
        return request;
    }

    private MagicClient.MagicUserInfo createMagicUserInfo() {
        MagicClient.MagicUserInfo magicUser = new MagicClient.MagicUserInfo();
        magicUser.setEmail("test@example.com");
        magicUser.setIssuer("test-issuer");
        magicUser.setPublicAddress("0x123");
        return magicUser;
    }

    private EnclaveClient.EnclaveUDAResponse createUdaResponse() {
        EnclaveClient.EnclaveUDAResponse udaResponse = new EnclaveClient.EnclaveUDAResponse();
        udaResponse.setUserId("enclave-user-id");
        udaResponse.setUdaAddress("0xuda123");
        udaResponse.setTag("tag123");
        udaResponse.setCreatedAt(Instant.now());
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