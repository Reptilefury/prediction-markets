package com.oregonMarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Category reference data entity
 * Table: categories
 * Categories: Sports, Politics, Cryptocurrency, Business, Entertainment, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("categories")
public class Category {

    @PrimaryKey
    @Column("category_id")
    private UUID categoryId;

    @Column("name")
    private String name;

    @Column("slug")
    private String slug; // URL-friendly name

    @Column("description")
    private String description;

    @Column("icon")
    private String icon; // Icon identifier or emoji

    @Column("color")
    private String color; // Hex color code for UI

    @Column("display_order")
    private Integer displayOrder;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
