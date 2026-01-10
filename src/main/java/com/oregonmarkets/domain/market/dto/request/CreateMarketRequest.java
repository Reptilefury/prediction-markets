package com.oregonmarkets.domain.market.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new market
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMarketRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    private String longDescription;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private UUID subcategoryId;

    @NotNull(message = "Market type is required")
    @Pattern(regexp = "BINARY|MULTIPLE_CHOICE|SCALAR|RANGE|CATEGORICAL|CUSTOM|COMBINATORIAL|POOL",
             message = "Invalid market type")
    private String marketType;

    private String marketSubtype; // Optional: OVER_UNDER, PRICE_PREDICTION, etc.

    @NotNull(message = "Market close time is required")
    @Future(message = "Market close time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant marketClose;

    @NotNull(message = "Resolution time is required")
    @Future(message = "Resolution time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant resolutionTime;

    @NotBlank(message = "Resolution criteria is required")
    @Size(min = 20, max = 5000, message = "Resolution criteria must be between 20 and 5000 characters")
    private String resolutionCriteria;

    @Pattern(regexp = "MANUAL|ORACLE|API|VOTING|BLOCKCHAIN", message = "Invalid resolution source")
    private String resolutionSource;

    @NotNull(message = "Outcomes are required")
    @Size(min = 2, message = "At least 2 outcomes are required")
    @Valid
    private List<OutcomeRequest> outcomes;

    private String imageUrl;

    private String bannerUrl;

    // Trading limits
    @DecimalMin(value = "0.01", message = "Minimum order size must be at least 0.01")
    private BigDecimal minOrderSize;

    @DecimalMin(value = "0.01", message = "Maximum order size must be at least 0.01")
    private BigDecimal maxOrderSize;

    private BigDecimal maxPositionSize;

    private Long tickSizeE4;

    // Fee configuration
    private Long makerFeeE4;

    private Long takerFeeE4;

    private Long settlementFeeE4;

    private Long creatorFeeE4;

    // Oracle configuration (optional)
    private UUID oracleId;

    private String oracleEndpoint;

    private String oracleConfig;

    // Blockchain configuration (optional)
    private String blockchainNetwork;

    private String contractAddress;

    // Display and localization
    private UUID viewTemplateId;
    private String viewConfig;

    private List<String> tags;

    private String languageCode;

    private String countryCode;

    private List<String> restrictedCountries;

    // Restrictions
    private Boolean kycRequired;

    private Integer minAge;

    // Extension data
    private Map<String, Object> sportsData;

    private Map<String, Object> electionData;

    private Map<String, Object> cryptoData;

    private Map<String, String> customMetadata;

    // External market integration
    @Pattern(regexp = "native|polymarket|kalshi|predictit|manifold", message = "Invalid source")
    private String source; // Default: native

    private String sourceMarketId; // External market ID

    private BigDecimal markup; // Price markup percentage

    /**
     * Nested DTO for outcome creation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeRequest {

        @NotBlank(message = "Outcome name is required")
        @Size(min = 1, max = 100, message = "Outcome name must be between 1 and 100 characters")
        private String name;

        private String description;

        @Min(value = 0, message = "Display order must be non-negative")
        private Integer displayOrder;

        private String color;

        private String icon;
    }
}
