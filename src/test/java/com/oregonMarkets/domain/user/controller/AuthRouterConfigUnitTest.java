package com.oregonMarkets.domain.user.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonMarkets.domain.user.service.UserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@ExtendWith(MockitoExtension.class)
class AuthRouterConfigUnitTest {

  @Mock private UserRegistrationService userRegistrationService;

  @Mock private Web3RegistrationService web3RegistrationService;

  private AuthRouterConfig authRouterConfig;

  @BeforeEach
  void setUp() {
    authRouterConfig = new AuthRouterConfig(userRegistrationService, web3RegistrationService);
  }

  @Test
  void authRoutes_CreatesRouterFunction() {
    RouterFunction<ServerResponse> routerFunction = authRouterConfig.authRoutes();

    assertNotNull(routerFunction);
  }

  @Test
  void constructor_InitializesServices() {
    AuthRouterConfig config =
        new AuthRouterConfig(userRegistrationService, web3RegistrationService);

    assertNotNull(config);
  }
}
