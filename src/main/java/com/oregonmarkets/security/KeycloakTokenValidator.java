package com.oregonmarkets.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonmarkets.common.exception.KeycloakAuthException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Keycloak token validator using JWT signature verification with JWKS.
 * This validates JWT tokens by verifying the signature against Keycloak's public keys.
 */
@Component
@Slf4j
public class KeycloakTokenValidator {

  @Value("${keycloak.admin.base-url}")
  private String keycloakBaseUrl;

  private final WebClient webClient;
  private final ObjectMapper mapper = new ObjectMapper();

  // Cache for JWKS (in production, use a proper cache with expiration)
  private Map<String, PublicKey> publicKeyCache = new HashMap<>();

  public KeycloakTokenValidator() {
    this.webClient = WebClient.builder().build();
  }

  /**
   * Validate a Keycloak JWT token using signature verification.
   * This properly validates the token signature against Keycloak's public keys.
   *
   * @param bearerToken The JWT token to validate
   * @return Mono containing user information if valid
   */
  public Mono<Map<String, Object>> validate(String bearerToken) {
    if (bearerToken == null || bearerToken.isBlank()) {
      return Mono.error(new KeycloakAuthException("Missing token"));
    }

    log.debug("Validating Keycloak JWT token using signature verification");

    return Mono.fromCallable(() -> {
      String[] parts = bearerToken.split("\\.");
      if (parts.length != 3) {
        throw new KeycloakAuthException("Invalid JWT format");
      }

      // Decode header and payload
      String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

      JsonNode header = mapper.readTree(headerJson);
      JsonNode payload = mapper.readTree(payloadJson);

      // Get key ID from header
      String kid = header.get("kid").asText();
      String alg = header.get("alg").asText();

      log.debug("Token kid: {}, alg: {}", kid, alg);

      // Verify expiration
      if (payload.has("exp")) {
        long exp = payload.get("exp").asLong();
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
          throw new KeycloakAuthException("Token expired",
              com.oregonmarkets.common.response.ResponseCode.TOKEN_EXPIRED);
        }
      }

      // Verify issuer (opm-admin realm)
      if (payload.has("iss")) {
        String issuer = payload.get("iss").asText();
        String expectedIssuer = keycloakBaseUrl + "/realms/opm-admin";
        if (!issuer.equals(expectedIssuer)) {
          log.warn("Invalid issuer: {} (expected: {})", issuer, expectedIssuer);
          throw new KeycloakAuthException("Invalid token issuer");
        }
      }

      return new String[]{kid, parts[0] + "." + parts[1], parts[2], payloadJson};
    })
    .subscribeOn(Schedulers.boundedElastic())
    .flatMap(data -> {
      String kid = data[0];
      String signedContent = data[1];
      String signature = data[2];
      String payloadJson = data[3];

      // Get public key and verify signature
      return getPublicKey(kid)
          .flatMap(publicKey -> verifySignature(signedContent, signature, publicKey))
          .flatMap(valid -> {
            if (!valid) {
              return Mono.error(new KeycloakAuthException("Invalid token signature"));
            }

            try {
              JsonNode payload = mapper.readTree(payloadJson);

              // Extract user information
              Map<String, Object> userInfo = new HashMap<>();
              if (payload.has("sub")) {
                userInfo.put("sub", payload.get("sub").asText());
              }
              if (payload.has("email")) {
                userInfo.put("email", payload.get("email").asText());
              }
              if (payload.has("preferred_username")) {
                userInfo.put("preferred_username", payload.get("preferred_username").asText());
              }
              if (payload.has("name")) {
                userInfo.put("name", payload.get("name").asText());
              }
              if (payload.has("given_name")) {
                userInfo.put("given_name", payload.get("given_name").asText());
              }
              if (payload.has("family_name")) {
                userInfo.put("family_name", payload.get("family_name").asText());
              }
              if (payload.has("realm_access")) {
                userInfo.put("realm_access", mapper.convertValue(payload.get("realm_access"), Map.class));
              }
              if (payload.has("resource_access")) {
                userInfo.put("resource_access", mapper.convertValue(payload.get("resource_access"), Map.class));
              }

              log.info("Token validated successfully for user: {}", userInfo.get("preferred_username"));
              return Mono.just(userInfo);
            } catch (Exception e) {
              return Mono.error(new KeycloakAuthException("Failed to parse token payload", e));
            }
          });
    })
    .onErrorResume(e -> {
      if (e instanceof KeycloakAuthException) {
        return Mono.error(e);
      }
      log.error("Error validating token: {}", e.getMessage(), e);
      return Mono.error(new KeycloakAuthException("Failed to validate token", e));
    });
  }

  /**
   * Get public key from JWKS endpoint
   */
  private Mono<PublicKey> getPublicKey(String kid) {
    // Check cache first
    if (publicKeyCache.containsKey(kid)) {
      log.debug("Using cached public key for kid: {}", kid);
      return Mono.just(publicKeyCache.get(kid));
    }

    // Fetch JWKS from Keycloak
    String jwksUrl = keycloakBaseUrl + "/realms/opm-admin/protocol/openid-connect/certs";

    return webClient.get()
        .uri(jwksUrl)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .flatMap(jwks -> {
          try {
            JsonNode keys = jwks.get("keys");
            for (JsonNode key : keys) {
              if (kid.equals(key.get("kid").asText())) {
                // Extract n and e for RSA public key
                String n = key.get("n").asText();
                String e = key.get("e").asText();

                PublicKey publicKey = createPublicKey(n, e);
                publicKeyCache.put(kid, publicKey);
                log.debug("Fetched and cached public key for kid: {}", kid);
                return Mono.just(publicKey);
              }
            }
            return Mono.error(new KeycloakAuthException("Public key not found for kid: " + kid));
          } catch (Exception ex) {
            return Mono.error(new KeycloakAuthException("Failed to parse JWKS", ex));
          }
        });
  }

  /**
   * Create RSA public key from modulus and exponent
   */
  private PublicKey createPublicKey(String modulusBase64, String exponentBase64) throws Exception {
    byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
    byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);

    BigInteger modulus = new BigInteger(1, modulusBytes);
    BigInteger exponent = new BigInteger(1, exponentBytes);

    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
    KeyFactory factory = KeyFactory.getInstance("RSA");
    return factory.generatePublic(spec);
  }

  /**
   * Verify JWT signature
   */
  private Mono<Boolean> verifySignature(String signedContent, String signatureBase64, PublicKey publicKey) {
    return Mono.fromCallable(() -> {
      try {
        byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureBase64);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(signedContent.getBytes(StandardCharsets.UTF_8));
        boolean valid = signature.verify(signatureBytes);
        log.debug("Signature verification result: {}", valid);
        return valid;
      } catch (Exception e) {
        log.error("Error verifying signature: {}", e.getMessage());
        return false;
      }
    }).subscribeOn(Schedulers.boundedElastic());
  }
}
