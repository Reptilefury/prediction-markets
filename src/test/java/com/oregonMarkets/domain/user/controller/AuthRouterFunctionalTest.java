package com.oregonMarkets.domain.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.oregonMarkets.common.exception.UserNotFoundException;
import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.request.Web3RegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.domain.user.service.IUserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

class AuthRouterFunctionalTest {

  @Mock private IUserRegistrationService userRegistrationService;
  @Mock private Web3RegistrationService web3RegistrationService;

  private WebTestClient clientWithMagic;
  private WebTestClient clientWithoutMagic;
  private MagicDIDValidator.MagicUserInfo magicUserInfo;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    AuthRouterConfig config = new AuthRouterConfig(userRegistrationService, web3RegistrationService);
    RouterFunction<ServerResponse> routerFunction = config.authRoutes();
    magicUserInfo = new MagicDIDValidator.MagicUserInfo();
    magicUserInfo.setEmail("magic@example.com");
    magicUserInfo.setUserId("magic-user");
    magicUserInfo.setPublicAddress("0x123");

    clientWithMagic =
        WebTestClient.bindToRouterFunction(routerFunction)
            .webFilter(
                (exchange, chain) -> {
                  exchange.getAttributes().put("magicUser", magicUserInfo);
                  exchange.getAttributes().put("magicToken", "test-token");
                  return chain.filter(exchange);
                })
            .configureClient()
            .baseUrl("/")
            .build();

    clientWithoutMagic =
        WebTestClient.bindToRouterFunction(routerFunction).configureClient().baseUrl("/").build();
  }

  @Test
  void register_WithMagicUser_ReturnsSuccess() {
    UserRegistrationResponse response =
        UserRegistrationResponse.builder()
            .userId(UUID.randomUUID())
            .email("magic@example.com")
            .build();
    when(userRegistrationService.registerUser(any(), eq(magicUserInfo), anyString()))
        .thenReturn(Mono.just(response));

    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("magic@example.com");
    request.setCountryCode("US");

    clientWithMagic
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ApiResponse.class)
        .value(
            apiResponse ->
                org.junit.jupiter.api.Assertions.assertEquals(
                    ResponseCode.USER_REGISTERED.getCode(), apiResponse.getCode()));
  }

  @Test
  void register_MissingMagicUser_ReturnsError() {
    UserRegistrationRequest request = new UserRegistrationRequest();
    request.setEmail("missing@example.com");
    request.setCountryCode("US");

    clientWithoutMagic
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void registerWeb3_ReturnsSuccess() {
    Web3RegistrationRequest request = new Web3RegistrationRequest();
    request.setWalletAddress("0x1234567890123456789012345678901234567890");
    request.setMessage("Sign this message to authenticate with Oregon Markets");
    request.setSignature(
        "0x123456789012345678901234567890123456789012345678901234567890123456789012345678");
    when(web3RegistrationService.registerUser(any(Web3RegistrationRequest.class)))
        .thenReturn(
            Mono.just(
                UserRegistrationResponse.builder()
                    .userId(UUID.randomUUID())
                    .magicWalletAddress("0x1234")
                    .build()));

    clientWithoutMagic
        .post()
        .uri("/api/auth/register/web3")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void getUserProfile_WithMagicUser_ReturnsProfile() {
    when(userRegistrationService.getUserProfile(magicUserInfo))
        .thenReturn(
            Mono.just(
                UserRegistrationResponse.builder()
                    .userId(UUID.randomUUID())
                    .email("magic@example.com")
                    .build()));

    clientWithMagic
        .get()
        .uri("/api/user/profile")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ApiResponse.class)
        .value(
            response ->
                org.junit.jupiter.api.Assertions.assertEquals(
                    ResponseCode.USER_PROFILE_RETRIEVED.getCode(), response.getCode()));
  }

  @Test
  void getUserProfile_UserNotFound_Returns404() {
    when(userRegistrationService.getUserProfile(magicUserInfo))
        .thenReturn(Mono.error(new UserNotFoundException("missing user")));

    clientWithMagic.get().uri("/api/user/profile").exchange().expectStatus().isNotFound();
  }

  @Test
  void getUserProfile_NoMagicUser_Returns401() {
    clientWithoutMagic.get().uri("/api/user/profile").exchange().expectStatus().isUnauthorized();
  }
}
