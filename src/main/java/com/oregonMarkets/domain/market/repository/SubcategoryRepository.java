package com.oregonMarkets.domain.market.repository;

import com.oregonMarkets.domain.market.model.Subcategory;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Subcategory entity (subcategories table)
 */
@Repository
public interface SubcategoryRepository extends ReactiveCassandraRepository<Subcategory, UUID> {

    /**
     * Find all subcategories for a category
     */
    Flux<Subcategory> findByCategoryId(UUID categoryId);

    /**
     * Find specific subcategory
     */
    @Query("SELECT * FROM subcategories WHERE category_id = ?0 AND subcategory_id = ?1")
    Mono<Subcategory> findByCategoryIdAndSubcategoryId(UUID categoryId, UUID subcategoryId);

    /**
     * Find subcategories by parent
     */
    @Query("SELECT * FROM subcategories WHERE category_id = ?0 AND parent_subcategory_id = ?1 ALLOW FILTERING")
    Flux<Subcategory> findByParentSubcategoryId(UUID categoryId, UUID parentSubcategoryId);

    /**
     * Find subcategory by slug
     */
    @Query("SELECT * FROM subcategories WHERE slug = ?0 ALLOW FILTERING")
    Mono<Subcategory> findBySlug(String slug);

    /**
     * Find enabled subcategories for a category
     */
    @Query("SELECT * FROM subcategories WHERE category_id = ?0 AND enabled = true ALLOW FILTERING")
    Flux<Subcategory> findEnabledByCategoryId(UUID categoryId);

    /**
     * Find subcategories by level
     */
    @Query("SELECT * FROM subcategories WHERE category_id = ?0 AND level = ?1 ALLOW FILTERING")
    Flux<Subcategory> findByCategoryIdAndLevel(UUID categoryId, Integer level);
}
