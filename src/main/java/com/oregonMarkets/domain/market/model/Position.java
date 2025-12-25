package com.oregonmarkets.domain.market.model;

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
 * Position entity - User's position in a specific outcome
 * Table: positions_by_user
 * Aggregates all trades for a user in a specific market outcome
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("positions_by_user")
public class Position {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID userId;

    @PrimaryKeyColumn(name = "market_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID marketId;

    @PrimaryKeyColumn(name = "outcome_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private UUID outcomeId;

    // Market and Outcome (Denormalized)
    @Column("market_title")
    private String marketTitle;

    @Column("market_status")
    private String marketStatus;

    @Column("outcome_name")
    private String outcomeName;

    // Position Details
    @Column("quantity")
    private BigDecimal quantity; // Net position (positive = long, negative = short)

    @Column("available_quantity")
    private BigDecimal availableQuantity; // Quantity not locked in open orders

    @Column("locked_quantity")
    private BigDecimal lockedQuantity; // Quantity locked in open orders

    @Column("average_entry_price_e4")
    private Long averageEntryPriceE4; // Average price paid per share

    @Column("total_cost")
    private BigDecimal totalCost; // Total amount invested

    @Column("realized_pnl")
    private BigDecimal realizedPnl; // Profit/loss from closed positions

    @Column("unrealized_pnl")
    private BigDecimal unrealizedPnl; // Current profit/loss

    // Current Market Data (for P&L calculation)
    @Column("current_price_e4")
    private Long currentPriceE4;

    @Column("current_value")
    private BigDecimal currentValue;

    // Trade Statistics
    @Column("total_trades")
    private Long totalTrades;

    @Column("total_bought")
    private BigDecimal totalBought;

    @Column("total_sold")
    private BigDecimal totalSold;

    @Column("total_fees_paid")
    private BigDecimal totalFeesPaid;

    // Resolution
    @Column("is_winner")
    private Boolean isWinner;

    @Column("payout_amount")
    private BigDecimal payoutAmount;

    @Column("payout_status")
    private String payoutStatus; // PENDING, PAID, FAILED

    @Column("paid_at")
    private Instant paidAt;

    // Timestamps
    @Column("first_trade_at")
    private Instant firstTradeAt;

    @Column("last_trade_at")
    private Instant lastTradeAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("version")
    private Long version;
}
