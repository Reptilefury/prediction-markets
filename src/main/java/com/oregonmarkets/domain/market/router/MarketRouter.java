package com.oregonmarkets.domain.market.router;

import com.oregonmarkets.domain.market.handler.MarketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Router configuration for market API endpoints
 */
@Configuration
public class MarketRouter {

    private static final String BASE_PATH = "/api/v1/markets";

    @Bean
    public RouterFunction<ServerResponse> marketRoutes(MarketHandler handler) {
        return RouterFunctions.nest(path(BASE_PATH),
            RouterFunctions
                .route(POST("").and(accept(MediaType.APPLICATION_JSON)), handler::createMarket)
                .andRoute(GET(""), request -> {
                    if (request.queryParam("category").isPresent()) {
                        return handler.getMarketsByCategory(request);
                    } else if (request.queryParam("status").isPresent()) {
                        return handler.getMarketsByStatus(request);
                    } else {
                        return handler.getAllMarkets(request);
                    }
                })
                .andRoute(GET("/featured"), handler::getFeaturedMarkets)
                .andRoute(GET("/trending"), handler::getTrendingMarkets)
                .andRoute(GET("/search"), handler::searchMarkets)
                .andRoute(GET("/slug/{slug}"), handler::getMarketBySlug)
                .andRoute(GET("/{marketId}"), handler::getMarketById)
                .andRoute(PUT("/{marketId}").and(accept(MediaType.APPLICATION_JSON)), handler::updateMarket)
                .andRoute(DELETE("/{marketId}"), handler::cancelMarket)
                .andRoute(POST("/{marketId}/resolve").and(accept(MediaType.APPLICATION_JSON)), handler::resolveMarket)
                .andRoute(POST("/{marketId}/close"), handler::closeMarket)
                .andRoute(POST("/{marketId}/suspend"), handler::suspendMarket)
                .andRoute(POST("/{marketId}/reopen"), handler::reopenMarket)
                .andRoute(GET("/{marketId}/outcomes"), handler::getMarketOutcomes)
        );
    }
}
