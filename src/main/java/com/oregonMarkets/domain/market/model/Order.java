package com.oregonMarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order entity
 * Table: orders_by_user
 * Represents a single order placed by a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders_by_user")
public class Order {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID userId;

    @PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Instant createdAt;

    @PrimaryKeyColumn(name = "order_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private UUID orderId;

    // Market and Outcome (Denormalized)
    @Column("market_id")
    private UUID marketId;

    @Column("market_title")
    private String marketTitle; // Denormalized

    @Column("outcome_id")
    private UUID outcomeId;

    @Column("outcome_name")
    private String outcomeName; // Denormalized

    // Order Details
    @Column("side")
    private String side; // BUY or SELL

    @Column("order_type")
    private String orderType; // LIMIT, MARKET

    @Column("price_e4")
    private Long priceE4; // Price in basis points

    @Column("quantity")
    private BigDecimal quantity;

    @Column("filled_quantity")
    private BigDecimal filledQuantity;

    @Column("remaining_quantity")
    private BigDecimal remainingQuantity;

    // Status
    @Column("status")
    private String status; // OPEN, PARTIALLY_FILLED, FILLED, CANCELLED, EXPIRED, REJECTED

    @Column("status_reason")
    private String statusReason;

    // Timing
    @Column("time_in_force")
    private String timeInForce; // GTC, IOC, FOK, DAY

    @Column("expires_at")
    private Instant expiresAt;

    @Column("filled_at")
    private Instant filledAt;

    @Column("cancelled_at")
    private Instant cancelledAt;

    // Financial
    @Column("total_cost")
    private BigDecimal totalCost;

    @Column("filled_cost")
    private BigDecimal filledCost;

    @Column("average_fill_price_e4")
    private Long averageFillPriceE4;

    @Column("fees_paid")
    private BigDecimal feesPaid;

    @Column("maker_fee_e4")
    private Long makerFeeE4;

    @Column("taker_fee_e4")
    private Long takerFeeE4;

    // Metadata
    @Column("client_order_id")
    private String clientOrderId; // User-provided ID

    @Column("ip_address")
    private String ipAddress;

    @Column("user_agent")
    private String userAgent;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("version")
    private Long version;
}
