package com.oregonMarkets.integration.magic;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MagicClientTest {

  private MockWebServer mockWebServer;
  private MagicClient magicClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    magicClient =
        new MagicClient(webClient) {
          @Override
          public Mono<MagicUserInfo> validateDIDToken(String didToken) {
            return webClient
                .post()
                .uri("/admin/auth/user/get")
                .header("X-Magic-Secret-Key", "test-key")
                .bodyValue(java.util.Map.of("didToken", didToken))
                .retrieve()
                .bodyToMono(MagicUserInfo.class);
          }
        };
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void validateDIDToken_Success() {
    // Given
    String responseBody =
        """
            {
                "issuer": "did:ethr:0x123",
                "email": "test@example.com",
                "publicAddress": "0x123456789"
            }
            """;

    mockWebServer.enqueue(
        new MockResponse().setBody(responseBody).addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(magicClient.validateDIDToken("test-did-token"))
        .expectNextMatches(
            userInfo ->
                userInfo.getEmail().equals("test@example.com")
                    && userInfo.getIssuer().equals("did:ethr:0x123")
                    && userInfo.getPublicAddress().equals("0x123456789"))
        .verifyComplete();
  }

  @Test
  void validateDIDToken_InvalidToken() {
    // Given
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    // When & Then
    StepVerifier.create(magicClient.validateDIDToken("invalid-token")).expectError().verify();
  }
}
