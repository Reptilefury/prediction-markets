package com.oregonMarkets.domain.market.repository;

import com.oregonMarkets.domain.market.model.Market;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Market entity (markets_by_id table)
 */
@Repository
public interface MarketRepository extends ReactiveCassandraRepository<Market, UUID> {

    /**
     * Find markets by status
     */
    Flux<Market> findByStatus(String status);

    /**
     * Find markets by category
     */
    Flux<Market> findByCategoryId(UUID categoryId);

    /**
     * Find featured markets
     */
    Flux<Market> findByFeaturedTrue();

    /**
     * Find trending markets
     */
    Flux<Market> findByTrendingTrue();

    /**
     * Find market by slug
     */
    Mono<Market> findBySlug(String slug);

    /**
     * Find markets by creator
     */
    Flux<Market> findByCreatorId(UUID creatorId);

    /**
     * Custom query to find open markets by category
     */
    @Query("SELECT * FROM markets_by_id WHERE category_id = ?0 AND status = 'OPEN' ALLOW FILTERING")
    Flux<Market> findOpenMarketsByCategory(UUID categoryId);
}
