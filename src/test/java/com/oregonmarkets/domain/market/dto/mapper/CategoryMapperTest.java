package com.oregonmarkets.domain.market.dto.mapper;

import com.oregonmarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonmarkets.domain.market.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    void toEntity_ShouldMapAllFields() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Politics");
        request.setSlug("politics");
        request.setDescription("Political markets");
        request.setIcon("vote");
        request.setColor("#FF0000");
        request.setDisplayOrder(1);

        // When
        Category category = categoryMapper.toEntity(request);

        // Then
        assertThat(category).isNotNull();
        assertThat(category.getCategoryId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Politics");
        assertThat(category.getSlug()).isEqualTo("politics");
        assertThat(category.getDescription()).isEqualTo("Political markets");
        assertThat(category.getIcon()).isEqualTo("vote");
        assertThat(category.getColor()).isEqualTo("#FF0000");
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.getEnabled()).isTrue();
    }

    @Test
    void toEntity_ShouldUseDefaultEnabled() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Sports");
        request.setSlug("sports");

        // When
        Category category = categoryMapper.toEntity(request);

        // Then
        assertThat(category.getEnabled()).isTrue();
    }

    @Test
    void updateEntity_ShouldUpdateFields() {
        // Given
        Category category = new Category();
        category.setName("Old Name");
        category.setDescription("Old description");
        category.setEnabled(true);

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New Name");
        request.setDescription("New description");
        request.setEnabled(false);

        // When
        Category updated = categoryMapper.updateEntity(category, request);

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateEntity_ShouldOnlyUpdateProvidedFields() {
        // Given
        Category category = new Category();
        category.setName("Old Name");
        category.setDescription("Old description");
        category.setIcon("old-icon");

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New Name");
        // description and icon not set

        // When
        Category updated = categoryMapper.updateEntity(category, request);

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("Old description");
        assertThat(updated.getIcon()).isEqualTo("old-icon");
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        // Given
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Sports");
        category.setSlug("sports");
        category.setDescription("Sports markets");
        category.setIcon("ball");
        category.setColor("#00FF00");
        category.setDisplayOrder(2);
        category.setEnabled(true);

        // When
        var response = categoryMapper.toResponse(category);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(category.getCategoryId());
        assertThat(response.getName()).isEqualTo("Sports");
        assertThat(response.getSlug()).isEqualTo("sports");
        assertThat(response.getDescription()).isEqualTo("Sports markets");
        assertThat(response.getIcon()).isEqualTo("ball");
        assertThat(response.getColor()).isEqualTo("#00FF00");
        assertThat(response.getEnabled()).isTrue();
    }
}
