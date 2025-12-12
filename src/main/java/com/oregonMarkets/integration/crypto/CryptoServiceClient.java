package com.oregonMarkets.integration.crypto;

import com.oregonMarkets.common.exception.ExternalServiceException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.common.util.DataMaskingUtil;
import com.oregonMarkets.integration.crypto.dto.CryptoServiceApiResponse;
import com.oregonMarkets.integration.crypto.dto.WalletCreateResponseData;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Client for interacting with the crypto-service API
 * Handles wallet creation, smart account management, and blockchain operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoServiceClient {

  @Value("${app.crypto-service.base-url}")
  private String cryptoServiceBaseUrl;

  private final WebClient.Builder webClientBuilder;

  /**
   * Create a Biconomy smart account for a user
   *
   * @param walletAddress User's EOA wallet address (from Magic)
   * @param didToken Magic DID token for authentication
   * @return Mono containing the wallet creation response
   */
  public Mono<WalletCreateResponseData> createSmartAccount(
      String walletAddress, String didToken) {

    WalletCreateRequest request = new WalletCreateRequest(walletAddress);
    String endpoint = cryptoServiceBaseUrl + "/wallets/create";

    log.info("[CRYPTO-SERVICE] Initiating smart account creation request");
    log.info("[CRYPTO-SERVICE] Endpoint: {}", endpoint);
    log.info("[CRYPTO-SERVICE] Request payload: walletAddress={}", DataMaskingUtil.maskWalletAddress(walletAddress));
    log.debug("[CRYPTO-SERVICE] Authentication configured");

    WebClient webClient =
        webClientBuilder
            .baseUrl(cryptoServiceBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    return webClient
        .post()
        .uri("/wallets/create")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + didToken)
        .bodyValue(request)
        .retrieve()
        .onStatus(
            status -> status.is4xxClientError(),
            response -> {
              log.error("[CRYPTO-SERVICE] Client error response: status={}", response.statusCode());
              return response.bodyToMono(String.class)
                  .flatMap(body -> {
                    String sanitized = DataMaskingUtil.sanitizeErrorBody(body);
                    log.error("[CRYPTO-SERVICE] Error response (sanitized): {}", sanitized);
                    return Mono.error(
                        new ExternalServiceException(
                            ResponseCode.EXTERNAL_SERVICE_ERROR,
                            "Crypto Service",
                            "Client error creating smart account - check logs for details"));
                  });
            })
        .onStatus(
            status -> status.is5xxServerError(),
            response -> {
              log.error("[CRYPTO-SERVICE] Server error response: status={}", response.statusCode());
              return response.bodyToMono(String.class)
                  .flatMap(body -> {
                    String sanitized = DataMaskingUtil.sanitizeErrorBody(body);
                    log.error("[CRYPTO-SERVICE] Error response (sanitized): {}", sanitized);
                    return Mono.error(
                        new ExternalServiceException(
                            ResponseCode.EXTERNAL_SERVICE_ERROR,
                            "Crypto Service",
                            "Server error creating smart account - check logs for details"));
                  });
            })
        .bodyToMono(
            new org.springframework.core.ParameterizedTypeReference<
                CryptoServiceApiResponse<WalletCreateResponseData>>() {})
        .doOnNext(response -> {
          log.info("[CRYPTO-SERVICE] Received response from crypto-service");
          log.debug("[CRYPTO-SERVICE] Response type: {}", DataMaskingUtil.maskResponseObject(response));
        })
        .map(CryptoServiceApiResponse::getData)
        .timeout(Duration.ofSeconds(30))
        .retryWhen(
            Retry.backoff(2, Duration.ofSeconds(1))
                .filter(throwable -> !(throwable instanceof ExternalServiceException))
                .doBeforeRetry(retrySignal ->
                    log.warn("[CRYPTO-SERVICE] Retrying smart account creation (attempt {}/2): {}",
                        retrySignal.totalRetries() + 1,
                        retrySignal.failure().getMessage())))
        .doOnSuccess(response -> {
          log.info("[CRYPTO-SERVICE] ✓ Successfully created smart account");
          log.info("[CRYPTO-SERVICE] Smart Account Address: {}",
              DataMaskingUtil.maskWalletAddress(response.getSmartAccount().getSmartAccountAddress()));
          log.info("[CRYPTO-SERVICE] Deployed: {}", response.getSmartAccount().getDeployed());
          log.info("[CRYPTO-SERVICE] Chain ID: {}", response.getSmartAccount().getChainId());
          log.debug("[CRYPTO-SERVICE] Bundler URL configured");
          log.debug("[CRYPTO-SERVICE] Paymaster URL configured");
        })
        .doOnError(error ->
            log.error("[CRYPTO-SERVICE] ✗ Failed to create smart account for wallet {}: {}",
                DataMaskingUtil.maskWalletAddress(walletAddress),
                error.getMessage(),
                error))
        .onErrorMap(throwable -> {
          if (throwable instanceof ExternalServiceException) {
            return throwable;
          }
          log.error("[CRYPTO-SERVICE] Mapping error to ExternalServiceException: {}",
              throwable.getClass().getSimpleName());
          return new ExternalServiceException(
              ResponseCode.EXTERNAL_SERVICE_ERROR,
              "Crypto Service",
              "Failed to create smart account: " + throwable.getMessage(),
              throwable);
        });
  }

  // Request DTO
  private record WalletCreateRequest(String walletAddress) {}
}
