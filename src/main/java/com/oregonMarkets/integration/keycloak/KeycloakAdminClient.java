package com.oregonMarkets.integration.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class KeycloakAdminClient {

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

    // In a real setup you'd acquire an admin access token; for brevity we assume baseUrl is secured appropriately

    public Mono<Void> createUserIfAbsent(String email, String password) {
        // Minimal flow: try to create; if 409, ignore; then set password
        return createUser(email)
                .onErrorResume(ex -> {
                    // If already exists, proceed
                    log.debug("createUser error (possibly exists): {}", ex.getMessage());
                    return Mono.empty();
                })
                .then(setPassword(email, password))
                .doOnSuccess(v -> log.info("Keycloak user ensured for {}", email));
    }

    private Mono<Void> createUser(String email) {
        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "enabled", true
        );
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/admin/realms/{realm}/users").build(realm))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> setPassword(String email, String password) {
        // Find user by email, then set password
        return findUserIdByEmail(email)
                .flatMap(userId -> webClient.put()
                        .uri(uriBuilder -> uriBuilder
                                .path("/admin/realms/{realm}/users/{id}/reset-password")
                                .build(realm, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        ))
                        .retrieve()
                        .toBodilessEntity()
                        .then())
                .doOnSuccess(v -> log.info("Keycloak password updated for {}", email))
                .doOnError(e -> log.warn("Failed to update Keycloak password for {}: {}", email, e.getMessage()));
    }

    private Mono<String> findUserIdByEmail(String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("email", email)
                        .build(realm))
                .retrieve()
                .bodyToFlux(Map.class)
                .next()
                .map(m -> String.valueOf(m.get("id")));
    }
}
