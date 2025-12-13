package com.oregonMarkets.domain.user.controller;

import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.service.IUserRegistrationService;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthRouterConfig.class)
class UserProfileEndpointTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IUserRegistrationService userRegistrationService;

    @Test
    void getUserProfile_Success() {
        // Given
        MagicDIDValidator.MagicUserInfo magicUser = new MagicDIDValidator.MagicUserInfo(
                "did:ethr:0x123",
                "test@example.com",
                "0x123", 
                null,
                "test-user-id"
        );

        UserRegistrationResponse expectedResponse = UserRegistrationResponse.builder()
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .username("TestUser-123")
                .magicWalletAddress("0x123")
                .proxyWalletAddress("0x456")
                .referralCode("REF123")
                .build();

        when(userRegistrationService.getUserProfile(any(MagicDIDValidator.MagicUserInfo.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webTestClient.get()
                .uri("/api/user/profile")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.data.email").isEqualTo("test@example.com")
                .jsonPath("$.data.username").isEqualTo("TestUser-123")
                .jsonPath("$.data.magicWalletAddress").isEqualTo("0x123")
                .jsonPath("$.data.proxyWalletAddress").isEqualTo("0x456");
    }

    @Test
    void getUserProfile_UserNotFound() {
        // Given
        when(userRegistrationService.getUserProfile(any(MagicDIDValidator.MagicUserInfo.class)))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        // When & Then
        webTestClient.get()
                .uri("/api/user/profile")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
