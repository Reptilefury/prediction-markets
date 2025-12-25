package com.oregonmarkets.integration.magic;

import com.oregonmarkets.common.exception.MagicAuthException;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MagicClient {

  private final WebClient webClient;

  @Value("${app.magic.api-key}")
  private String apiKey;

  @PostConstruct
  public void init() {
    log.info(
        "Magic API Key loaded: {}... (length: {})",
        apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "null",
        apiKey != null ? apiKey.length() : 0);
  }

  public MagicClient(@Qualifier("magicWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<MagicUserInfo> validateDIDToken(String didToken) {
    // Log API key details
    log.info(
        "API Key check - starts with sk_: {}, length: {}",
        apiKey != null && apiKey.startsWith("sk_"),
        apiKey != null ? apiKey.length() : 0);

    Map<String, Object> requestBody = Map.of("didToken", didToken);

    log.info("Making Magic API request to: https://api.magic.link/admin/auth/user/get");
    log.info(
        "Headers: X-Magic-Secret-Key: {}..., User-Agent: OregonMarkets/1.0",
        apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "null");
    log.info("Request body: {}", requestBody);

    return webClient
        .post()
        .uri("/admin/auth/user/get")
        .header("X-Magic-Secret-Key", apiKey)
        .header("User-Agent", "OregonMarkets/1.0")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(
            status -> status.is4xxClientError() || status.is5xxServerError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .doOnNext(
                        body ->
                            log.error(
                                "Magic API error response: Status={}, Body={}",
                                response.statusCode(),
                                body))
                    .then(
                        Mono.error(
                            new MagicAuthException("Magic API returned " + response.statusCode()))))
        .bodyToMono(MagicUserInfo.class)
        .doOnSuccess(
            userInfo ->
                log.info("Successfully validated DID token for user: {}", userInfo.getEmail()))
        .onErrorMap(
            error -> {
              if (error instanceof MagicAuthException) return error;
              return new MagicAuthException(
                  "Invalid or expired DID token " + error.getMessage(), error);
            });
  }

  public static class MagicUserInfo {
    private String issuer;
    private String email;
    private String publicAddress;

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      this.issuer = issuer;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPublicAddress() {
      return publicAddress;
    }

    public void setPublicAddress(String publicAddress) {
      this.publicAddress = publicAddress;
    }
  }
}
