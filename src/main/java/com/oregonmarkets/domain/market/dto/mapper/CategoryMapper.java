package com.oregonmarkets.domain.market.dto.mapper;

import com.oregonmarkets.domain.market.dto.request.CreateCategoryRequest;
import com.oregonmarkets.domain.market.dto.request.UpdateCategoryRequest;
import com.oregonmarkets.domain.market.dto.response.CategoryResponse;
import com.oregonmarkets.domain.market.model.Category;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper for Category entities and DTOs
 */
@Component
public class CategoryMapper {

  /**
   * Map CreateCategoryRequest to Category entity
   */
  public Category toEntity(CreateCategoryRequest request) {
    Instant now = Instant.now();

    return Category.builder()
        .categoryId(UUID.randomUUID())
        .name(request.getName())
        .slug(request.getSlug())
        .description(request.getDescription())
        .icon(request.getIcon())
        .color(request.getColor())
        .displayOrder(request.getDisplayOrder())
        .enabled(request.getEnabled() != null ? request.getEnabled() : true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Update Category entity from UpdateCategoryRequest
   */
  public Category updateEntity(Category existing, UpdateCategoryRequest request) {
    if (request.getName() != null) {
      existing.setName(request.getName());
    }
    if (request.getSlug() != null) {
      existing.setSlug(request.getSlug());
    }
    if (request.getDescription() != null) {
      existing.setDescription(request.getDescription());
    }
    if (request.getIcon() != null) {
      existing.setIcon(request.getIcon());
    }
    if (request.getColor() != null) {
      existing.setColor(request.getColor());
    }
    if (request.getDisplayOrder() != null) {
      existing.setDisplayOrder(request.getDisplayOrder());
    }
    if (request.getEnabled() != null) {
      existing.setEnabled(request.getEnabled());
    }
    existing.setUpdatedAt(Instant.now());

    return existing;
  }

  /**
   * Map Category entity to CategoryResponse DTO
   */
  public CategoryResponse toResponse(Category category) {
    return CategoryResponse.builder()
        .categoryId(category.getCategoryId())
        .name(category.getName())
        .slug(category.getSlug())
        .description(category.getDescription())
        .icon(category.getIcon())
        .color(category.getColor())
        .displayOrder(category.getDisplayOrder())
        .enabled(category.getEnabled())
        .createdAt(category.getCreatedAt())
        .updatedAt(category.getUpdatedAt())
        .build();
  }
}
