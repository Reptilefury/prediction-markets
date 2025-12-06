package com.oregonMarkets.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.KeycloakAuthException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Minimal Keycloak token validator. NOTE: This does not perform signature verification. For
 * production, replace with proper JWT verification against the realm's JWKS or introspection.
 */
@Component
public class KeycloakTokenValidator {

  private static final String PREFERRED_USERNAME = "preferred_username";
  private static final String EMAIL = "email";
  private final ObjectMapper mapper = new ObjectMapper();

  public Mono<Map<String, Object>> validate(String bearerToken) {
    if (bearerToken == null || bearerToken.isBlank()) {
      return Mono.error(new KeycloakAuthException("Missing token"));
    }
    String[] parts = bearerToken.split("\\.");
    if (parts.length < 2) {
      return Mono.error(new KeycloakAuthException("Invalid JWT format"));
    }
    try {
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      JsonNode payload = mapper.readTree(payloadJson);
      // Optional exp check
      if (payload.has("exp")) {
        long exp = payload.get("exp").asLong();
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
          return Mono.error(new KeycloakAuthException("Token expired"));
        }
      }
      Map<String, Object> userInfo = new HashMap<>();
      if (payload.has("sub")) userInfo.put("sub", payload.get("sub").asText());
      if (payload.has(EMAIL)) userInfo.put(EMAIL, payload.get(EMAIL).asText());
      if (payload.has(PREFERRED_USERNAME))
        userInfo.put(PREFERRED_USERNAME, payload.get(PREFERRED_USERNAME).asText());
      return Mono.just(userInfo);
    } catch (IllegalArgumentException e) {
      return Mono.error(new KeycloakAuthException("Invalid base64 in JWT", e));
    } catch (Exception e) {
      return Mono.error(new KeycloakAuthException("Failed to parse JWT", e));
    }
  }
}
