package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.Trade;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for Trade entity (trades_by_market table)
 */
@Repository
public interface TradeRepository extends ReactiveCassandraRepository<Trade, UUID> {

    /**
     * Find all trades for a market
     */
    Flux<Trade> findByMarketId(UUID marketId);

    /**
     * Find specific trade
     */
    @Query("SELECT * FROM trades_by_market WHERE market_id = ?0 AND executed_at = ?1 AND trade_id = ?2")
    Mono<Trade> findByMarketIdAndExecutedAtAndTradeId(UUID marketId, Instant executedAt, UUID tradeId);

    /**
     * Find trades for a market and outcome
     */
    @Query("SELECT * FROM trades_by_market WHERE market_id = ?0 AND outcome_id = ?1 ALLOW FILTERING")
    Flux<Trade> findByMarketIdAndOutcomeId(UUID marketId, UUID outcomeId);

    /**
     * Find trades within time range
     */
    @Query("SELECT * FROM trades_by_market WHERE market_id = ?0 AND executed_at >= ?1 AND executed_at <= ?2")
    Flux<Trade> findByMarketIdAndExecutedAtBetween(UUID marketId, Instant start, Instant end);

    /**
     * Find recent trades (last N hours)
     */
    @Query("SELECT * FROM trades_by_market WHERE market_id = ?0 AND executed_at >= ?1")
    Flux<Trade> findRecentTradesByMarketId(UUID marketId, Instant since);

    /**
     * Find trades involving a specific user
     */
    @Query("SELECT * FROM trades_by_market WHERE market_id = ?0 AND (buyer_user_id = ?1 OR seller_user_id = ?1) ALLOW FILTERING")
    Flux<Trade> findByMarketIdAndUserId(UUID marketId, UUID userId);
}
