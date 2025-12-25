package com.oregonmarkets.domain.market.handler;

import com.oregonmarkets.common.exception.BusinessException;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.mapper.CategoryMapper;
import com.oregonmarkets.domain.market.dto.mapper.SubcategoryMapper;
import com.oregonmarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonmarkets.domain.market.dto.response.CategoryResponse;
import com.oregonmarkets.domain.market.dto.response.SubcategoryResponse;
import com.oregonmarkets.domain.market.model.Category;
import com.oregonmarkets.domain.market.model.Subcategory;
import com.oregonmarkets.domain.market.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryHandlerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SubcategoryMapper subcategoryMapper;

    @Mock
    private ServerRequest serverRequest;

    @InjectMocks
    private CategoryHandler categoryHandler;

    private Category testCategory;
    private CategoryResponse testCategoryResponse;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        testCategory = new Category();
        testCategory.setCategoryId(categoryId);
        testCategory.setName("Test Category");
        testCategory.setSlug("test-category");

        testCategoryResponse = new CategoryResponse();
        testCategoryResponse.setCategoryId(categoryId);
        testCategoryResponse.setName("Test Category");
        testCategoryResponse.setSlug("test-category");
    }

    @Test
    void getAllCategories_ShouldReturnAllCategories() {
        // Given
        when(categoryService.getAllCategories()).thenReturn(Flux.just(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testCategoryResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.getAllCategories(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).getAllCategories();
        verify(categoryMapper).toResponse(testCategory);
    }

    @Test
    void getCategoryById_ValidId_ShouldReturnCategory() {
        // Given
        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(categoryService.getCategoryById(categoryId)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testCategoryResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.getCategoryById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).getCategoryById(categoryId);
    }

    @Test
    void getCategoryById_InvalidId_ShouldReturnBadRequest() {
        // Given
        when(serverRequest.pathVariable("categoryId")).thenReturn("invalid-uuid");

        // When
        Mono<ServerResponse> response = categoryHandler.getCategoryById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 400)
                .verifyComplete();

        verify(categoryService, never()).getCategoryById(any());
    }

    @Test
    void getCategoryById_NotFound_ShouldReturnNotFound() {
        // Given
        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(categoryService.getCategoryById(categoryId))
                .thenReturn(Mono.error(new BusinessException(ResponseCode.NOT_FOUND, "Category not found")));

        // When
        Mono<ServerResponse> response = categoryHandler.getCategoryById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 404)
                .verifyComplete();
    }

    @Test
    void getCategoryBySlug_ShouldReturnCategory() {
        // Given
        String slug = "test-category";
        when(serverRequest.pathVariable("slug")).thenReturn(slug);
        when(categoryService.getCategoryBySlug(slug)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testCategoryResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.getCategoryBySlug(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).getCategoryBySlug(slug);
    }

    @Test
    void getSubcategories_ValidId_ShouldReturnSubcategories() {
        // Given
        Subcategory sub = new Subcategory();
        SubcategoryResponse subResponse = new SubcategoryResponse();

        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(categoryService.getSubcategories(categoryId)).thenReturn(Flux.just(sub));
        when(subcategoryMapper.toResponse(sub)).thenReturn(subResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.getSubcategories(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).getSubcategories(categoryId);
    }

    @Test
    void createCategory_ValidRequest_ShouldCreateCategory() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("New Category");
        request.setSlug("new-category");

        when(serverRequest.bodyToMono(CreateCategoryRequest.class)).thenReturn(Mono.just(request));
        when(categoryService.createCategory(request)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testCategoryResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.createCategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 201)
                .verifyComplete();

        verify(categoryService).createCategory(request);
    }

    @Test
    void updateCategory_ValidRequest_ShouldUpdateCategory() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Updated Name");

        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(serverRequest.bodyToMono(UpdateCategoryRequest.class)).thenReturn(Mono.just(request));
        when(categoryService.updateCategory(categoryId, request)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testCategoryResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.updateCategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).updateCategory(categoryId, request);
    }

    @Test
    void deleteCategory_ValidId_ShouldDeleteCategory() {
        // Given
        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(categoryService.deleteCategory(categoryId)).thenReturn(Mono.empty());

        // When
        Mono<ServerResponse> response = categoryHandler.deleteCategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    void createSubcategory_ValidRequest_ShouldCreateSubcategory() {
        // Given
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(categoryId);
        request.setName("New Subcategory");

        Subcategory subcategory = new Subcategory();
        SubcategoryResponse subResponse = new SubcategoryResponse();

        when(serverRequest.bodyToMono(CreateSubcategoryRequest.class)).thenReturn(Mono.just(request));
        when(categoryService.createSubcategory(request)).thenReturn(Mono.just(subcategory));
        when(subcategoryMapper.toResponse(subcategory)).thenReturn(subResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.createSubcategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 201)
                .verifyComplete();

        verify(categoryService).createSubcategory(request);
    }

    @Test
    void updateSubcategory_ValidRequest_ShouldUpdateSubcategory() {
        // Given
        UUID subcategoryId = UUID.randomUUID();
        UpdateSubcategoryRequest request = new UpdateSubcategoryRequest();

        Subcategory subcategory = new Subcategory();
        SubcategoryResponse subResponse = new SubcategoryResponse();

        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(serverRequest.pathVariable("subcategoryId")).thenReturn(subcategoryId.toString());
        when(serverRequest.bodyToMono(UpdateSubcategoryRequest.class)).thenReturn(Mono.just(request));
        when(categoryService.updateSubcategory(categoryId, subcategoryId, request)).thenReturn(Mono.just(subcategory));
        when(subcategoryMapper.toResponse(subcategory)).thenReturn(subResponse);

        // When
        Mono<ServerResponse> response = categoryHandler.updateSubcategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).updateSubcategory(categoryId, subcategoryId, request);
    }

    @Test
    void deleteSubcategory_ValidIds_ShouldDeleteSubcategory() {
        // Given
        UUID subcategoryId = UUID.randomUUID();

        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(serverRequest.pathVariable("subcategoryId")).thenReturn(subcategoryId.toString());
        when(categoryService.deleteSubcategory(categoryId, subcategoryId)).thenReturn(Mono.empty());

        // When
        Mono<ServerResponse> response = categoryHandler.deleteSubcategory(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 200)
                .verifyComplete();

        verify(categoryService).deleteSubcategory(categoryId, subcategoryId);
    }

    @Test
    void handleError_BusinessException_ShouldReturnProperStatus() {
        // Given
        when(serverRequest.pathVariable("categoryId")).thenReturn(categoryId.toString());
        when(categoryService.getCategoryById(categoryId))
                .thenReturn(Mono.error(new BusinessException(ResponseCode.NOT_FOUND, "Not found")));

        // When
        Mono<ServerResponse> response = categoryHandler.getCategoryById(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 404)
                .verifyComplete();
    }

    @Test
    void handleError_GenericException_ShouldReturn500() {
        // Given
        when(categoryService.getAllCategories())
                .thenReturn(Flux.error(new RuntimeException("Unexpected error")));

        // When
        Mono<ServerResponse> response = categoryHandler.getAllCategories(serverRequest);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> serverResponse.statusCode().value() == 500)
                .verifyComplete();
    }
}
