package com.oregonmarkets.domain.market.dto.mapper;

import com.oregonmarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonmarkets.domain.market.dto.response.MarketResponse;
import com.oregonmarkets.domain.market.dto.response.OutcomeResponse;
import com.oregonmarkets.domain.market.model.Category;
import com.oregonmarkets.domain.market.model.Market;
import com.oregonmarkets.domain.market.model.MarketStatus;
import com.oregonmarkets.domain.market.model.Outcome;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapper for Market entities and DTOs
 */
@Component
public class MarketMapper {

    /**
     * Map CreateMarketRequest to Market entity
     */
    public Market toEntity(CreateMarketRequest request, Category category, UUID createdBy) {
        Instant now = Instant.now();
        UUID marketId = UUID.randomUUID();
        String slug = generateSlug(request.getTitle(), marketId);

        return Market.builder()
                .marketId(marketId)
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .longDescription(request.getLongDescription())
                .imageUrl(request.getImageUrl())
                .bannerUrl(request.getBannerUrl())
                .categoryId(category.getCategoryId())
                .categoryName(category.getName())
                .categorySlug(category.getSlug())
                .subcategoryId(request.getSubcategoryId())
                .marketType(request.getMarketType())
                .status(MarketStatus.OPEN.name())
                .marketOpen(now)
                .marketClose(request.getMarketClose())
                .resolutionTime(request.getResolutionTime())
                .resolutionCriteria(request.getResolutionCriteria())
                .resolutionSource(request.getResolutionSource() != null ? request.getResolutionSource() : "MANUAL")
                .minOrderSize(request.getMinOrderSize() != null ? request.getMinOrderSize() : BigDecimal.valueOf(1))
                .maxOrderSize(request.getMaxOrderSize())
                .maxPositionSize(request.getMaxPositionSize())
                .tickSizeE4(request.getTickSizeE4() != null ? request.getTickSizeE4() : 100L)
                .makerFeeE4(request.getMakerFeeE4() != null ? request.getMakerFeeE4() : 10L)
                .takerFeeE4(request.getTakerFeeE4() != null ? request.getTakerFeeE4() : 20L)
                .settlementFeeE4(request.getSettlementFeeE4() != null ? request.getSettlementFeeE4() : 0L)
                .creatorFeeE4(request.getCreatorFeeE4() != null ? request.getCreatorFeeE4() : 5L)
                .oracleId(request.getOracleId())
                .oracleEndpoint(request.getOracleEndpoint())
                .oracleConfig(request.getOracleConfig())
                .blockchainNetwork(request.getBlockchainNetwork())
                .contractAddress(request.getContractAddress())
                .viewTemplateId(request.getViewTemplateId())
                .featured(false)
                .trending(false)
                .displayPriority(0)
                .tags(request.getTags())
                .languageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "en")
                .countryCode(request.getCountryCode())
                .restrictedCountries(request.getRestrictedCountries())
                .kycRequired(request.getKycRequired() != null ? request.getKycRequired() : false)
                .minAge(request.getMinAge() != null ? request.getMinAge() : 18)
                .creatorId(createdBy)
                .totalVolume(BigDecimal.ZERO)
                .volume24h(BigDecimal.ZERO)
                .totalLiquidity(BigDecimal.ZERO)
                .openInterest(BigDecimal.ZERO)
                .totalTraders(0L)
                .totalTrades(0L)
                .totalOrders(0L)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .version(1L)
                .build();
    }

    /**
     * Map CreateMarketRequest.OutcomeRequest to Outcome entity
     */
    public Outcome toOutcomeEntity(UUID marketId, CreateMarketRequest.OutcomeRequest request) {
        Instant now = Instant.now();
        return Outcome.builder()
                .marketId(marketId)
                .outcomeId(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder())
                .color(request.getColor())
                .icon(request.getIcon())
                .currentPriceE4(5000L) // Default 50%
                .lastPriceE4(5000L)
                .bestBidE4(0L)
                .bestAskE4(0L)
                .totalVolume(BigDecimal.ZERO)
                .volume24h(BigDecimal.ZERO)
                .openInterest(BigDecimal.ZERO)
                .totalLiquidity(BigDecimal.ZERO)
                .isWinner(false)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Map Market entity to MarketResponse DTO
     */
    public MarketResponse toResponse(Market market) {
        return MarketResponse.builder()
                .marketId(market.getMarketId())
                .title(market.getTitle())
                .slug(market.getSlug())
                .description(market.getDescription())
                .longDescription(market.getLongDescription())
                .imageUrl(market.getImageUrl())
                .bannerUrl(market.getBannerUrl())
                .categoryId(market.getCategoryId())
                .categoryName(market.getCategoryName())
                .categorySlug(market.getCategorySlug())
                .subcategoryId(market.getSubcategoryId())
                .subcategoryName(market.getSubcategoryName())
                .marketType(market.getMarketType())
                .status(market.getStatus())
                .marketOpen(market.getMarketOpen())
                .marketClose(market.getMarketClose())
                .resolutionTime(market.getResolutionTime())
                .actualResolutionTime(market.getActualResolutionTime())
                .resolutionCriteria(market.getResolutionCriteria())
                .resolutionSource(market.getResolutionSource())
                .resolutionNotes(market.getResolutionNotes())
                .winningOutcomeId(market.getWinningOutcomeId())
                .winningOutcomeName(market.getWinningOutcomeName())
                .minOrderSize(market.getMinOrderSize())
                .maxOrderSize(market.getMaxOrderSize())
                .maxPositionSize(market.getMaxPositionSize())
                .tickSizeE4(market.getTickSizeE4())
                .totalVolume(market.getTotalVolume())
                .volume24h(market.getVolume24h())
                .totalLiquidity(market.getTotalLiquidity())
                .openInterest(market.getOpenInterest())
                .totalTraders(market.getTotalTraders())
                .totalTrades(market.getTotalTrades())
                .totalOrders(market.getTotalOrders())
                .makerFeeE4(market.getMakerFeeE4())
                .takerFeeE4(market.getTakerFeeE4())
                .settlementFeeE4(market.getSettlementFeeE4())
                .creatorFeeE4(market.getCreatorFeeE4())
                .featured(market.getFeatured())
                .trending(market.getTrending())
                .displayPriority(market.getDisplayPriority())
                .tags(market.getTags())
                .languageCode(market.getLanguageCode())
                .countryCode(market.getCountryCode())
                .availableLanguages(market.getAvailableLanguages())
                .restrictedCountries(market.getRestrictedCountries())
                .kycRequired(market.getKycRequired())
                .minAge(market.getMinAge())
                .creatorId(market.getCreatorId())
                .creatorUsername(market.getCreatorUsername())
                .createdAt(market.getCreatedAt())
                .updatedAt(market.getUpdatedAt())
                .version(market.getVersion())
                .build();
    }

    /**
     * Map Outcome entity to OutcomeResponse DTO
     */
    public OutcomeResponse toOutcomeResponse(Outcome outcome) {
        return OutcomeResponse.builder()
                .outcomeId(outcome.getOutcomeId())
                .marketId(outcome.getMarketId())
                .name(outcome.getName())
                .description(outcome.getDescription())
                .displayOrder(outcome.getDisplayOrder())
                .color(outcome.getColor())
                .icon(outcome.getIcon())
                .currentPrice(outcome.getCurrentPriceE4() != null ? outcome.getCurrentPriceE4() / 100.0 : null)
                .currentPriceE4(outcome.getCurrentPriceE4())
                .lastPrice(outcome.getLastPriceE4() != null ? outcome.getLastPriceE4() / 100.0 : null)
                .lastPriceE4(outcome.getLastPriceE4())
                .bestBid(outcome.getBestBidE4() != null ? outcome.getBestBidE4() / 100.0 : null)
                .bestBidE4(outcome.getBestBidE4())
                .bestAsk(outcome.getBestAskE4() != null ? outcome.getBestAskE4() / 100.0 : null)
                .bestAskE4(outcome.getBestAskE4())
                .totalVolume(outcome.getTotalVolume())
                .volume24h(outcome.getVolume24h())
                .openInterest(outcome.getOpenInterest())
                .totalLiquidity(outcome.getTotalLiquidity())
                .isWinner(outcome.getIsWinner())
                .payoutAmount(outcome.getPayoutAmount())
                .enabled(outcome.getEnabled())
                .createdAt(outcome.getCreatedAt())
                .updatedAt(outcome.getUpdatedAt())
                .build();
    }

    /**
     * Generate URL-friendly slug from title and market ID
     */
    public String generateSlug(String title, UUID marketId) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        // Limit to 50 characters
        if (baseSlug.length() > 50) {
            baseSlug = baseSlug.substring(0, 50);
        }

        return baseSlug + "-" + marketId.toString().substring(0, 8);
    }
}
