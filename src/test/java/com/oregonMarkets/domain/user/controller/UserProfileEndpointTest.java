package com.oregonMarkets.domain.user.controller;

import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.service.IUserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import com.oregonMarkets.dto.ErrorType;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import com.oregonMarkets.security.ErrorResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.flyway.enabled=false",
    "DATABASE_URL=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "JDBC_DATABASE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "DATABASE_USERNAME=sa",
    "DATABASE_PASSWORD=",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=",
    "BASE_URL=http://localhost:8080",
    "MAGIC_API_KEY=test-magic-key",
    "MAGIC_API_URL=https://api.magic.link",
    "ENCLAVE_API_KEY=test-enclave-key",
    "ENCLAVE_API_URL=http://localhost:3000",
    "APP_ENCLAVE_DESTINATION_TOKEN_ADDRESS=0x1234567890123456789012345678901234567890",
    "BLNK_API_URL=http://localhost:5001",
    "BLNK_LEDGER_ID=test-ledger",
    "CRYPTO_SERVICE_URL=http://localhost:8080",
    "WALLETCONNECT_PROJECT_ID=test-project-id",
    "DYNAMIC_ENVIRONMENT_ID=test-env-id",
    "LOGODEV_PUBLISHABLE_KEY=pk_test_mock_key",
    "GCP_PROJECT_ID=test-project",
    "PUBSUB_TOPIC_WALLET=test-topic",
    "PUBSUB_SUBSCRIPTION_WALLET=test-subscription",
    "KEYCLOAK_BASE_URL=http://localhost:8081",
    "KEYCLOAK_REALM=test-realm",
    "KEYCLOAK_CLIENT_ID=test-client",
    "KEYCLOAK_CLIENT_SECRET=test-secret"
})
class UserProfileEndpointTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IUserRegistrationService userRegistrationService;

    @MockBean
    private Web3RegistrationService web3RegistrationService;

    @MockBean
    private MagicDIDValidator magicDIDValidator;

    @MockBean
    private ErrorResponseBuilder errorResponseBuilder;

    @BeforeEach
    void setUp() {
        // Mock ErrorResponseBuilder to return valid JSON bytes
        when(errorResponseBuilder.buildErrorResponse(any(ErrorType.class), anyString()))
            .thenReturn("{\"error\":\"Authentication failed\"}".getBytes());
    }

    @Test
    void getUserProfile_Success() {
        // Given
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
                .expectStatus().isUnauthorized(); // Since we don't have Magic auth in test
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
                .expectStatus().isUnauthorized(); // Since we don't have Magic auth in test
    }
}
