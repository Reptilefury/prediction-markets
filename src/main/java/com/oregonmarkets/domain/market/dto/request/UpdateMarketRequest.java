package com.oregonmarkets.domain.market.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for updating an existing market
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMarketRequest {

    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    private String title;

    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    private String longDescription;

    private String marketSubtype;

    @Pattern(regexp = "OPEN|SUSPENDED|CLOSED|RESOLVED|CANCELLED", message = "Invalid market status")
    private String status;

    @Future(message = "Market close time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant marketClose;

    @Future(message = "Resolution time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant resolutionTime;

    @Size(min = 20, max = 5000, message = "Resolution criteria must be between 20 and 5000 characters")
    private String resolutionCriteria;

    private String imageUrl;

    private String bannerUrl;

    private Boolean featured;

    private Boolean trending;

    private Integer displayPriority;

    private List<String> tags;

    private List<String> restrictedCountries;

    private Boolean kycRequired;

    private Integer minAge;

    private UUID viewTemplateId;
    private String viewConfig;

    private Map<String, String> customMetadata;

    @Pattern(regexp = "native|polymarket|kalshi|predictit|manifold", message = "Invalid source")
    private String source;

    private String sourceMarketId;

    private java.math.BigDecimal markup;

    @Pattern(regexp = "synced|syncing|error|paused", message = "Invalid sync status")
    private String syncStatus;
}
