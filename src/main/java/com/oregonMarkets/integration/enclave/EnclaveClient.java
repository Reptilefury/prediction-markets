package com.oregonMarkets.integration.enclave;

import com.oregonMarkets.common.exception.EnclaveApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
public class EnclaveClient {
    
    private final WebClient webClient;
    
    @Value("${app.enclave.api-key}")
    private String apiKey;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    public EnclaveClient(@Qualifier("enclaveWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<EnclaveUDAResponse> createUDA(String userId, String email, String walletAddress) {
        Map<String, Object> requestBody = Map.of(
            "user_id", userId,
            "email", email,
            "wallet_address", walletAddress,
            "chains", Arrays.asList("ethereum", "polygon", "arbitrum", "optimism", "base"),
            "webhook_url", baseUrl + "/api/webhooks/enclave"
        );
        
        return webClient.post()
            .uri("/v1/users/uda")
            .header("X-API-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(EnclaveUDAResponse.class)
            .doOnSuccess(response -> log.info("Successfully created Enclave UDA for user: {}", userId))
            .onErrorMap(error -> new EnclaveApiException("Failed to create UDA", error));
    }
    
    public static class EnclaveUDAResponse {
        private String userId;
        private String udaAddress;
        private String tag;
        private Map<String, String> chainAddresses;
        private String status;
        private Instant createdAt;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUdaAddress() { return udaAddress; }
        public void setUdaAddress(String udaAddress) { this.udaAddress = udaAddress; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public Map<String, String> getChainAddresses() { return chainAddresses; }
        public void setChainAddresses(Map<String, String> chainAddresses) { this.chainAddresses = chainAddresses; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}