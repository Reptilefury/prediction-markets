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
 * Language reference data entity
 * Table: languages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("languages")
public class Language {

    @PrimaryKey
    @Column("code")
    private String code; // e.g., "en", "es", "fr"

    @Column("name")
    private String name; // e.g., "English", "Spanish"

    @Column("native_name")
    private String nativeName; // e.g., "English", "Español"

    @Column("rtl")
    private Boolean rtl; // Right-to-left language flag

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
