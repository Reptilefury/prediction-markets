package com.oregonmarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Market entity - Primary table with ALL market data denormalized
 * Table: markets_by_id
 * This is the source of truth for market data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("markets_by_id")
public class Market {

    @PrimaryKey
    @Column("market_id")
    private UUID marketId;

    // Basic Information
    @Column("title")
    private String title;

    @Column("slug")
    private String slug; // URL-friendly identifier

    @Column("description")
    private String description;

    @Column("long_description")
    private String longDescription;

    @Column("image_url")
    private String imageUrl;

    @Column("banner_url")
    private String bannerUrl;

    // Category and Type (Denormalized)
    @Column("category_id")
    private UUID categoryId;

    @Column("category_name")
    private String categoryName; // Denormalized

    @Column("category_slug")
    private String categorySlug; // Denormalized

    @Column("subcategory_id")
    private UUID subcategoryId;

    @Column("subcategory_name")
    private String subcategoryName; // Denormalized

    @Column("market_type")
    private String marketType; // BINARY, MULTIPLE_CHOICE, SCALAR, etc.

    @Column("market_subtype")
    private String marketSubtype; // Optional: OVER_UNDER, PRICE_PREDICTION, etc.

    // Status and Lifecycle
    @Column("status")
    private String status; // OPEN, SUSPENDED, CLOSED, RESOLVED, CANCELLED

    @Column("market_open")
    private Instant marketOpen; // When trading starts

    @Column("market_close")
    private Instant marketClose; // When trading ends

    @Column("resolution_time")
    private Instant resolutionTime; // Expected resolution time

    @Column("actual_resolution_time")
    private Instant actualResolutionTime;

    // Resolution
    @Column("resolution_criteria")
    private String resolutionCriteria;

    @Column("resolution_source")
    private String resolutionSource; // MANUAL, ORACLE, API, VOTING, BLOCKCHAIN

    @Column("resolution_notes")
    private String resolutionNotes;

    @Column("resolved_by")
    private UUID resolvedBy; // User ID of resolver

    @Column("winning_outcome_id")
    private UUID winningOutcomeId;

    @Column("winning_outcome_name")
    private String winningOutcomeName; // Denormalized

    // Trading Limits
    @Column("min_order_size")
    private BigDecimal minOrderSize;

    @Column("max_order_size")
    private BigDecimal maxOrderSize;

    @Column("max_position_size")
    private BigDecimal maxPositionSize;

    @Column("tick_size_e4")
    private Long tickSizeE4; // Minimum price increment (e.g., 100 = 1%)

    // Financial Data
    @Column("total_volume")
    private BigDecimal totalVolume;

    @Column("volume_24h")
    private BigDecimal volume24h;

    @Column("total_liquidity")
    private BigDecimal totalLiquidity;

    @Column("open_interest")
    private BigDecimal openInterest;

    @Column("total_traders")
    private Long totalTraders;

    @Column("total_trades")
    private Long totalTrades;

    @Column("total_orders")
    private Long totalOrders;

    // Fee Configuration
    @Column("maker_fee_e4")
    private Long makerFeeE4; // Maker fee in basis points (e.g., 10 = 0.10%)

    @Column("taker_fee_e4")
    private Long takerFeeE4; // Taker fee in basis points

    @Column("settlement_fee_e4")
    private Long settlementFeeE4;

    // Oracle Configuration (if applicable)
    @Column("oracle_id")
    private UUID oracleId;

    @Column("oracle_endpoint")
    private String oracleEndpoint;

    @Column("oracle_config")
    private String oracleConfig; // JSON configuration

    @Column("oracle_last_update")
    private Instant oracleLastUpdate;

    // Blockchain Configuration (if applicable)
    @Column("blockchain_network")
    private String blockchainNetwork; // ethereum, polygon, etc.

    @Column("contract_address")
    private String contractAddress;

    @Column("block_height")
    private Long blockHeight;

    // View and Display
    @Column("view_template_id")
    private UUID viewTemplateId;

    @Column("view_config")
    private String viewConfig;

    @Column("featured_view_template_id")
    private UUID featuredViewTemplateId;

    @Column("featured_view_config")
    private String featuredViewConfig;

    @Column("featured_rank")
    private Integer featuredRank;

    @Column("featured")
    private Boolean featured;

    @Column("trending")
    private Boolean trending;

    @Column("display_priority")
    private Integer displayPriority;

    // Tags and Search
    @Column("tags")
    private List<String> tags;

    @Column("search_keywords")
    private List<String> searchKeywords;

    // Localization
    @Column("language_code")
    private String languageCode; // Default language

    @Column("country_code")
    private String countryCode; // Geographic restriction

    @Column("available_languages")
    private List<String> availableLanguages;

    // Permissions and Restrictions
    @Column("restricted_countries")
    private List<String> restrictedCountries;

    @Column("kyc_required")
    private Boolean kycRequired;

    @Column("min_age")
    private Integer minAge;

    // Creator Information
    @Column("creator_id")
    private UUID creatorId;

    @Column("creator_username")
    private String creatorUsername; // Denormalized

    @Column("creator_fee_e4")
    private Long creatorFeeE4; // Creator's fee share

    // External Market Integration
    @Column("source")
    private String source; // native, polymarket, kalshi, predictit, manifold

    @Column("source_market_id")
    private String sourceMarketId; // External market ID

    @Column("markup")
    private BigDecimal markup; // Price markup percentage

    @Column("sync_status")
    private String syncStatus; // synced, syncing, error, paused

    @Column("last_synced_at")
    private Instant lastSyncedAt;

    // Market Extension Data (for specialized markets)
    @Column("sports_data")
    private String sportsData; // JSON for sports-specific fields

    @Column("election_data")
    private String electionData; // JSON for election-specific fields

    @Column("crypto_data")
    private String cryptoData; // JSON for crypto-specific fields

    @Column("custom_metadata")
    private Map<String, String> customMetadata;

    // Audit Fields
    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("created_by")
    private UUID createdBy;

    @Column("updated_by")
    private UUID updatedBy;

    @Column("version")
    private Long version; // Optimistic locking
}
