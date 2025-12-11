package com.oregonMarkets.integration.crypto;

import com.oregonMarkets.common.exception.ExternalServiceException;
import com.oregonMarkets.common.response.ResponseCode;
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
    log.info(
        "Creating smart account via crypto-service for wallet: {}",
        walletAddress);

    WebClient webClient =
        webClientBuilder
            .baseUrl(cryptoServiceBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    return webClient
        .post()
        .uri("/wallets/create")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + didToken)
        .bodyValue(new WalletCreateRequest(walletAddress))
        .retrieve()
        .bodyToMono(
            new org.springframework.core.ParameterizedTypeReference<
                CryptoServiceApiResponse<WalletCreateResponseData>>() {})
        .map(CryptoServiceApiResponse::getData)
        .timeout(Duration.ofSeconds(30))
        .retryWhen(
            Retry.backoff(2, Duration.ofSeconds(1))
                .filter(
                    throwable ->
                        !(throwable instanceof ExternalServiceException)))
        .doOnSuccess(
            response ->
                log.info(
                    "Successfully created smart account: {}",
                    response.getSmartAccount().getSmartAccountAddress()))
        .doOnError(
            error ->
                log.error(
                    "Failed to create smart account for wallet {}: {}",
                    walletAddress,
                    error.getMessage()))
        .onErrorMap(
            throwable -> {
              if (throwable instanceof ExternalServiceException) {
                return throwable;
              }
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
