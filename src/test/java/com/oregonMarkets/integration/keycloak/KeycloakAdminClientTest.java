package com.oregonMarkets.integration.keycloak;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class KeycloakAdminClientTest {

  private MockWebServer mockWebServer;
  private KeycloakAdminClient keycloakClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    keycloakClient = new KeycloakAdminClient(webClient);
    ReflectionTestUtils.setField(keycloakClient, "realm", "test-realm");
    ReflectionTestUtils.setField(keycloakClient, "baseUrl", mockWebServer.url("/").toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void createUserIfAbsent_WithValidData_ShouldComplete() {
    // Mock token response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"access_token\":\"test-token\"}")
            .setHeader("Content-Type", "application/json"));

    // Mock create user response (201 Created)
    mockWebServer.enqueue(new MockResponse().setResponseCode(201));

    // Mock token response for password setting
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"access_token\":\"test-token\"}")
            .setHeader("Content-Type", "application/json"));

    // Mock find user response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("[{\"id\":\"user-123\"}]")
            .setHeader("Content-Type", "application/json"));

    // Mock set password response
    mockWebServer.enqueue(new MockResponse().setResponseCode(204));

    StepVerifier.create(keycloakClient.createUserIfAbsent("testuser", "password123"))
        .verifyComplete();
  }

  @Test
  void createUserIfAbsent_WithEmail_ShouldComplete() {
    // Mock token response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"access_token\":\"test-token\"}")
            .setHeader("Content-Type", "application/json"));

    // Mock create user response (201 Created)
    mockWebServer.enqueue(new MockResponse().setResponseCode(201));

    // Mock token response for password setting
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"access_token\":\"test-token\"}")
            .setHeader("Content-Type", "application/json"));

    // Mock find user response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("[{\"id\":\"user-123\"}]")
            .setHeader("Content-Type", "application/json"));

    // Mock set password response
    mockWebServer.enqueue(new MockResponse().setResponseCode(204));

    StepVerifier.create(
            keycloakClient.createUserIfAbsent("testuser", "password123", "test@example.com"))
        .verifyComplete();
  }
}
