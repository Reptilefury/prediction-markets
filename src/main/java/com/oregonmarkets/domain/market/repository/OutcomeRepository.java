package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.Outcome;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Outcome entity (outcomes table)
 */
@Repository
public interface OutcomeRepository extends ReactiveCassandraRepository<Outcome, UUID> {

    /**
     * Find all outcomes for a market
     */
    Flux<Outcome> findByMarketId(UUID marketId);

    /**
     * Find specific outcome
     */
    @Query("SELECT * FROM outcomes WHERE market_id = ?0 AND outcome_id = ?1")
    Mono<Outcome> findByMarketIdAndOutcomeId(UUID marketId, UUID outcomeId);

    /**
     * Find winning outcomes
     */
    @Query("SELECT * FROM outcomes WHERE market_id = ?0 AND is_winner = true ALLOW FILTERING")
    Flux<Outcome> findWinningOutcomesByMarketId(UUID marketId);

    /**
     * Find enabled outcomes for a market
     */
    @Query("SELECT * FROM outcomes WHERE market_id = ?0 AND enabled = true ALLOW FILTERING")
    Flux<Outcome> findEnabledOutcomesByMarketId(UUID marketId);
}
