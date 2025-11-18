package com.oregonMarkets.domain.user.controller;

import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.service.UserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthRouterConfig.class)
@Import(AuthRouterConfigTest.TestSecurityConfig.class)
class AuthRouterConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserRegistrationService userRegistrationService;

    @MockBean
    private Web3RegistrationService web3RegistrationService;

    @Configuration
    @EnableWebFluxSecurity
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            http.csrf().disable()
                .authorizeExchange()
                .anyExchange().permitAll();
            return http.build();
        }
    }

    @Test
    @Disabled("RouterFunction beans are not properly registered in WebFluxTest context. Requires integration test setup.")
    void register_Success() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setDidToken("test-did-token");
        request.setEmail("test@example.com");
        request.setCountryCode("US");

        UserRegistrationResponse response = UserRegistrationResponse.builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .magicWalletAddress("0x123")
            .referralCode("REF12345678")
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .createdAt(Instant.now())
            .build();

        when(userRegistrationService.registerUser(any(UserRegistrationRequest.class)))
            .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.magicWalletAddress").isEqualTo("0x123")
            .jsonPath("$.referralCode").isEqualTo("REF12345678");
    }



    @Test
    @Disabled("RouterFunction beans are not properly registered in WebFluxTest context. Requires integration test setup.")
    void register_ServiceError() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setDidToken("test-did-token");
        request.setEmail("test@example.com");
        request.setCountryCode("US");

        when(userRegistrationService.registerUser(any(UserRegistrationRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When & Then
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .isEqualTo("Registration failed: Service error");
    }
}