package com.oregonMarkets.domain.market.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for market data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketResponse {

    private UUID marketId;
    private String title;
    private String slug;
    private String description;
    private String longDescription;
    private String imageUrl;
    private String bannerUrl;

    // Category
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private UUID subcategoryId;
    private String subcategoryName;

    // Type and Status
    private String marketType;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant marketOpen;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant marketClose;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant resolutionTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant actualResolutionTime;

    // Resolution
    private String resolutionCriteria;
    private String resolutionSource;
    private String resolutionNotes;
    private UUID winningOutcomeId;
    private String winningOutcomeName;

    // Trading Limits
    private BigDecimal minOrderSize;
    private BigDecimal maxOrderSize;
    private BigDecimal maxPositionSize;
    private Long tickSizeE4;

    // Financial Data
    private BigDecimal totalVolume;
    private BigDecimal volume24h;
    private BigDecimal totalLiquidity;
    private BigDecimal openInterest;
    private Long totalTraders;
    private Long totalTrades;
    private Long totalOrders;

    // Fees
    private Long makerFeeE4;
    private Long takerFeeE4;
    private Long settlementFeeE4;
    private Long creatorFeeE4;

    // Display
    private Boolean featured;
    private Boolean trending;
    private Integer displayPriority;
    private List<String> tags;

    // Localization
    private String languageCode;
    private String countryCode;
    private List<String> availableLanguages;
    private List<String> restrictedCountries;

    // Restrictions
    private Boolean kycRequired;
    private Integer minAge;

    // Creator
    private UUID creatorId;
    private String creatorUsername;

    // Outcomes
    private List<OutcomeResponse> outcomes;

    // Extension data
    private Map<String, Object> sportsData;
    private Map<String, Object> electionData;
    private Map<String, Object> cryptoData;
    private Map<String, String> customMetadata;

    // Audit
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant updatedAt;

    private Long version;
}
