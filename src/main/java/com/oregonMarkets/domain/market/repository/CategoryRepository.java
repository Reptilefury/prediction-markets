package com.oregonMarkets.domain.market.repository;

import com.oregonMarkets.domain.market.model.Category;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Category entity (categories table)
 */
@Repository
public interface CategoryRepository extends ReactiveCassandraRepository<Category, UUID> {

    /**
     * Find category by slug
     */
    @Query("SELECT * FROM categories WHERE slug = ?0 ALLOW FILTERING")
    Mono<Category> findBySlug(String slug);

    /**
     * Find all enabled categories ordered by display_order
     */
    @Query("SELECT * FROM categories WHERE enabled = true ALLOW FILTERING")
    Flux<Category> findAllEnabled();

    /**
     * Find categories by name (case-insensitive search)
     */
    @Query("SELECT * FROM categories WHERE name = ?0 ALLOW FILTERING")
    Flux<Category> findByName(String name);
}
