package com.oregonMarkets.integration.blnk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oregonMarkets.config.BlnkProperties;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BlnkClientTest {

  private MockWebServer server;
  private BlnkClient blnkClient;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    String baseUrl = server.url("/").toString();
    BlnkProperties properties = new BlnkProperties(baseUrl, "ledger-1");
    blnkClient = new BlnkClient(WebClient.builder().baseUrl(baseUrl).build(), properties);
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
            .addHeader("Content-Type", "application/json"));

    Mono<String> result =
        blnkClient.createIdentity("user-1", "user@example.com", Map.of("key", "value"));

    StepVerifier.create(result).expectNext("identity-123").verifyComplete();
  }

  @Test
  void createBalance_ReturnsBalanceId() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"balance_id\":\"balance-456\"}")
            .addHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.createBalance("USDC"))
        .expectNext("balance-456")
        .verifyComplete();
  }

  @Test
  void getBalancesByIdentity_ReturnsList() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"data\":[{\"currency\":\"USDC\"}]}")
            .addHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.getBalancesByIdentity("identity-1"))
        .expectNextMatches(list -> !list.isEmpty() && "USDC".equals(list.get(0).get("currency")))
        .verifyComplete();
  }
}
