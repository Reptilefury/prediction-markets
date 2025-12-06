package com.oregonMarkets.integration.keycloak;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class KeycloakAdminClient {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final WebClient webClient;

  public KeycloakAdminClient(@Qualifier("keycloakAdminWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  @Value("${keycloak.admin.realm}")
  private String realm;

  @Value("${keycloak.admin.client-id:}")
  private String clientId;

  @Value("${keycloak.admin.client-secret:}")
  private String clientSecret;

  @Value("${keycloak.admin.base-url}")
  private String baseUrl;

  private Mono<String> getAccessToken() {
    // Authenticate to master realm where admin-cli client exists
    // Use Resource Owner Password Credentials grant with admin credentials
    return WebClient.create(baseUrl)
        .post()
        .uri("/realms/master/protocol/openid-connect/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue("grant_type=password&username=admin&password=admin123&client_id=admin-cli")
        .retrieve()
        .onStatus(
            status -> !status.is2xxSuccessful(),
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(
                              "Keycloak token request failed - Status: {}, Response: {}",
                              clientResponse.statusCode(),
                              body);
                          return Mono.error(
                              new RuntimeException(
                                  "Failed to get Keycloak access token: "
                                      + clientResponse.statusCode()));
                        }))
        .bodyToMono(Map.class)
        .map(response -> (String) response.get("access_token"))
        .retryWhen(
            Retry.backoff(3, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(10))
                .jitter(0.5)
                .filter(
                    throwable ->
                        throwable
                                instanceof
                                org.springframework.web.reactive.function.client
                                    .WebClientRequestException
                            || throwable.getCause()
                                instanceof reactor.netty.http.client.PrematureCloseException
                            || throwable.getMessage().contains("Connection prematurely closed")
                            || throwable.getMessage().contains("PrematureCloseException")));
  }

  public Mono<Void> createUserIfAbsent(String username, String password) {
    return createUserIfAbsent(username, password, null);
  }

  public Mono<Void> createUserIfAbsent(String username, String password, String email) {
    // Get access token first, then try to create user; if 409, ignore; then set password
    // username is the Magic user ID (sub field from DID token)
    // Sanitize the username by removing base64 padding characters (=) which are invalid in Keycloak
    // usernames
    String sanitizedUsername = username.replace("=", "");

    return getAccessToken()
        .flatMap(token -> createUser(sanitizedUsername, email, token))
        .onErrorResume(
            ex -> {
              // If already exists, proceed
              log.debug("createUser error (possibly exists): {}", ex.getMessage());
              return Mono.empty();
            })
        .then(getAccessToken().flatMap(token -> setPassword(sanitizedUsername, password, token)))
        .retryWhen(
            Retry.backoff(3, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(10))
                .jitter(0.5)
                .filter(
                    throwable ->
                        throwable
                                instanceof
                                org.springframework.web.reactive.function.client
                                    .WebClientRequestException
                            || throwable.getCause()
                                instanceof reactor.netty.http.client.PrematureCloseException
                            || throwable.getMessage().contains("Connection prematurely closed")
                            || throwable.getMessage().contains("PrematureCloseException")))
        .doOnSuccess(v -> log.info("Keycloak user ensured for {}", sanitizedUsername));
  }

  private Mono<Void> createUser(String username, String email, String accessToken) {
    // username is the Magic user ID (sub field from DID token)
    // Remove base64 padding characters (=) which are invalid in Keycloak usernames
    String sanitizedUsername = username.replace("=", "");

    // Create user payload with email if provided
    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("username", sanitizedUsername);
    payload.put("enabled", true);

    if (email != null && !email.isEmpty()) {
      payload.put("email", email);
      payload.put("emailVerified", true);
    }

    return webClient
        .post()
        .uri(uriBuilder -> uriBuilder.path("/admin/realms/{realm}/users").build(realm))
        .contentType(MediaType.APPLICATION_JSON)
        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
        .bodyValue(payload)
        .retrieve()
        .onStatus(
            status -> !status.is2xxSuccessful() && status.value() != 409,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(
                              "Keycloak createUser failed for {} - Status: {}, Response: {}",
                              username,
                              clientResponse.statusCode(),
                              body);
                          return Mono.error(
                              new RuntimeException(
                                  "Failed to create Keycloak user: "
                                      + clientResponse.statusCode()));
                        }))
        .toBodilessEntity()
        .then()
        .doOnSuccess(v -> log.info("Keycloak user created for {}", username));
  }

  public Mono<Void> setPassword(String username, String password, String accessToken) {
    // Find user by username (Magic user ID), then set password
    return findUserIdByUsername(username, accessToken)
        .flatMap(
            userId ->
                webClient
                    .put()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/admin/realms/{realm}/users/{id}/reset-password")
                                .build(realm, userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .bodyValue(Map.of("type", "password", "value", password, "temporary", false))
                    .retrieve()
                    .toBodilessEntity()
                    .then())
        .doOnSuccess(v -> log.info("Keycloak password updated for {}", username))
        .doOnError(
            e ->
                log.warn(
                    "Failed to update Keycloak password for {}: {}", username, e.getMessage()));
  }

  private Mono<String> findUserIdByUsername(String username, String accessToken) {
    // username is the Magic user ID (sub field from DID token)
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/admin/realms/{realm}/users")
                    .queryParam("username", username)
                    .build(realm))
        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
        .retrieve()
        .onStatus(
            status -> !status.is2xxSuccessful(),
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(
                              "Keycloak findUserIdByUsername failed - Status: {}, Response: {}",
                              clientResponse.statusCode(),
                              body);
                          return Mono.error(
                              new RuntimeException(
                                  "Failed to find user in Keycloak: "
                                      + clientResponse.statusCode()));
                        }))
        .bodyToFlux(Map.class)
        .next()
        .map(m -> String.valueOf(m.get("id")))
        .doOnError(
            e -> log.error("Error finding user by username {}: {}", username, e.getMessage()));
  }
}
