package com.oregonmarkets.domain.user.controller;

import com.oregonmarkets.domain.user.service.IUserRegistrationService;
import com.oregonmarkets.domain.user.service.Web3RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AuthRouterConfigTest {

    @Mock
    private IUserRegistrationService userRegistrationService;
    
    @Mock
    private Web3RegistrationService web3RegistrationService;

    private AuthRouterConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthRouterConfig(userRegistrationService, web3RegistrationService);
    }

    @Test
    void shouldCreateAuthRoutes() {
        RouterFunction<ServerResponse> routes = config.authRoutes();
        assertNotNull(routes);
    }

    @Test
    void shouldHaveCorrectDependencies() {
        assertNotNull(config);
    }
}
