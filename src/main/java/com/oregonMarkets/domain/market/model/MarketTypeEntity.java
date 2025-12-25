package com.oregonmarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

/**
 * Market type reference data entity
 * Table: market_types
 * Different from MarketType enum - this is the persistent entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("market_types")
public class MarketTypeEntity {

    @PrimaryKey
    @Column("type")
    private String type; // BINARY, MULTIPLE_CHOICE, SCALAR, etc.

    @Column("name")
    private String name; // Display name

    @Column("description")
    private String description;

    @Column("min_outcomes")
    private Integer minOutcomes;

    @Column("max_outcomes")
    private Integer maxOutcomes;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
