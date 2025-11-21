package com.oregonMarkets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean("magicWebClient")
    public WebClient magicWebClient(@Value("${app.magic.api-url}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
    
    @Bean("enclaveWebClient")
    public WebClient enclaveWebClient(@Value("${app.enclave.api-url}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
    
    @Bean("blnkWebClient")
    public WebClient blnkWebClient(@Value("${app.blnk.api-url}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Bean("keycloakAdminWebClient")
    public WebClient keycloakAdminWebClient(@Value("${keycloak.admin.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}