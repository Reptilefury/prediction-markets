package com.oregonmarkets.domain.market.handler;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.mapper.CategoryMapper;
import com.oregonmarkets.domain.market.dto.mapper.SubcategoryMapper;
import com.oregonmarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonmarkets.domain.market.dto.response.CategoryResponse;
import com.oregonmarkets.domain.market.dto.response.SubcategoryResponse;
import com.oregonmarkets.domain.market.service.CategoryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Functional handler for category API endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryHandler {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;

    /**
     * GET /api/v1/categories - Get all categories with subcategories
     */
    public Mono<ServerResponse> getAllCategories(ServerRequest request) {
        return categoryService.getAllCategories()
                .flatMap(category -> 
                    categoryService.getSubcategories(category.getCategoryId())
                        .map(subcategoryMapper::toResponse)
                        .collectList()
                        .map(subcategories -> {
                            CategoryResponse response = categoryMapper.toResponse(category);
                            response.setSubcategories(subcategories);
                            response.setSubcategoryCount(subcategories.size());
                            return response;
                        })
                )
                .collectList()
                .flatMap(categories -> {
                    ApiResponse<java.util.List<CategoryResponse>> response = ApiResponse.success(categories);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/categories/{categoryId} - Get category by ID
     */
    public Mono<ServerResponse> getCategoryById(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);

            return categoryService.getCategoryById(categoryId)
                    .map(categoryMapper::toResponse)
                    .flatMap(category -> {
                        ApiResponse<CategoryResponse> response = ApiResponse.success(category);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid category ID format"));
        }
    }

    /**
     * GET /api/v1/categories/slug/{slug} - Get category by slug
     */
    public Mono<ServerResponse> getCategoryBySlug(ServerRequest request) {
        String slug = request.pathVariable("slug");

        return categoryService.getCategoryBySlug(slug)
                .map(categoryMapper::toResponse)
                .flatMap(category -> {
                    ApiResponse<CategoryResponse> response = ApiResponse.success(category);
                    return ServerResponse.ok().bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * GET /api/v1/categories/{categoryId}/subcategories - Get subcategories
     */
    public Mono<ServerResponse> getSubcategories(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);

            return categoryService.getSubcategories(categoryId)
                    .map(subcategoryMapper::toResponse)
                    .collectList()
                    .flatMap(subcategories -> {
                        ApiResponse<java.util.List<SubcategoryResponse>> response = ApiResponse.success(subcategories);
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid category ID format"));
        }
    }

    // ==================== Admin Category Operations ====================

    /**
     * POST /api/v1/admin/categories - Create a new category
     */
    public Mono<ServerResponse> createCategory(ServerRequest request) {
        return request.bodyToMono(CreateCategoryRequest.class)
                .flatMap(categoryService::createCategory)
                .map(categoryMapper::toResponse)
                .flatMap(category -> {
                    ApiResponse<CategoryResponse> response = ApiResponse.success(
                            ResponseCode.CREATED,
                            category
                    );
                    return ServerResponse.status(201).bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * PUT /api/v1/admin/categories/{categoryId} - Update category
     */
    public Mono<ServerResponse> updateCategory(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);

            return request.bodyToMono(UpdateCategoryRequest.class)
                    .flatMap(updateRequest ->
                            categoryService.updateCategory(categoryId, updateRequest))
                    .map(categoryMapper::toResponse)
                    .flatMap(category -> {
                        ApiResponse<CategoryResponse> response = ApiResponse.success(
                                ResponseCode.UPDATED,
                                category
                        );
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid category ID format"));
        }
    }

    /**
     * DELETE /api/v1/admin/categories/{categoryId} - Delete category
     */
    public Mono<ServerResponse> deleteCategory(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);

            return categoryService.deleteCategory(categoryId)
                    .then(Mono.fromCallable(() -> {
                        ApiResponse<Void> response = ApiResponse.success(
                                ResponseCode.DELETED,
                                "Category deleted successfully",
                                null
                        );
                        return response;
                    }))
                    .flatMap(response -> ServerResponse.ok().bodyValue(response))
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid category ID format"));
        }
    }

    // ==================== Admin Subcategory Operations ====================

    /**
     * POST /api/v1/admin/subcategories - Create a new subcategory
     */
    public Mono<ServerResponse> createSubcategory(ServerRequest request) {
        return request.bodyToMono(CreateSubcategoryRequest.class)
                .flatMap(categoryService::createSubcategory)
                .map(subcategoryMapper::toResponse)
                .flatMap(subcategory -> {
                    ApiResponse<SubcategoryResponse> response = ApiResponse.success(
                            ResponseCode.CREATED,
                            subcategory
                    );
                    return ServerResponse.status(201).bodyValue(response);
                })
                .onErrorResume(this::handleError);
    }

    /**
     * PUT /api/v1/admin/categories/{categoryId}/subcategories/{subcategoryId} - Update subcategory
     */
    public Mono<ServerResponse> updateSubcategory(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");
        String subcategoryIdStr = request.pathVariable("subcategoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);
            UUID subcategoryId = UUID.fromString(subcategoryIdStr);

            return request.bodyToMono(UpdateSubcategoryRequest.class)
                    .flatMap(updateRequest ->
                            categoryService.updateSubcategory(categoryId, subcategoryId, updateRequest))
                    .map(subcategoryMapper::toResponse)
                    .flatMap(subcategory -> {
                        ApiResponse<SubcategoryResponse> response = ApiResponse.success(
                                ResponseCode.UPDATED,
                                subcategory
                        );
                        return ServerResponse.ok().bodyValue(response);
                    })
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid ID format"));
        }
    }

    /**
     * DELETE /api/v1/admin/categories/{categoryId}/subcategories/{subcategoryId} - Delete subcategory
     */
    public Mono<ServerResponse> deleteSubcategory(ServerRequest request) {
        String categoryIdStr = request.pathVariable("categoryId");
        String subcategoryIdStr = request.pathVariable("subcategoryId");

        try {
            UUID categoryId = UUID.fromString(categoryIdStr);
            UUID subcategoryId = UUID.fromString(subcategoryIdStr);

            return categoryService.deleteSubcategory(categoryId, subcategoryId)
                    .then(Mono.fromCallable(() -> {
                        ApiResponse<Void> response = ApiResponse.success(
                                ResponseCode.DELETED,
                                "Subcategory deleted successfully",
                                null
                        );
                        return response;
                    }))
                    .flatMap(response -> ServerResponse.ok().bodyValue(response))
                    .onErrorResume(this::handleError);
        } catch (IllegalArgumentException e) {
            return handleError(new IllegalArgumentException("Invalid ID format"));
        }
    }

    // ==================== Error Handling ====================

    private Mono<ServerResponse> handleError(Throwable error) {
        log.error("Error handling request", error);

        if (error instanceof com.oregonmarkets.common.exception.BusinessException) {
            com.oregonmarkets.common.exception.BusinessException be =
                    (com.oregonmarkets.common.exception.BusinessException) error;
            ApiResponse<Void> response = ApiResponse.error(be.getResponseCode(), be.getMessage());
            return ServerResponse
                    .status(be.getResponseCode().getHttpStatus())
                    .bodyValue(response);
        }

        if (error instanceof IllegalArgumentException) {
            ApiResponse<Void> response = ApiResponse.error(
                    ResponseCode.INVALID_INPUT,
                    error.getMessage()
            );
            return ServerResponse.badRequest().bodyValue(response);
        }

        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        return ServerResponse
                .status(500)
                .bodyValue(response);
    }
}
