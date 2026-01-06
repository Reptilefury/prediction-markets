package com.oregonmarkets.domain.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class AdminUserRouter {

    private final AdminUserHandler adminUserHandler;

    @Bean
    public RouterFunction<ServerResponse> adminUserRoutes() {
        return route()
            .path("/api/admin/users", builder -> builder
                .POST("", adminUserHandler::createAdminUser)
                .GET("", adminUserHandler::getAllAdminUsers)
                .GET("/{id}", adminUserHandler::getAdminUser)
                .PUT("/{id}", adminUserHandler::updateAdminUser)
                .DELETE("/{id}", adminUserHandler::deleteAdminUser)
                .POST("/{id}/login", adminUserHandler::updateLastLogin)
            )
            .build();
    }
}
