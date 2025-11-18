package com.oregonMarkets.integration.blnk;

import com.oregonMarkets.common.exception.BlnkApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Component
@Slf4j
public class BlnkClient {
    
    private final WebClient webClient;
    
    public BlnkClient(@Qualifier("blnkWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<String> createIdentity(String userId, String email, Map<String, Object> metadata) {
        Map<String, Object> requestBody = Map.of(
            "identity_type", "individual",
            "first_name", email.split("@")[0],
            "email", email,
            "meta_data", metadata
        );
        
        return webClient.post()
            .uri("/identities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (String) response.get("identity_id"))
            .doOnSuccess(identityId -> log.info("Successfully created Blnk identity for user {}: {}", userId, identityId))
            .onErrorMap(error -> new BlnkApiException("Failed to create identity", error));
    }
    
    public Mono<String> createAccount(String identityId, String currency, String accountName) {
        Map<String, Object> requestBody = Map.of(
            "ledger_id", "oregon_markets",
            "identity_id", identityId,
            "currency", currency,
            "account_name", accountName,
            "meta_data", Map.of("type", "user_balance")
        );
        
        return webClient.post()
            .uri("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (String) response.get("account_id"))
            .doOnSuccess(accountId -> log.info("Successfully created Blnk account: {}", accountId))
            .onErrorMap(error -> new BlnkApiException("Failed to create account", error));
    }
}