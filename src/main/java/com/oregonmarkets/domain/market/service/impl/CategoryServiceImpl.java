package com.oregonmarkets.domain.market.service.impl;

import com.oregonmarkets.common.exception.BusinessException;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.market.dto.mapper.CategoryMapper;
import com.oregonmarkets.domain.market.dto.mapper.SubcategoryMapper;
import com.oregonmarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonmarkets.domain.market.model.Category;
import com.oregonmarkets.domain.market.model.Subcategory;
import com.oregonmarkets.domain.market.repository.CategoryRepository;
import com.oregonmarkets.domain.market.repository.SubcategoryRepository;
import com.oregonmarkets.domain.market.service.CategoryService;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Implementation of CategoryService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;

    private boolean isClosedSession(Throwable t) {
        // Detect transient driver/session closed errors
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalStateException &&
                cur.getMessage() != null && cur.getMessage().toLowerCase().contains("session is closed")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private Retry transientSessionRetry() {
        return Retry
            .backoff(3, Duration.ofMillis(200))
            .maxBackoff(Duration.ofSeconds(2))
            .filter(this::isClosedSession)
            .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    @Override
    public Flux<Category> getAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAllEnabled();
    }

    @Override
    public Mono<Category> getCategoryById(UUID categoryId) {
        log.debug("Fetching category by ID: {}", categoryId);

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with ID: " + categoryId
                )));
    }

    @Override
    public Mono<Category> getCategoryBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);

        return categoryRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with slug: " + slug
                )));
    }

    @Override
    public Flux<Subcategory> getSubcategories(UUID categoryId) {
        log.debug("Fetching subcategories for category: {}", categoryId);

        return subcategoryRepository.findEnabledByCategoryId(categoryId);
    }

    @Override
    public Mono<Subcategory> getSubcategory(UUID categoryId, UUID subcategoryId) {
        log.debug("Fetching subcategory: {} for category: {}", subcategoryId, categoryId);

        return subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Subcategory not found"
                )));
    }

    // ==================== Admin Category Operations ====================

    @Override
    public Mono<Category> createCategory(CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Check if slug already exists
        return categoryRepository.findBySlug(request.getSlug())
                .flatMap(existing -> Mono.<Category>error(new BusinessException(
                        ResponseCode.DUPLICATE_ORDER,
                        "Category with slug '" + request.getSlug() + "' already exists"
                )))
                .switchIfEmpty(Mono.defer(() -> {
                    Category category = categoryMapper.toEntity(request);

                    return categoryRepository.save(category)
                            .doOnSuccess(saved ->
                                    log.info("Category created successfully: {}", saved.getCategoryId()))
                            .retryWhen(transientSessionRetry());
                }));
    }

    @Override
    public Mono<Category> updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        log.info("Updating category: {}", categoryId);

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with ID: " + categoryId
                )))
                .flatMap(existing -> {
                    // Check if slug is being changed and if new slug already exists
                    if (request.getSlug() != null && !request.getSlug().equals(existing.getSlug())) {
                        return categoryRepository.findBySlug(request.getSlug())
                                .flatMap(other -> Mono.<Category>error(new BusinessException(
                                        ResponseCode.DUPLICATE_ORDER,
                                        "Category with slug '" + request.getSlug() + "' already exists"
                                )))
                                .switchIfEmpty(Mono.just(existing));
                    }
                    return Mono.just(existing);
                })
                .map(existing -> categoryMapper.updateEntity(existing, request))
                .flatMap(categoryRepository::save)
                .doOnSuccess(updated ->
                        log.info("Category updated successfully: {}", updated.getCategoryId()))
                .retryWhen(transientSessionRetry());
    }

    @Override
    public Mono<Void> deleteCategory(UUID categoryId) {
        log.info("Deleting category: {}", categoryId);

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with ID: " + categoryId
                )))
                .flatMap(category -> {
                    // Soft delete by disabling
                    category.setEnabled(false);
                    category.setUpdatedAt(Instant.now());
                    return categoryRepository.save(category);
                })
                .doOnSuccess(deleted ->
                        log.info("Category disabled successfully: {}", categoryId))
                .then()
                .retryWhen(transientSessionRetry());
    }

    // ==================== Admin Subcategory Operations ====================

    @Override
    public Mono<Subcategory> createSubcategory(CreateSubcategoryRequest request) {
        log.info("Creating new subcategory: {} for category: {}",
                request.getName(), request.getCategoryId());

        // Verify category exists
        return categoryRepository.findById(request.getCategoryId())
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Category not found with ID: " + request.getCategoryId()
                )))
                .then(subcategoryRepository.findBySlug(request.getSlug())
                        .flatMap(existing -> Mono.<Subcategory>error(new BusinessException(
                                ResponseCode.DUPLICATE_ORDER,
                                "Subcategory with slug '" + request.getSlug() + "' already exists"
                        )))
                        .switchIfEmpty(Mono.defer(() -> {
                            Subcategory subcategory = subcategoryMapper.toEntity(request);

                            return subcategoryRepository.save(subcategory)
                                    .doOnSuccess(saved ->
                                            log.info("Subcategory created successfully: {}",
                                                    saved.getSubcategoryId()))
                                    .retryWhen(transientSessionRetry());
                        }))
                );
    }

    @Override
    public Mono<Subcategory> updateSubcategory(
            UUID categoryId, UUID subcategoryId, UpdateSubcategoryRequest request) {
        log.info("Updating subcategory: {} for category: {}", subcategoryId, categoryId);

        return subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Subcategory not found"
                )))
                .flatMap(existing -> {
                    // Check if slug is being changed and if new slug already exists
                    if (request.getSlug() != null && !request.getSlug().equals(existing.getSlug())) {
                        return subcategoryRepository.findBySlug(request.getSlug())
                                .flatMap(other -> Mono.<Subcategory>error(new BusinessException(
                                        ResponseCode.DUPLICATE_ORDER,
                                        "Subcategory with slug '" + request.getSlug() + "' already exists"
                                )))
                                .switchIfEmpty(Mono.just(existing));
                    }
                    return Mono.just(existing);
                })
                .map(existing -> subcategoryMapper.updateEntity(existing, request))
                .flatMap(subcategoryRepository::save)
                .doOnSuccess(updated ->
                        log.info("Subcategory updated successfully: {}", updated.getSubcategoryId()))
                .retryWhen(transientSessionRetry());
    }

    @Override
    public Mono<Void> deleteSubcategory(UUID categoryId, UUID subcategoryId) {
        log.info("Deleting subcategory: {} for category: {}", subcategoryId, categoryId);

        return subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ResponseCode.NOT_FOUND,
                        "Subcategory not found"
                )))
                .flatMap(subcategory -> {
                    // Soft delete by disabling
                    subcategory.setEnabled(false);
                    subcategory.setUpdatedAt(Instant.now());
                    return subcategoryRepository.save(subcategory);
                })
                .doOnSuccess(deleted ->
                        log.info("Subcategory disabled successfully: {}", subcategoryId))
                .then()
                .retryWhen(transientSessionRetry());
    }
}
