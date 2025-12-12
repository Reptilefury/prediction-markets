package com.oregonMarkets.integration.magic;

import com.oregonMarkets.common.exception.MagicAuthException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class MagicClientIntegrationTest {

  private MockWebServer mockWebServer;
  private MagicClient magicClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    magicClient = new MagicClient(webClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void validateDIDToken_Success() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"issuer\":\"did:ethr:0x123\",\"email\":\"test@example.com\",\"publicAddress\":\"0x123\"}"));

    StepVerifier.create(magicClient.validateDIDToken("valid-token"))
        .expectNextMatches(
            userInfo ->
                userInfo.getEmail().equals("test@example.com")
                    && userInfo.getIssuer().equals("did:ethr:0x123")
                    && userInfo.getPublicAddress().equals("0x123"))
        .verifyComplete();
  }

  @Test
  void validateDIDToken_ApiError_4xx() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Unauthorized\"}"));

    StepVerifier.create(magicClient.validateDIDToken("invalid-token"))
        .expectErrorMatches(
            error ->
                error instanceof MagicAuthException
                    && error.getMessage().contains("Magic API returned"))
        .verify();
  }

  @Test
  void validateDIDToken_ApiError_5xx() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Internal Server Error\"}"));

    StepVerifier.create(magicClient.validateDIDToken("token"))
        .expectErrorMatches(
            error ->
                error instanceof MagicAuthException
                    && error.getMessage().contains("Magic API returned"))
        .verify();
  }

  @Test
  void validateDIDToken_NetworkError() {
    mockWebServer.enqueue(
        new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

    StepVerifier.create(magicClient.validateDIDToken("token"))
        .expectErrorMatches(error -> error instanceof MagicAuthException)
        .verify();
  }

  @Test
  void magicUserInfo_GettersAndSetters() {
    MagicClient.MagicUserInfo userInfo = new MagicClient.MagicUserInfo();
    userInfo.setIssuer("issuer-123");
    userInfo.setEmail("user@test.com");
    userInfo.setPublicAddress("0xabc");

    assert userInfo.getIssuer().equals("issuer-123");
    assert userInfo.getEmail().equals("user@test.com");
    assert userInfo.getPublicAddress().equals("0xabc");
  }
}
