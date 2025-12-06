package com.oregonMarkets.domain.enclave.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EnclaveWebhookController {

  @Bean
  public RouterFunction<ServerResponse> enclaveWebhookRoutes() {
    return RouterFunctions.route()
        .POST("/api/webhooks/enclave", this::handleEnclaveWebhook)
        .build();
  }

  private Mono<ServerResponse> handleEnclaveWebhook(ServerRequest request) {
    return request
        .bodyToMono(Map.class)
        .doOnNext(payload -> log.info("Received Enclave webhook: {}", payload))
        .flatMap(
            payload -> {
              String eventType = (String) payload.get("event_type");

              switch (eventType) {
                case "deposit.detected":
                  return handleDepositDetected(payload);
                case "deposit.confirmed":
                  return handleDepositConfirmed(payload);
                default:
                  log.warn("Unknown Enclave event type: {}", eventType);
                  return ServerResponse.ok().build();
              }
            })
        .onErrorResume(
            error -> {
              log.error("Error processing Enclave webhook: {}", error.toString());
              return ServerResponse.badRequest().build();
            });
  }

  private Mono<ServerResponse> handleDepositDetected(Map<String, Object> payload) {
    log.info("Processing deposit detected event");
    return ServerResponse.ok().build();
  }

  private Mono<ServerResponse> handleDepositConfirmed(Map<String, Object> payload) {
    log.info("Processing deposit confirmed event");
    return ServerResponse.ok().build();
  }
}
