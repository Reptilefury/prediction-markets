package com.oregonmarkets.domain.market.router;

import com.oregonmarkets.domain.market.handler.AdminMarketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Router configuration for admin market view configuration endpoints
 */
@Configuration
public class AdminMarketRouter {

    @Bean
    public RouterFunction<ServerResponse> adminMarketRoutes(AdminMarketHandler handler) {
        return RouterFunctions.nest(path("/api/v1/admin/markets"),
                RouterFunctions
                        .route(GET("/{marketId}/view-preview"), handler::getMarketViewPreview)
                        .andRoute(PATCH("/{marketId}/view-config").and(accept(MediaType.APPLICATION_JSON)),
                                handler::updateMarketViewConfig)
                        .andRoute(PATCH("/{marketId}/featured-config").and(accept(MediaType.APPLICATION_JSON)),
                                handler::updateFeaturedMarketConfig)
        );
    }
}
