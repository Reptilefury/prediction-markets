package com.oregonMarkets.domain.user.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.oregonMarkets.domain.user.service.UserRegistrationService;
import com.oregonMarkets.domain.user.service.Web3RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthRouterConfigTest {

  @Mock private UserRegistrationService userRegistrationService;

  @Mock private Web3RegistrationService web3RegistrationService;

  @InjectMocks private AuthRouterConfig authRouterConfig;

  @Test
  void constructor_WithValidDependencies_CreatesInstance() {
    assertNotNull(authRouterConfig);
  }

  @Test
  void authRoutes_ShouldReturnRouterFunction() {
    var routes = authRouterConfig.authRoutes();
    assertNotNull(routes);
  }
}
