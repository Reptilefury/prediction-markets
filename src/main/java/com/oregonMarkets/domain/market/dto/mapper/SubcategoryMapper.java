package com.oregonMarkets.domain.market.dto.mapper;

import com.oregonMarkets.domain.market.dto.request.CreateSubcategoryRequest;
import com.oregonMarkets.domain.market.dto.request.UpdateSubcategoryRequest;
import com.oregonMarkets.domain.market.dto.response.SubcategoryResponse;
import com.oregonMarkets.domain.market.model.Subcategory;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper for Subcategory entities and DTOs
 */
@Component
public class SubcategoryMapper {

  /**
   * Map CreateSubcategoryRequest to Subcategory entity
   */
  public Subcategory toEntity(CreateSubcategoryRequest request) {
    Instant now = Instant.now();

    return Subcategory.builder()
        .categoryId(request.getCategoryId())
        .subcategoryId(UUID.randomUUID())
        .parentSubcategoryId(request.getParentSubcategoryId())
        .name(request.getName())
        .slug(request.getSlug())
        .description(request.getDescription())
        .level(request.getLevel())
        .path(request.getPath())
        .displayOrder(request.getDisplayOrder())
        .enabled(request.getEnabled() != null ? request.getEnabled() : true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Update Subcategory entity from UpdateSubcategoryRequest
   */
  public Subcategory updateEntity(Subcategory existing, UpdateSubcategoryRequest request) {
    if (request.getParentSubcategoryId() != null) {
      existing.setParentSubcategoryId(request.getParentSubcategoryId());
    }
    if (request.getName() != null) {
      existing.setName(request.getName());
    }
    if (request.getSlug() != null) {
      existing.setSlug(request.getSlug());
    }
    if (request.getDescription() != null) {
      existing.setDescription(request.getDescription());
    }
    if (request.getLevel() != null) {
      existing.setLevel(request.getLevel());
    }
    if (request.getPath() != null) {
      existing.setPath(request.getPath());
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
   * Map Subcategory entity to SubcategoryResponse DTO
   */
  public SubcategoryResponse toResponse(Subcategory subcategory) {
    return SubcategoryResponse.builder()
        .categoryId(subcategory.getCategoryId())
        .subcategoryId(subcategory.getSubcategoryId())
        .parentSubcategoryId(subcategory.getParentSubcategoryId())
        .name(subcategory.getName())
        .slug(subcategory.getSlug())
        .description(subcategory.getDescription())
        .level(subcategory.getLevel())
        .path(subcategory.getPath())
        .displayOrder(subcategory.getDisplayOrder())
        .enabled(subcategory.getEnabled())
        .createdAt(subcategory.getCreatedAt())
        .updatedAt(subcategory.getUpdatedAt())
        .build();
  }
}
