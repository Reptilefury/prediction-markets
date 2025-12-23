package com.oregonMarkets.domain.market.service.impl;

import com.oregonMarkets.common.exception.BusinessException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.domain.market.dto.mapper.CategoryMapper;
import com.oregonMarkets.domain.market.dto.mapper.SubcategoryMapper;
import com.oregonMarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonMarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonMarkets.domain.market.model.Category;
import com.oregonMarkets.domain.market.model.Subcategory;
import com.oregonMarkets.domain.market.repository.CategoryRepository;
import com.oregonMarkets.domain.market.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SubcategoryMapper subcategoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private Subcategory testSubcategory;
    private UUID categoryId;
    private UUID subcategoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        subcategoryId = UUID.randomUUID();

        testCategory = new Category();
        testCategory.setCategoryId(categoryId);
        testCategory.setName("Test Category");
        testCategory.setSlug("test-category");
        testCategory.setEnabled(true);
        testCategory.setCreatedAt(Instant.now());
        testCategory.setUpdatedAt(Instant.now());

        testSubcategory = new Subcategory();
        testSubcategory.setSubcategoryId(subcategoryId);
        testSubcategory.setCategoryId(categoryId);
        testSubcategory.setName("Test Subcategory");
        testSubcategory.setSlug("test-subcategory");
        testSubcategory.setEnabled(true);
        testSubcategory.setCreatedAt(Instant.now());
        testSubcategory.setUpdatedAt(Instant.now());
    }

    // ==================== Read Operations ====================

    @Test
    void getAllCategories_ShouldReturnAllEnabledCategories() {
        // Given
        Category category2 = new Category();
        category2.setCategoryId(UUID.randomUUID());
        category2.setName("Category 2");
        category2.setEnabled(true);

        when(categoryRepository.findAllEnabled()).thenReturn(Flux.just(testCategory, category2));

        // When & Then
        StepVerifier.create(categoryService.getAllCategories())
                .expectNext(testCategory)
                .expectNext(category2)
                .verifyComplete();

        verify(categoryRepository).findAllEnabled();
    }

    @Test
    void getCategoryById_WhenExists_ShouldReturnCategory() {
        // Given
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.getCategoryById(categoryId))
                .expectNext(testCategory)
                .verifyComplete();

        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void getCategoryById_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.getCategoryById(nonExistentId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND &&
                                throwable.getMessage().contains("Category not found")
                )
                .verify();

        verify(categoryRepository).findById(nonExistentId);
    }

    @Test
    void getCategoryBySlug_WhenExists_ShouldReturnCategory() {
        // Given
        String slug = "test-category";
        when(categoryRepository.findBySlug(slug)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.getCategoryBySlug(slug))
                .expectNext(testCategory)
                .verifyComplete();

        verify(categoryRepository).findBySlug(slug);
    }

    @Test
    void getCategoryBySlug_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        String slug = "non-existent";
        when(categoryRepository.findBySlug(slug)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.getCategoryBySlug(slug))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();

        verify(categoryRepository).findBySlug(slug);
    }

    @Test
    void getSubcategories_ShouldReturnEnabledSubcategories() {
        // Given
        Subcategory sub2 = new Subcategory();
        sub2.setSubcategoryId(UUID.randomUUID());
        sub2.setCategoryId(categoryId);

        when(subcategoryRepository.findEnabledByCategoryId(categoryId))
                .thenReturn(Flux.just(testSubcategory, sub2));

        // When & Then
        StepVerifier.create(categoryService.getSubcategories(categoryId))
                .expectNext(testSubcategory)
                .expectNext(sub2)
                .verifyComplete();

        verify(subcategoryRepository).findEnabledByCategoryId(categoryId);
    }

    @Test
    void getSubcategory_WhenExists_ShouldReturnSubcategory() {
        // Given
        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.just(testSubcategory));

        // When & Then
        StepVerifier.create(categoryService.getSubcategory(categoryId, subcategoryId))
                .expectNext(testSubcategory)
                .verifyComplete();

        verify(subcategoryRepository).findByCategoryIdAndSubcategoryId(categoryId, subcategoryId);
    }

    @Test
    void getSubcategory_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.getSubcategory(categoryId, subcategoryId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();
    }

    // ==================== Create Operations ====================

    @Test
    void createCategory_WhenSlugIsUnique_ShouldCreateCategory() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("New Category");
        request.setSlug("new-category");
        request.setDescription("Description");

        when(categoryRepository.findBySlug(request.getSlug())).thenReturn(Mono.empty());
        when(categoryMapper.toEntity(request)).thenReturn(testCategory);
        when(categoryRepository.save(testCategory)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.createCategory(request))
                .expectNext(testCategory)
                .verifyComplete();

        verify(categoryRepository).findBySlug(request.getSlug());
        verify(categoryMapper).toEntity(request);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void createCategory_WhenSlugAlreadyExists_ShouldThrowBusinessException() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setSlug("existing-slug");

        when(categoryRepository.findBySlug(request.getSlug())).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.createCategory(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.DUPLICATE_ORDER &&
                                throwable.getMessage().contains("already exists")
                )
                .verify();

        verify(categoryRepository).findBySlug(request.getSlug());
        verify(categoryRepository, never()).save(any());
    }

    // ==================== Update Operations ====================

    @Test
    void updateCategory_WhenCategoryExists_ShouldUpdateCategory() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");

        Category updatedCategory = new Category();
        updatedCategory.setCategoryId(categoryId);
        updatedCategory.setName("Updated Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.updateEntity(testCategory, request)).thenReturn(testCategory);
        when(categoryRepository.save(testCategory)).thenReturn(Mono.just(updatedCategory));

        // When & Then
        StepVerifier.create(categoryService.updateCategory(categoryId, request))
                .expectNext(updatedCategory)
                .verifyComplete();

        verify(categoryRepository).findById(categoryId);
        verify(categoryMapper).updateEntity(testCategory, request);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void updateCategory_WhenCategoryNotExists_ShouldThrowBusinessException() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.updateCategory(categoryId, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_WhenChangingSlugToExistingOne_ShouldThrowBusinessException() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setSlug("another-slug");

        Category anotherCategory = new Category();
        anotherCategory.setCategoryId(UUID.randomUUID());
        anotherCategory.setSlug("another-slug");

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(categoryRepository.findBySlug("another-slug")).thenReturn(Mono.just(anotherCategory));

        // When & Then
        StepVerifier.create(categoryService.updateCategory(categoryId, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.DUPLICATE_ORDER
                )
                .verify();

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findBySlug("another-slug");
    }

    @Test
    void updateCategory_WhenSlugUnchanged_ShouldUpdate() {
        // Given
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setSlug("test-category"); // Same slug

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(categoryMapper.updateEntity(testCategory, request)).thenReturn(testCategory);
        when(categoryRepository.save(testCategory)).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.updateCategory(categoryId, request))
                .expectNext(testCategory)
                .verifyComplete();
    }

    // ==================== Delete Operations ====================

    @Test
    void deleteCategory_WhenExists_ShouldSoftDelete() {
        // Given
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(Mono.just(testCategory));

        // When & Then
        StepVerifier.create(categoryService.deleteCategory(categoryId))
                .verifyComplete();

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(argThat(category -> !category.getEnabled()));
    }

    @Test
    void deleteCategory_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.deleteCategory(categoryId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    // ==================== Subcategory Operations ====================

    @Test
    void createSubcategory_WhenCategoryExistsAndSlugUnique_ShouldCreateSubcategory() {
        // Given
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(categoryId);
        request.setName("New Subcategory");
        request.setSlug("new-subcategory");

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(subcategoryRepository.findBySlug(request.getSlug())).thenReturn(Mono.empty());
        when(subcategoryMapper.toEntity(request)).thenReturn(testSubcategory);
        when(subcategoryRepository.save(testSubcategory)).thenReturn(Mono.just(testSubcategory));

        // When & Then
        StepVerifier.create(categoryService.createSubcategory(request))
                .expectNext(testSubcategory)
                .verifyComplete();

        verify(categoryRepository).findById(categoryId);
        verify(subcategoryRepository).findBySlug(request.getSlug());
        verify(subcategoryRepository).save(testSubcategory);
    }

    @Test
    void createSubcategory_WhenCategoryNotExists_ShouldThrowBusinessException() {
        // Given
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(categoryId);
        request.setSlug("test-slug");

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.empty());
        when(subcategoryRepository.findBySlug(anyString())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.createSubcategory(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND &&
                                throwable.getMessage().contains("Category not found")
                )
                .verify();

        verify(categoryRepository).findById(categoryId);
        verify(subcategoryRepository, never()).save(any());
    }

    @Test
    void createSubcategory_WhenSlugAlreadyExists_ShouldThrowBusinessException() {
        // Given
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(categoryId);
        request.setSlug("existing-slug");

        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(testCategory));
        when(subcategoryRepository.findBySlug(request.getSlug())).thenReturn(Mono.just(testSubcategory));

        // When & Then
        StepVerifier.create(categoryService.createSubcategory(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.DUPLICATE_ORDER
                )
                .verify();
    }

    @Test
    void updateSubcategory_WhenExists_ShouldUpdateSubcategory() {
        // Given
        UpdateSubcategoryRequest request = new UpdateSubcategoryRequest();
        request.setName("Updated Subcategory");

        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.just(testSubcategory));
        when(subcategoryMapper.updateEntity(testSubcategory, request)).thenReturn(testSubcategory);
        when(subcategoryRepository.save(testSubcategory)).thenReturn(Mono.just(testSubcategory));

        // When & Then
        StepVerifier.create(categoryService.updateSubcategory(categoryId, subcategoryId, request))
                .expectNext(testSubcategory)
                .verifyComplete();

        verify(subcategoryRepository).findByCategoryIdAndSubcategoryId(categoryId, subcategoryId);
        verify(subcategoryRepository).save(testSubcategory);
    }

    @Test
    void updateSubcategory_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        UpdateSubcategoryRequest request = new UpdateSubcategoryRequest();
        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.updateSubcategory(categoryId, subcategoryId, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();
    }

    @Test
    void deleteSubcategory_WhenExists_ShouldSoftDelete() {
        // Given
        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.just(testSubcategory));
        when(subcategoryRepository.save(any(Subcategory.class))).thenReturn(Mono.just(testSubcategory));

        // When & Then
        StepVerifier.create(categoryService.deleteSubcategory(categoryId, subcategoryId))
                .verifyComplete();

        verify(subcategoryRepository).findByCategoryIdAndSubcategoryId(categoryId, subcategoryId);
        verify(subcategoryRepository).save(argThat(sub -> !sub.getEnabled()));
    }

    @Test
    void deleteSubcategory_WhenNotExists_ShouldThrowBusinessException() {
        // Given
        when(subcategoryRepository.findByCategoryIdAndSubcategoryId(categoryId, subcategoryId))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(categoryService.deleteSubcategory(categoryId, subcategoryId))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getResponseCode() == ResponseCode.NOT_FOUND
                )
                .verify();

        verify(subcategoryRepository, never()).save(any());
    }
}
