package com.oregonMarkets.domain.user.controller;

import com.oregonMarkets.common.exception.MagicAuthException;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonMarkets.domain.user.service.UserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import com.oregonMarkets.integration.magic.MagicClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthRouterConfig {

    private final UserRegistrationService userRegistrationService;
    private final Web3RegistrationService web3RegistrationService;

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return RouterFunctions.route()
                .POST("/api/auth/register", this::register)
                .POST("/api/auth/register/web3", this::registerWeb3)
                .build();
    }

    private Mono<ServerResponse> register(ServerRequest request) {
        log.info("Received registration request");
        return request.bodyToMono(UserRegistrationRequest.class)
                .doOnNext(req -> log.info("Registration request received for email: {}", req.getEmail()))
                .flatMap(req -> {
                    var exchange = request.exchange();
                    MagicClient.MagicUserInfo magicUser = exchange.getAttribute("magicUser");
                    String didToken = exchange.getAttribute("magicToken");
                    if (magicUser == null || didToken == null) {
                        return Mono.error(new MagicAuthException("Missing Magic authentication context"));
                    }
                    return userRegistrationService.registerUser(req, magicUser, didToken);
                })
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    private Mono<ServerResponse> registerWeb3(ServerRequest request) {
        return request.bodyToMono(Web3RegistrationRequest.class)
                .doOnNext(req -> log.info("Web3 registration request received for wallet: {}", req.getWalletAddress()))
                .flatMap(web3RegistrationService::registerUser)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }
}