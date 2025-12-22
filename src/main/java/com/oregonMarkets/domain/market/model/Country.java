package com.oregonMarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

/**
 * Country reference data entity
 * Table: countries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("countries")
public class Country {

    @PrimaryKey
    @Column("iso_code")
    private String isoCode; // e.g., "US", "GB", "FR"

    @Column("name")
    private String name; // e.g., "United States"

    @Column("flag_emoji")
    private String flagEmoji; // e.g., "🇺🇸"

    @Column("phone_code")
    private String phoneCode; // e.g., "+1"

    @Column("currency_code")
    private String currencyCode; // e.g., "USD"

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
