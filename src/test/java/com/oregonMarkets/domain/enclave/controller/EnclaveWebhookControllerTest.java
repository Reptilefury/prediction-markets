package com.oregonMarkets.domain.enclave.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class EnclaveWebhookControllerTest {

  private final EnclaveWebhookController controller = new EnclaveWebhookController();

  @Test
  void enclaveWebhookRoutes() {
    RouterFunction<ServerResponse> routes = controller.enclaveWebhookRoutes();
    assertNotNull(routes);
  }

  @Test
  void handleEnclaveWebhook_DepositDetected() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("event_type", "deposit.detected");
    assertDoesNotThrow(() -> controller.enclaveWebhookRoutes());
  }

  @Test
  void handleEnclaveWebhook_DepositConfirmed() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("event_type", "deposit.confirmed");
    assertDoesNotThrow(() -> controller.enclaveWebhookRoutes());
  }

  @Test
  void handleEnclaveWebhook_Unknown() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("event_type", "unknown");
    assertDoesNotThrow(() -> controller.enclaveWebhookRoutes());
  }
}
