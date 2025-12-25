package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.Order;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for Order entity (orders_by_user table)
 */
@Repository
public interface OrderRepository extends ReactiveCassandraRepository<Order, UUID> {

    /**
     * Find all orders for a user
     */
    Flux<Order> findByUserId(UUID userId);

    /**
     * Find specific order
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND created_at = ?1 AND order_id = ?2")
    Mono<Order> findByUserIdAndCreatedAtAndOrderId(UUID userId, Instant createdAt, UUID orderId);

    /**
     * Find user's orders for a specific market
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND market_id = ?1 ALLOW FILTERING")
    Flux<Order> findByUserIdAndMarketId(UUID userId, UUID marketId);

    /**
     * Find user's orders by status
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND status = ?1 ALLOW FILTERING")
    Flux<Order> findByUserIdAndStatus(UUID userId, String status);

    /**
     * Find user's open orders
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND status IN ('OPEN', 'PARTIALLY_FILLED') ALLOW FILTERING")
    Flux<Order> findOpenOrdersByUserId(UUID userId);

    /**
     * Find user's orders within date range
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND created_at >= ?1 AND created_at <= ?2")
    Flux<Order> findByUserIdAndCreatedAtBetween(UUID userId, Instant start, Instant end);
}
