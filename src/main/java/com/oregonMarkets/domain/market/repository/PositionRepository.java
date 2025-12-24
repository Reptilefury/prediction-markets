package com.oregonMarkets.domain.market.repository;

import com.oregonMarkets.domain.market.model.Position;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Position entity (positions_by_user table)
 */
@Repository
public interface PositionRepository extends ReactiveCassandraRepository<Position, UUID> {

    /**
     * Find all positions for a user
     */
    Flux<Position> findByUserId(UUID userId);

    /**
     * Find specific position
     */
    @Query("SELECT * FROM positions_by_user WHERE user_id = ?0 AND market_id = ?1 AND outcome_id = ?2")
    Mono<Position> findByUserIdAndMarketIdAndOutcomeId(UUID userId, UUID marketId, UUID outcomeId);

    /**
     * Find all positions for a user in a specific market
     */
    @Query("SELECT * FROM positions_by_user WHERE user_id = ?0 AND market_id = ?1")
    Flux<Position> findByUserIdAndMarketId(UUID userId, UUID marketId);

    /**
     * Find user's winning positions
     */
    @Query("SELECT * FROM positions_by_user WHERE user_id = ?0 AND is_winner = true ALLOW FILTERING")
    Flux<Position> findWinningPositionsByUserId(UUID userId);

    /**
     * Find user's positions by market status
     */
    @Query("SELECT * FROM positions_by_user WHERE user_id = ?0 AND market_status = ?1 ALLOW FILTERING")
    Flux<Position> findByUserIdAndMarketStatus(UUID userId, String marketStatus);

    /**
     * Find user's open positions (markets not resolved)
     */
    @Query("SELECT * FROM positions_by_user WHERE user_id = ?0 AND market_status IN ('OPEN', 'SUSPENDED') ALLOW FILTERING")
    Flux<Position> findOpenPositionsByUserId(UUID userId);
}
