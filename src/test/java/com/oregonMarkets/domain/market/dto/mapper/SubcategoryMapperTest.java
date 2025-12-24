package com.oregonMarkets.domain.market.dto.mapper;

import com.oregonMarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonMarkets.domain.market.model.Subcategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubcategoryMapperTest {

    private SubcategoryMapper subcategoryMapper;

    @BeforeEach
    void setUp() {
        subcategoryMapper = new SubcategoryMapper();
    }

    @Test
    void toEntity_ShouldMapAllFields() {
        // Given
        UUID categoryId = UUID.randomUUID();
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(categoryId);
        request.setName("Elections");
        request.setSlug("elections");
        request.setDescription("Election markets");
        request.setLevel(1);
        request.setPath("Politics > Elections");
        request.setDisplayOrder(1);

        // When
        Subcategory subcategory = subcategoryMapper.toEntity(request);

        // Then
        assertThat(subcategory).isNotNull();
        assertThat(subcategory.getSubcategoryId()).isNotNull();
        assertThat(subcategory.getCategoryId()).isEqualTo(categoryId);
        assertThat(subcategory.getName()).isEqualTo("Elections");
        assertThat(subcategory.getSlug()).isEqualTo("elections");
        assertThat(subcategory.getDescription()).isEqualTo("Election markets");
        assertThat(subcategory.getLevel()).isEqualTo(1);
        assertThat(subcategory.getPath()).isEqualTo("Politics > Elections");
        assertThat(subcategory.getDisplayOrder()).isEqualTo(1);
        assertThat(subcategory.getEnabled()).isTrue();
    }

    @Test
    void toEntity_ShouldUseDefaultEnabled() {
        // Given
        CreateSubcategoryRequest request = new CreateSubcategoryRequest();
        request.setCategoryId(UUID.randomUUID());
        request.setName("Test");
        request.setSlug("test");

        // When
        Subcategory subcategory = subcategoryMapper.toEntity(request);

        // Then
        assertThat(subcategory.getEnabled()).isTrue();
    }

    @Test
    void updateEntity_ShouldUpdateFields() {
        // Given
        Subcategory subcategory = new Subcategory();
        subcategory.setName("Old Name");
        subcategory.setDescription("Old description");
        subcategory.setEnabled(true);

        UpdateSubcategoryRequest request = new UpdateSubcategoryRequest();
        request.setName("New Name");
        request.setDescription("New description");
        request.setEnabled(false);

        // When
        Subcategory updated = subcategoryMapper.updateEntity(subcategory, request);

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateEntity_ShouldOnlyUpdateProvidedFields() {
        // Given
        Subcategory subcategory = new Subcategory();
        subcategory.setName("Old Name");
        subcategory.setDescription("Old description");
        subcategory.setLevel(1);

        UpdateSubcategoryRequest request = new UpdateSubcategoryRequest();
        request.setName("New Name");
        // description and level not set

        // When
        Subcategory updated = subcategoryMapper.updateEntity(subcategory, request);

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("Old description");
        assertThat(updated.getLevel()).isEqualTo(1);
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        // Given
        UUID categoryId = UUID.randomUUID();
        UUID subcategoryId = UUID.randomUUID();
        Subcategory subcategory = new Subcategory();
        subcategory.setCategoryId(categoryId);
        subcategory.setSubcategoryId(subcategoryId);
        subcategory.setName("Presidential Race");
        subcategory.setSlug("presidential-race");
        subcategory.setDescription("Presidential race markets");
        subcategory.setLevel(2);
        subcategory.setPath("Politics > Elections > Presidential");
        subcategory.setDisplayOrder(1);
        subcategory.setEnabled(true);

        // When
        var response = subcategoryMapper.toResponse(subcategory);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubcategoryId()).isEqualTo(subcategoryId);
        assertThat(response.getCategoryId()).isEqualTo(categoryId);
        assertThat(response.getName()).isEqualTo("Presidential Race");
        assertThat(response.getSlug()).isEqualTo("presidential-race");
        assertThat(response.getDescription()).isEqualTo("Presidential race markets");
        assertThat(response.getLevel()).isEqualTo(2);
        assertThat(response.getPath()).isEqualTo("Politics > Elections > Presidential");
        assertThat(response.getEnabled()).isTrue();
    }
}
