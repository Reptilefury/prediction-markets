package com.oregonmarkets.integration.keycloak;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
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
  private String  realm;

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
                .filter(throwable ->
                        throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException
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
                    .flatMap(body -> {
                          log.error("Keycloak createUser failed for {} - Status: {}, Response: {}", username, clientResponse.statusCode(), body);
                          return Mono.error(new RuntimeException("Failed to create Keycloak user: " + clientResponse.statusCode()));
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
        .uri(uriBuilder -> uriBuilder
                    .path("/admin/realms/{realm}/users")
                    .queryParam("username", username)
                    .build(realm))
        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                          log.error("Keycloak findUserIdByUsername failed - Status: {}, Response: {}", clientResponse.statusCode(), body);
                          return Mono.error(new RuntimeException("Failed to find user in Keycloak: " + clientResponse.statusCode()));
                        }))
        .bodyToFlux(Map.class)
        .next()
        .map(m -> String.valueOf(m.get("id")))
        .doOnError(e -> log.error("Error finding user by username {}: {}", username, e.getMessage()));
  }

  // ============================================
  // ADMIN USER MANAGEMENT METHODS
  // ============================================

  /**
   * Get all users in the realm
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getAllUsers() {
    return getAccessToken()
        .flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/users", realm)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  /**
   * Get user by ID
   */
  @SuppressWarnings("unchecked")
  public Mono<Map<String, Object>> getUserById(String userId) {
    return getAccessToken()
        .flatMap(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToMono(Map.class))
        .map(map -> (Map<String, Object>) map);
  }

  /**
   * Create admin user with full details
   */
  public Mono<String> createAdminUser(Map<String, Object> userRepresentation) {
    return getAccessToken()
        .flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRepresentation)
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                  // Extract user ID from Location header
                  String location = response.getHeaders().getLocation().toString();
                  return location.substring(location.lastIndexOf('/') + 1);
                }))
        .doOnSuccess(userId -> log.info("Created admin user with ID: {}", userId));
  }

  /**
   * Update user details
   */
  public Mono<Void> updateUser(String userId, Map<String, Object> userRepresentation) {
    return getAccessToken()
        .flatMap(token ->
            webClient.put()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRepresentation)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Updated user: {}", userId));
  }

  /**
   * Delete user
   */
  public Mono<Void> deleteUser(String userId) {
    return getAccessToken()
        .flatMap(token ->
            webClient.delete()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Deleted user: {}", userId));
  }

  // ============================================
  // ROLE MANAGEMENT METHODS
  // ============================================

  /**
   * Get all realm roles
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getRealmRoles() {
    return getAccessToken()
        .flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/roles", realm)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  /**
   * Get role by name
   */
  @SuppressWarnings("unchecked")
  public Mono<Map<String, Object>> getRoleByName(String roleName) {
    return getAccessToken().flatMap(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToMono(Map.class))
        .map(map -> (Map<String, Object>) map);
  }

  /**
   * Create a new realm role
   */
  public Mono<Void> createRealmRole(Map<String, Object> roleRepresentation) {
    return getAccessToken().flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/roles", realm)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(roleRepresentation)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Created realm role: {}", roleRepresentation.get("name")));
  }

  /**
   * Assign realm roles to user
   */
  public Mono<Void> assignRealmRolesToUser(String userId, java.util.List<Map<String, Object>> roles) {
    return getAccessToken()
        .flatMap(token -> webClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Assigned realm roles to user: {}", userId));
  }

  /**
   * Get user's realm role mappings
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getUserRealmRoles(String userId) {
    return getAccessToken().flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, userId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  // ============================================
  // CLIENT ROLE (PERMISSION) MANAGEMENT METHODS
  // ============================================

  /**
   * Get client by clientId
   */
  @SuppressWarnings("unchecked")
  public Mono<Map<String, Object>> getClientByClientId(String clientIdParam) {
    return getAccessToken()
        .flatMap(token ->
            webClient.get().uri(uriBuilder ->
                    uriBuilder.path("/admin/realms/{realm}/clients")
                        .queryParam("clientId", clientIdParam)
                        .build(realm))
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class)
                .next())
        .map(map -> (Map<String, Object>) map);
  }

  /**
   * Get all client roles (permissions) for a client
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getClientRoles(String clientUuid) {
    return getAccessToken().flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles", realm, clientUuid)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  /**
   * Create client role (permission)
   */
  public Mono<Void> createClientRole(String clientUuid, Map<String, Object> roleRepresentation) {
    return getAccessToken().flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles", realm, clientUuid)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(roleRepresentation)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Created client role: {}", roleRepresentation.get("name")));
  }

  /**
   * Assign client roles (permissions) to user
   */
  public Mono<Void> assignClientRolesToUser(String userId, String clientUuid, java.util.List<Map<String, Object>> roles) {
    return getAccessToken().flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}", realm, userId, clientUuid)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(roles)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Assigned client roles to user: {}", userId));
  }

  /**
   * Get user's client role mappings (permissions)
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getUserClientRoles(String userId, String clientUuid) {
    return getAccessToken().flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}", realm, userId, clientUuid)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  // ============================================
  // COMPOSITE ROLE (ROLE-PERMISSION) METHODS
  // ============================================

  /**
   * Get composite roles (permissions) for a realm role
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getRoleComposites(String roleName) {
    return getAccessToken().flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}/composites", realm, roleName)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  /**
   * Get client-level composite roles for a realm role
   */
  @SuppressWarnings("unchecked")
  public Mono<java.util.List<Map<String, Object>>> getRoleClientComposites(String roleName, String clientUuid) {
    return getAccessToken().flatMapMany(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}/composites/clients/{clientUuid}", realm, roleName, clientUuid)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToFlux(Map.class))
        .collectList()
        .map(list -> (java.util.List<Map<String, Object>>) (Object) list);
  }

  /**
   * Add composite roles (permissions) to a realm role
   * This makes the role a composite role containing client roles (permissions)
   */
  public Mono<Void> addRoleComposites(String roleName, java.util.List<Map<String, Object>> compositeRoles) {
    return getAccessToken().flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/roles/{roleName}/composites", realm, roleName)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(compositeRoles)
                .retrieve()
                .toBodilessEntity()
                .then())
        .doOnSuccess(v -> log.info("Added {} composite roles to role: {}", compositeRoles.size(), roleName));
  }

  /**
   * Get a specific client role by name
   */
  @SuppressWarnings("unchecked")
  public Mono<Map<String, Object>> getClientRoleByName(String clientUuid, String roleName) {
    return getAccessToken().flatMap(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}", realm, clientUuid, roleName)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .retrieve()
                .bodyToMono(Map.class))
        .map(map -> (Map<String, Object>) map);
  }

  /**
   * Update a realm role
   */
  public Mono<Void> updateRealmRole(String roleName, Map<String, Object> roleRepresentation) {
    return getAccessToken().flatMap(token ->
        webClient.put()
            .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(roleRepresentation)
            .retrieve()
            .toBodilessEntity()
            .then())
        .doOnSuccess(v -> log.info("Updated realm role: {}", roleName))
        .doOnError(e -> log.error("Error updating realm role {}: {}", roleName, e.getMessage()));
  }

  /**
   * Delete a realm role
   */
  public Mono<Void> deleteRealmRole(String roleName) {
    return getAccessToken().flatMap(token ->
        webClient.delete()
            .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .retrieve()
            .toBodilessEntity()
            .then())
        .doOnSuccess(v -> log.info("Deleted realm role: {}", roleName))
        .doOnError(e -> log.error("Error deleting realm role {}: {}", roleName, e.getMessage()));
  }

  /**
   * Remove composite roles from a realm role
   */
  public Mono<Void> removeRoleComposites(String roleName, java.util.List<Map<String, Object>> compositeRoles) {
    return getAccessToken().flatMap(token ->
        webClient.method(HttpMethod.DELETE)
            .uri("/admin/realms/{realm}/roles/{roleName}/composites", realm, roleName)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(compositeRoles)
            .retrieve()
            .toBodilessEntity()
            .then())
        .doOnSuccess(v -> log.info("Removed {} composite roles from role: {}", compositeRoles.size(), roleName))
        .doOnError(e -> log.error("Error removing composites from role {}: {}", roleName, e.getMessage()));
  }

  /**
   * Update a client role
   */
  public Mono<Void> updateClientRole(String clientUuid, String roleName, Map<String, Object> roleRepresentation) {
    return getAccessToken().flatMap(token ->
        webClient.put()
            .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}", realm, clientUuid, roleName)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(roleRepresentation)
            .retrieve()
            .toBodilessEntity()
            .then())
        .doOnSuccess(v -> log.info("Updated client role: {}", roleName))
        .doOnError(e -> log.error("Error updating client role {}: {}", roleName, e.getMessage()));
  }

  /**
   * Delete a client role
   */
  public Mono<Void> deleteClientRole(String clientUuid, String roleName) {
    return getAccessToken().flatMap(token ->
        webClient.delete()
            .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}", realm, clientUuid, roleName)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .retrieve()
            .toBodilessEntity()
            .then())
        .doOnSuccess(v -> log.info("Deleted client role: {}", roleName))
        .doOnError(e -> log.error("Error deleting client role {}: {}", roleName, e.getMessage()));
  }
}
