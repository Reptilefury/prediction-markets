package com.oregonmarkets.domain.user.controller;

import com.oregonmarkets.common.exception.MagicAuthException;
import com.oregonmarkets.common.exception.UserNotFoundException;
import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonmarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonmarkets.domain.user.service.IUserRegistrationService;
import com.oregonmarkets.domain.user.service.Web3RegistrationService;
import com.oregonmarkets.integration.magic.MagicDIDValidator;
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

  private final IUserRegistrationService userRegistrationService;
  private final Web3RegistrationService web3RegistrationService;

  @Bean
  public RouterFunction<ServerResponse> authRoutes() {
    return RouterFunctions.route()
        .POST("/api/auth/register", this::register)
        .POST("/api/auth/register/web3", this::registerWeb3)
        .GET("/api/user/profile", this::getUserProfile)
        .build();
  }

  private Mono<ServerResponse> register(ServerRequest request) {
    log.info("Received registration request");
    return request
        .bodyToMono(UserRegistrationRequest.class)
        .doOnNext(req -> log.info("Registration request received for email: {}", req.getEmail()))
        .flatMap(
            req -> {
              var exchange = request.exchange();
              MagicDIDValidator.MagicUserInfo magicUser = exchange.getAttribute("magicUser");
              String didToken = exchange.getAttribute("magicToken");
              if (magicUser == null || didToken == null) {
                return Mono.error(
                    new MagicAuthException(
                        "Missing Magic DID token validation - registration requires Magic authentication"));
              }
              return userRegistrationService.registerUser(req, magicUser, didToken);
            })
        .map(response -> ApiResponse.success(ResponseCode.USER_REGISTERED, response))
        .flatMap(
            apiResponse ->
                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(apiResponse));
  }

  private Mono<ServerResponse> registerWeb3(ServerRequest request) {
    return request
        .bodyToMono(Web3RegistrationRequest.class)
        .doOnNext(
            req ->
                log.info(
                    "Web3 registration request received for wallet: {}", req.getWalletAddress()))
        .flatMap(web3RegistrationService::registerUser)
        .map(response -> ApiResponse.success(ResponseCode.USER_REGISTERED, response))
        .flatMap(
            apiResponse ->
                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(apiResponse));
  }

  private Mono<ServerResponse> getUserProfile(ServerRequest request) {
    log.info("Received user profile request");
    var exchange = request.exchange();
    MagicDIDValidator.MagicUserInfo magicUser = exchange.getAttribute("magicUser");
    
    if (magicUser == null) {
      return ServerResponse.status(401)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(ApiResponse.error(ResponseCode.UNAUTHORIZED, "Authentication required"));
    }
    
    return userRegistrationService.getUserProfile(magicUser)
        .map(userProfile -> ApiResponse.success(ResponseCode.USER_PROFILE_RETRIEVED, userProfile))
        .flatMap(apiResponse -> 
            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(apiResponse))
        .onErrorResume(UserNotFoundException.class, e -> 
            ServerResponse.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiResponse.error(ResponseCode.USER_NOT_FOUND, e.getMessage())))
        .onErrorResume(Exception.class, e -> {
            log.error("Error retrieving user profile", e);
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "Failed to retrieve user profile"));
        });
  }
}
