package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.common.exception.BlnkApiException;
import com.oregonMarkets.config.BlnkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class BlnkClient {

    private final WebClient webClient;
    private final BlnkProperties blnkProperties;

    public BlnkClient(@Qualifier("blnkWebClient") WebClient webClient, BlnkProperties blnkProperties) {
        this.webClient = webClient;
        this.blnkProperties = blnkProperties;
    }
    
    public Mono<String> createIdentity(String userId, String email, Map<String, Object> metadata) {
        if (userId == null || userId.isEmpty()) {
            return Mono.error(new BlnkApiException("User ID is required for identity creation"));
        }
        if (email == null || email.isEmpty()) {
            return Mono.error(new BlnkApiException("Email is required for identity creation"));
        }
        if (metadata == null) {
            return Mono.error(new BlnkApiException("Metadata is required for identity creation"));
        }

        String firstName = email.split("@")[0];

        Map<String, Object> requestBody = Map.of(
            "identity_type", "individual",
            "first_name", firstName,
            "email", email,
            "meta_data", metadata
        );

        return webClient.post()
            .uri("/identities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Blnk createIdentity failed - Status: {}, Response: {}",
                            clientResponse.statusCode(), body);
                        return Mono.error(new BlnkApiException("Failed to create identity: " +
                            clientResponse.statusCode() + " - " + body));
                    }))
            .bodyToMono(Map.class)
            .map(response -> {
                Object idObj = response.get("identity_id");
                if (idObj == null) {
                    idObj = response.get("id");
                }
                if (idObj == null) {
                    throw new BlnkApiException("Identity ID not found in Blnk response: " + response);
                }
                return (String) idObj;
            })
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .maxBackoff(Duration.ofSeconds(2))
                    .doBeforeRetry(signal -> log.warn("Retrying createIdentity attempt {} for user {} - Error: {}",
                        signal.totalRetries() + 1, userId, signal.failure().getMessage())))
            .doOnSuccess(identityId -> log.info("Successfully created Blnk identity for user {}: {}", userId, identityId))
            .onErrorMap(error -> error instanceof BlnkApiException ? error :
                new BlnkApiException("Failed to create identity", error));
    }
    
    public Mono<String> createBalance(String currency) {
        if (currency == null || currency.isEmpty()) {
            return Mono.error(new BlnkApiException("Currency is required for balance creation"));
        }

        Map<String, Object> requestBody = Map.of(
            "ledger_id", blnkProperties.getLedgerId(),
            "currency", currency,
            "meta_data", Map.of("type", "user_balance")
        );

        return webClient.post()
            .uri("/balances")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Blnk createBalance failed - Status: {}, Response: {}",
                            clientResponse.statusCode(), body);
                        return Mono.error(new BlnkApiException("Failed to create balance: " +
                            clientResponse.statusCode() + " - " + body));
                    }))
            .bodyToMono(Map.class)
            .map(response -> {
                Object idObj = response.get("balance_id");
                if (idObj == null) {
                    idObj = response.get("id");
                }
                if (idObj == null) {
                    throw new BlnkApiException("Balance ID not found in Blnk response: " + response);
                }
                return (String) idObj;
            })
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .maxBackoff(Duration.ofSeconds(2))
                    .doBeforeRetry(signal -> log.warn("Retrying createBalance attempt {} for currency {} - Error: {}",
                        signal.totalRetries() + 1, currency, signal.failure().getMessage())))
            .doOnSuccess(balanceId -> log.info("Successfully created Blnk balance for currency {}: {}", currency, balanceId))
            .onErrorMap(error -> error instanceof BlnkApiException ? error :
                new BlnkApiException("Failed to create balance", error));
    }

    @SuppressWarnings("unchecked")
    public Mono<java.util.List<Map<String, Object>>> getBalancesByIdentity(String identityId) {
        if (identityId == null || identityId.isEmpty()) {
            return Mono.error(new BlnkApiException("Identity ID is required"));
        }

        return webClient.get()
            .uri("/balances?identity_id={identityId}", identityId)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Blnk getBalancesByIdentity failed - Status: {}, Response: {}",
                            clientResponse.statusCode(), body);
                        return Mono.error(new BlnkApiException("Failed to get balances: " +
                            clientResponse.statusCode() + " - " + body));
                    }))
            .bodyToMono(Map.class)
            .map(response -> {
                try {
                    Object dataObj = response.get("data");
                    if (dataObj instanceof java.util.List) {
                        return (java.util.List<Map<String, Object>>) dataObj;
                    }
                    return java.util.Collections.<Map<String, Object>>emptyList();
                } catch (Exception e) {
                    log.warn("Failed to parse balances response: {}", e.getMessage());
                    return java.util.Collections.<Map<String, Object>>emptyList();
                }
            })
            .doOnSuccess(balances -> log.info("Found {} balances for identity {}", balances.size(), identityId))
            .onErrorReturn(java.util.Collections.<Map<String, Object>>emptyList());
    }
}
