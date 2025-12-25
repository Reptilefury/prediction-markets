package com.oregonmarkets.domain.market.dto.mapper;

import com.oregonmarkets.domain.market.dto.request.CreateMarketRequest;
import com.oregonmarkets.domain.market.model.Category;
import com.oregonmarkets.domain.market.model.Market;
import com.oregonmarkets.domain.market.model.MarketStatus;
import com.oregonmarkets.domain.market.model.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMapperTest {

    private MarketMapper marketMapper;

    @BeforeEach
    void setUp() {
        marketMapper = new MarketMapper();
    }

    @Test
    void toEntity_ShouldMapBasicFields() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setTitle("Test Market");
        request.setDescription("Test description");
        request.setMarketType("BINARY");
        request.setMarketClose(Instant.now().plusSeconds(3600));
        request.setResolutionTime(Instant.now().plusSeconds(7200));

        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Politics");

        UUID createdBy = UUID.randomUUID();

        // When
        Market market = marketMapper.toEntity(request, category, createdBy);

        // Then
        assertThat(market).isNotNull();
        assertThat(market.getMarketId()).isNotNull();
        assertThat(market.getTitle()).isEqualTo("Test Market");
        assertThat(market.getDescription()).isEqualTo("Test description");
        assertThat(market.getMarketType()).isEqualTo("BINARY");
        assertThat(market.getCategoryId()).isEqualTo(category.getCategoryId());
        assertThat(market.getCategoryName()).isEqualTo("Politics");
        assertThat(market.getStatus()).isEqualTo(MarketStatus.OPEN.name());
        assertThat(market.getCreatorId()).isEqualTo(createdBy);
        assertThat(market.getVersion()).isEqualTo(1L);
    }

    @Test
    void toEntity_ShouldUseDefaults() {
        // Given
        CreateMarketRequest request = new CreateMarketRequest();
        request.setTitle("Simple Market");
        request.setMarketClose(Instant.now().plusSeconds(3600));
        request.setResolutionTime(Instant.now().plusSeconds(7200));

        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Sports");

        UUID createdBy = UUID.randomUUID();

        // When
        Market market = marketMapper.toEntity(request, category, createdBy);

        // Then
        assertThat(market.getResolutionSource()).isEqualTo("MANUAL");
        assertThat(market.getMinOrderSize()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(market.getTickSizeE4()).isEqualTo(100L);
        assertThat(market.getMakerFeeE4()).isEqualTo(10L);
        assertThat(market.getTakerFeeE4()).isEqualTo(20L);
        assertThat(market.getFeatured()).isFalse();
        assertThat(market.getTrending()).isFalse();
        assertThat(market.getTotalVolume()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void toOutcomeEntity_ShouldMapFields() {
        // Given
        UUID marketId = UUID.randomUUID();
        CreateMarketRequest.OutcomeRequest request = new CreateMarketRequest.OutcomeRequest();
        request.setName("Yes");
        request.setDescription("Yes outcome");
        request.setDisplayOrder(1);
        request.setColor("#00FF00");

        // When
        Outcome outcome = marketMapper.toOutcomeEntity(marketId, request);

        // Then
        assertThat(outcome).isNotNull();
        assertThat(outcome.getMarketId()).isEqualTo(marketId);
        assertThat(outcome.getOutcomeId()).isNotNull();
        assertThat(outcome.getName()).isEqualTo("Yes");
        assertThat(outcome.getDescription()).isEqualTo("Yes outcome");
        assertThat(outcome.getDisplayOrder()).isEqualTo(1);
        assertThat(outcome.getColor()).isEqualTo("#00FF00");
    }

    @Test
    void generateSlug_ShouldCreateValidSlug() {
        // When
        String slug1 = marketMapper.generateSlug("Will Bitcoin reach $100k?", UUID.randomUUID());
        String slug2 = marketMapper.generateSlug("Test Market", UUID.randomUUID());

        // Then
        assertThat(slug1).isNotNull();
        assertThat(slug1).containsPattern("[a-z0-9-]+");
        assertThat(slug2).isNotNull();
        assertThat(slug2).containsPattern("[a-z0-9-]+");
    }

    @Test
    void toResponse_ShouldMapMarket() {
        // Given
        Market market = new Market();
        market.setMarketId(UUID.randomUUID());
        market.setTitle("Test Market");
        market.setSlug("test-market");
        market.setStatus(MarketStatus.OPEN.name());
        market.setMarketType("BINARY");
        market.setTotalVolume(BigDecimal.valueOf(1000));
        market.setTotalTraders(50L);

        // When
        var response = marketMapper.toResponse(market);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMarketId()).isEqualTo(market.getMarketId());
        assertThat(response.getTitle()).isEqualTo("Test Market");
        assertThat(response.getSlug()).isEqualTo("test-market");
        assertThat(response.getStatus()).isEqualTo(MarketStatus.OPEN.name());
    }

    @Test
    void toOutcomeResponse_ShouldMapOutcome() {
        // Given
        Outcome outcome = new Outcome();
        outcome.setOutcomeId(UUID.randomUUID());
        outcome.setMarketId(UUID.randomUUID());
        outcome.setName("Yes");
        outcome.setCurrentPriceE4(5500L);
        outcome.setTotalVolume(BigDecimal.valueOf(1000));
        outcome.setIsWinner(false);

        // When
        var response = marketMapper.toOutcomeResponse(outcome);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOutcomeId()).isEqualTo(outcome.getOutcomeId());
        assertThat(response.getName()).isEqualTo("Yes");
        assertThat(response.getTotalVolume()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(response.getIsWinner()).isFalse();
    }
}
