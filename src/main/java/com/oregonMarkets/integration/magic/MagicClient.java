package com.oregonMarkets.integration.magic;

import com.oregonMarkets.common.exception.MagicAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class MagicClient {
    
    private final WebClient webClient;
    
    @Value("${app.magic.api-key}")
    private String apiKey;
    
    public MagicClient(@Qualifier("magicWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<MagicUserInfo> validateDIDToken(String didToken) {
        Map<String, Object> requestBody = Map.of("didToken", didToken);
        
        return webClient.post()
            .uri("/admin/auth/user/get")
            .header("X-Magic-Secret-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(MagicUserInfo.class)
            .doOnSuccess(userInfo -> log.info("Successfully validated DID token for user: {}", userInfo.getEmail()))
            .onErrorMap(error -> new MagicAuthException("Invalid or expired DID token", error));
    }
    
    public static class MagicUserInfo {
        private String issuer;
        private String email;
        private String publicAddress;
        
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPublicAddress() { return publicAddress; }
        public void setPublicAddress(String publicAddress) { this.publicAddress = publicAddress; }
    }
}