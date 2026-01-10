package com.oregonmarkets.domain.market.router;

import com.oregonmarkets.domain.market.handler.MarketTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class MarketTypeRouter {

    @Bean
    public RouterFunction<ServerResponse> adminMarketTypeRoutes(MarketTypeHandler handler) {
        return RouterFunctions.nest(path("/api/v1/admin/market-types"),
            RouterFunctions.route(GET(""), handler::getAllMarketTypes)
        );
    }
}
