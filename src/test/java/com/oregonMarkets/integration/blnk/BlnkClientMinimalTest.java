package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.common.exception.BlnkApiException;
import com.oregonMarkets.config.BlnkProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class BlnkClientMinimalTest {

    @Mock
    private WebClient webClient;

    @Mock
    private BlnkProperties blnkProperties;

    private BlnkClient blnkClient;

    @BeforeEach
    void setUp() {
        blnkClient = new BlnkClient(webClient, blnkProperties);
    }

    @Test
    void constructor_WithValidDependencies_CreatesInstance() {
        assertNotNull(blnkClient);
    }

    @Test
    void createIdentity_WithNullUserId_ReturnsError() {
        Map<String, Object> metadata = Map.of("key", "value");

        StepVerifier.create(blnkClient.createIdentity(null, "test@example.com", metadata))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createIdentity_WithEmptyUserId_ReturnsError() {
        Map<String, Object> metadata = Map.of("key", "value");

        StepVerifier.create(blnkClient.createIdentity("", "test@example.com", metadata))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createIdentity_WithNullEmail_ReturnsError() {
        Map<String, Object> metadata = Map.of("key", "value");

        StepVerifier.create(blnkClient.createIdentity("user-123", null, metadata))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createIdentity_WithEmptyEmail_ReturnsError() {
        Map<String, Object> metadata = Map.of("key", "value");

        StepVerifier.create(blnkClient.createIdentity("user-123", "", metadata))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createIdentity_WithNullMetadata_ReturnsError() {
        StepVerifier.create(blnkClient.createIdentity("user-123", "test@example.com", null))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createBalance_WithNullIdentityId_ReturnsError() {
        StepVerifier.create(blnkClient.createBalance(null))
            .expectError(BlnkApiException.class)
            .verify();
    }

    @Test
    void createBalance_WithEmptyIdentityId_ReturnsError() {
        StepVerifier.create(blnkClient.createBalance(""))
            .expectError(BlnkApiException.class)
            .verify();
    }
}
