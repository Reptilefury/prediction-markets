package com.oregonmarkets.domain.market.router;

import com.oregonmarkets.domain.market.handler.ViewTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Router configuration for view template admin endpoints
 */
@Configuration
public class ViewTemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> adminViewTemplateRoutes(ViewTemplateHandler handler) {
        return RouterFunctions.nest(path("/api/v1/admin/view-templates"),
                RouterFunctions.route(GET(""), handler::getAllViewTemplates)
        );
    }
}
