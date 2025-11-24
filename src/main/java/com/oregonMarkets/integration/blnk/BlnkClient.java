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
                return (String) idObj;
            })
            .doOnSuccess(identityId -> log.info("Successfully created Blnk identity for user {}: {}", userId, identityId))
            .onErrorMap(error -> error instanceof BlnkApiException ? error :
                new BlnkApiException("Failed to create identity", error));
    }
    
    public Mono<String> createAccount(String identityId, String currency, String accountName) {
        if (identityId == null || identityId.isEmpty()) {
            return Mono.error(new BlnkApiException("Identity ID is required for account creation"));
        }
        if (currency == null || currency.isEmpty()) {
            return Mono.error(new BlnkApiException("Currency is required for account creation"));
        }
        if (accountName == null || accountName.isEmpty()) {
            return Mono.error(new BlnkApiException("Account name is required for account creation"));
        }

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
            .onStatus(status -> !status.is2xxSuccessful(),
                clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Blnk createAccount failed - Status: {}, Response: {}",
                            clientResponse.statusCode(), body);
                        return Mono.error(new BlnkApiException("Failed to create account: " +
                            clientResponse.statusCode() + " - " + body));
                    }))
            .bodyToMono(Map.class)
            .map(response -> {
                Object idObj = response.get("account_id");
                if (idObj == null) {
                    idObj = response.get("id");
                }
                return (String) idObj;
            })
            .doOnSuccess(accountId -> log.info("Successfully created Blnk account: {}", accountId))
            .onErrorMap(error -> error instanceof BlnkApiException ? error :
                new BlnkApiException("Failed to create account", error));
    }
}