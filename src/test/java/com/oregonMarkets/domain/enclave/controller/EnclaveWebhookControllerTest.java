package com.oregonMarkets.domain.enclave.controller;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

class EnclaveWebhookControllerTest {

  private WebTestClient webTestClient;
  private EnclaveWebhookController controller;

  @BeforeEach
  void setUp() {
    controller = new EnclaveWebhookController();
    RouterFunction<ServerResponse> routes = controller.enclaveWebhookRoutes();
    webTestClient = WebTestClient.bindToRouterFunction(routes).build();
  }

  @Test
  void handleEnclaveWebhook_DepositDetectedEvent_ReturnsOk() {
    Map<String, Object> payload =
        Map.of(
            "event_type", "deposit.detected",
            "transaction_hash", "0x123",
            "amount", "100.00",
            "currency", "USDC");

    webTestClient
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void handleEnclaveWebhook_DepositConfirmedEvent_ReturnsOk() {
    Map<String, Object> payload =
        Map.of(
            "event_type", "deposit.confirmed",
            "transaction_hash", "0x456",
            "amount", "50.00",
            "currency", "USDC",
            "confirmations", 12);

    webTestClient
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void handleEnclaveWebhook_UnknownEventType_ReturnsOk() {
    Map<String, Object> payload =
        Map.of(
            "event_type", "unknown.event",
            "data", "some data");

    webTestClient
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void handleEnclaveWebhook_InvalidPayload_ReturnsBadRequest() {
    webTestClient
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue("invalid json")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void handleEnclaveWebhook_EmptyPayload_ReturnsOk() {
    webTestClient.post().uri("/api/webhooks/enclave").exchange().expectStatus().isOk();
  }

  @Test
  void handleEnclaveWebhook_MissingEventType_ReturnsBadRequest() {
    Map<String, Object> payload =
        Map.of(
            "transaction_hash", "0x789",
            "amount", "25.00");

    webTestClient
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }
}
