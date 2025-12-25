package com.oregonmarkets.domain.enclave.controller;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class EnclaveWebhookControllerTest {

  private WebTestClient client;

  @BeforeEach
  void setUp() {
    EnclaveWebhookController controller = new EnclaveWebhookController();
    client =
        WebTestClient.bindToRouterFunction(controller.enclaveWebhookRoutes())
            .configureClient()
            .baseUrl("/")
            .build();
  }

  @Test
  void depositDetectedEvent_ReturnsOk() {
    client
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(Map.of("event_type", "deposit.detected", "payload", Map.of("amount", "10")))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void depositConfirmedEvent_ReturnsOk() {
    client
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(
            Map.of("event_type", "deposit.confirmed", "payload", Map.of("status", "confirmed")))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void unknownEventType_ReturnsOk() {
    client
        .post()
        .uri("/api/webhooks/enclave")
        .bodyValue(Map.of("event_type", "random.event"))
        .exchange()
        .expectStatus()
        .isOk();
  }
}
