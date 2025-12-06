package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.common.exception.BlnkApiException;
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

  private MockWebServer mockWebServer;
  private BlnkClient blnkClient;
  private BlnkProperties blnkProperties;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    blnkProperties = new BlnkProperties();
    blnkProperties.setLedgerId("test-ledger");

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    blnkClient = new BlnkClient(webClient, blnkProperties);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void createIdentity_WithValidData_ShouldReturnIdentityId() {
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"identity_id\":\"test-id\"}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(
            blnkClient.createIdentity("user123", "test@example.com", Map.of("key", "value")))
        .expectNext("test-id")
        .verifyComplete();
  }

  @Test
  void createIdentity_WithNullUserId_ShouldReturnError() {
    StepVerifier.create(blnkClient.createIdentity(null, "test@example.com", Map.of()))
        .expectError(BlnkApiException.class)
        .verify();
  }

  @Test
  void createBalance_WithValidCurrency_ShouldReturnBalanceId() {
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"balance_id\":\"balance-123\"}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.createBalance("USD")).expectNext("balance-123").verifyComplete();
  }

  @Test
  void createBalance_WithNullCurrency_ShouldReturnError() {
    StepVerifier.create(blnkClient.createBalance(null))
        .expectError(BlnkApiException.class)
        .verify();
  }

  @Test
  void getBalancesByIdentity_WithValidId_ShouldReturnBalances() {
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"data\":[{\"id\":\"balance1\"},{\"id\":\"balance2\"}]}")
            .setHeader("Content-Type", "application/json"));

    StepVerifier.create(blnkClient.getBalancesByIdentity("identity123"))
        .assertNext(
            balances -> {
              assert balances.size() == 2;
            })
        .verifyComplete();
  }
}
