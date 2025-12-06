package com.oregonMarkets.integration.enclave;

import com.oregonMarkets.common.exception.EnclaveApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class EnclaveClientTest {

    private MockWebServer mockWebServer;
    private EnclaveClient enclaveClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        enclaveClient = new EnclaveClient(webClient);
        ReflectionTestUtils.setField(enclaveClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(enclaveClient, "baseUrl", "http://localhost:8080");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void createUDA_Success() {
        String responseBody = """
            {
                "success": true,
                "data": {
                    "_id": "uda-123",
                    "userId": "user-123",
                    "destinationChainId": 8453,
                    "destinationAddress": "0x123",
                    "destinationTokenAddress": "0x456",
                    "status": "active",
                    "createdAt": 1234567890,
                    "updatedAt": 1234567890,
                    "depositAddresses": {"ethereum": "0x789"},
                    "__v": 0
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseBody));

        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", "0x456"))
            .expectNextMatches(response -> 
                "uda-123".equals(response.getUdaAddress()) &&
                "user-123".equals(response.getUserId()) &&
                "0x123".equals(response.getDestinationAddress()) &&
                "active".equals(response.getStatus())
            )
            .verifyComplete();
    }

    @Test
    void createUDA_NullUserId_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA(null, "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_EmptyUserId_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("", "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_NullEmail_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", null, "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_EmptyEmail_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", "", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_NullWalletAddress_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", null, "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_EmptyWalletAddress_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_NullDestinationTokenAddress_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", null))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_EmptyDestinationTokenAddress_ThrowsException() {
        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", ""))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_NoApiKey_ThrowsException() {
        ReflectionTestUtils.setField(enclaveClient, "apiKey", "");

        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_MissingDataField_ThrowsException() {
        String responseBody = """
            {
                "success": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseBody));

        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_InvalidJsonResponse_ThrowsException() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("invalid json"));

        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }

    @Test
    void createUDA_HttpError_ThrowsException() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        StepVerifier.create(enclaveClient.createUDA("user-123", "test@example.com", "0x123", "0x456"))
            .expectError(EnclaveApiException.class)
            .verify();
    }
}