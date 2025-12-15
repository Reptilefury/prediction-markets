package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.common.exception.UserAlreadyExistsException;
import com.oregonMarkets.common.exception.UserNotFoundException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserProfileMapper;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.model.User;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.integration.polymarket.ProxyWalletOnboardingService;
import com.oregonMarkets.service.CacheService;
import com.oregonMarkets.service.UsernameGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ProxyWalletOnboardingService proxyWalletService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private UsernameGenerationService usernameGenerationService;
    
    @Mock
    private UserProfileMapper userProfileMapper;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(
                userRepository,
                proxyWalletService,
                cacheService,
                eventPublisher,
                usernameGenerationService,
                userProfileMapper
        );
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        MagicDIDValidator.MagicUserInfo magicUser = mock(MagicDIDValidator.MagicUserInfo.class);
        when(magicUser.getUserId()).thenReturn("magic-user-id");
        when(magicUser.getEmail()).thenReturn("test@example.com");
        when(userRepository.findByMagicUserId(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(service.getUserProfile(magicUser))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldReturnUserProfileWhenUserExists() {
        MagicDIDValidator.MagicUserInfo magicUser = mock(MagicDIDValidator.MagicUserInfo.class);
        when(magicUser.getUserId()).thenReturn("magic-user-id");
        when(magicUser.getEmail()).thenReturn("test@example.com");
        
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();
        
        UserRegistrationResponse response = UserRegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .build();

        when(userRepository.findByMagicUserId("magic-user-id")).thenReturn(Mono.just(user));
        when(userProfileMapper.toResponse(user)).thenReturn(response);

        StepVerifier.create(service.getUserProfile(magicUser))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldThrowUserAlreadyExistsWhenEmailExists() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("existing@example.com");
        request.setCountryCode("US");
        
        MagicDIDValidator.MagicUserInfo magicUser = mock(MagicDIDValidator.MagicUserInfo.class);
        when(magicUser.getEmail()).thenReturn("existing@example.com");
        lenient().when(magicUser.getUserId()).thenReturn("magic-id");
        lenient().when(magicUser.getIssuer()).thenReturn("issuer");
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(Mono.just(true));
        lenient().when(userRepository.existsByMagicUserId("issuer")).thenReturn(Mono.just(false));

        StepVerifier.create(service.registerUser(request, magicUser, "token"))
                .expectError(UserAlreadyExistsException.class)
                .verify();
    }
}
