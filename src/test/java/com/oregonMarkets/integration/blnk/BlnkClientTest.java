package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.config.BlnkProperties;
import java.io.IOException;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class BlnkClientTest {

  private MockWebServer server;
  private BlnkClient blnkClient;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    String baseUrl = server.url("/").toString();
    blnkClient =
        new BlnkClient(
            WebClient.builder().baseUrl(baseUrl).build(), new BlnkProperties(baseUrl, "ledger-1"));
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void createIdentity_ReturnsIdentityId() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"identity_id\":\"identity-123\"}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(
            blnkClient.createIdentity("user-1", "user@example.com", Map.of("key", "value")))
        .expectNext("identity-123")
        .verifyComplete();
  }

  @Test
  void createBalance_ReturnsBalanceId() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"balance_id\":\"balance-456\"}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.createBalance("USDC"))
        .expectNext("balance-456")
        .verifyComplete();
  }

  @Test
  void getBalancesByIdentity_ReturnsList() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"data\":[{\"currency\":\"USDC\"}]}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.getBalancesByIdentity("identity-1"))
        .expectNextMatches(
            list -> !list.isEmpty() && "USDC".equals(list.get(0).get("currency")))
        .verifyComplete();
  }
}
