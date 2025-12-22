package com.oregonMarkets.domain.market.router;

import com.oregonMarkets.domain.market.handler.CategoryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Router configuration for category API endpoints
 * Includes both public and admin endpoints
 */
@Configuration
public class CategoryRouter {

    private static final String BASE_PATH = "/api/v1/categories";
    private static final String ADMIN_BASE_PATH = "/api/v1/admin";

    /**
     * Public category routes - accessible to all users
     */
    @Bean
    public RouterFunction<ServerResponse> categoryRoutes(CategoryHandler handler) {
        return RouterFunctions.nest(path(BASE_PATH),
            RouterFunctions
                .route(GET(""), handler::getAllCategories)
                .andRoute(GET("/slug/{slug}"), handler::getCategoryBySlug)
                .andRoute(GET("/{categoryId}"), handler::getCategoryById)
                .andRoute(GET("/{categoryId}/subcategories"), handler::getSubcategories)
        );
    }

    /**
     * Admin category routes - restricted to admins only
     * TODO: Add authentication/authorization filter
     */
    @Bean
    public RouterFunction<ServerResponse> adminCategoryRoutes(CategoryHandler handler) {
        return RouterFunctions.nest(path(ADMIN_BASE_PATH),
            RouterFunctions
                // Category management
                .route(POST("/categories"), handler::createCategory)
                .andRoute(PUT("/categories/{categoryId}"), handler::updateCategory)
                .andRoute(DELETE("/categories/{categoryId}"), handler::deleteCategory)

                // Subcategory management
                .andRoute(POST("/subcategories"), handler::createSubcategory)
                .andRoute(PUT("/categories/{categoryId}/subcategories/{subcategoryId}"),
                        handler::updateSubcategory)
                .andRoute(DELETE("/categories/{categoryId}/subcategories/{subcategoryId}"),
                        handler::deleteSubcategory)
        );
    }
}
