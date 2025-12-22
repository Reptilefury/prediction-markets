package com.oregonMarkets.domain.market.service;

import com.oregonMarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonMarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonMarkets.domain.market.model.Category;
import com.oregonMarkets.domain.market.model.Subcategory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service interface for category operations
 */
public interface CategoryService {

    // ==================== Public Read Operations ====================

    /**
     * Get all categories
     */
    Flux<Category> getAllCategories();

    /**
     * Get category by ID
     */
    Mono<Category> getCategoryById(UUID categoryId);

    /**
     * Get category by slug
     */
    Mono<Category> getCategoryBySlug(String slug);

    /**
     * Get subcategories for a category
     */
    Flux<Subcategory> getSubcategories(UUID categoryId);

    /**
     * Get specific subcategory
     */
    Mono<Subcategory> getSubcategory(UUID categoryId, UUID subcategoryId);

    // ==================== Admin Category Operations ====================

    /**
     * Create a new category
     */
    Mono<Category> createCategory(CreateCategoryRequest request);

    /**
     * Update an existing category
     */
    Mono<Category> updateCategory(UUID categoryId, UpdateCategoryRequest request);

    /**
     * Delete/disable a category
     */
    Mono<Void> deleteCategory(UUID categoryId);

    // ==================== Admin Subcategory Operations ====================

    /**
     * Create a new subcategory
     */
    Mono<Subcategory> createSubcategory(CreateSubcategoryRequest request);

    /**
     * Update an existing subcategory
     */
    Mono<Subcategory> updateSubcategory(
        UUID categoryId, UUID subcategoryId, UpdateSubcategoryRequest request);

    /**
     * Delete/disable a subcategory
     */
    Mono<Void> deleteSubcategory(UUID categoryId, UUID subcategoryId);
}
