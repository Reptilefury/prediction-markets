package com.oregonMarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Subcategory reference data entity
 * Table: subcategories
 * Hierarchical structure under categories
 * Examples: NBA under Basketball under Sports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subcategories")
public class Subcategory {

    @PrimaryKeyColumn(name = "category_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID categoryId;

    @PrimaryKeyColumn(name = "subcategory_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID subcategoryId;

    @Column("parent_subcategory_id")
    private UUID parentSubcategoryId; // For nested hierarchies

    @Column("name")
    private String name;

    @Column("slug")
    private String slug;

    @Column("description")
    private String description;

    @Column("level")
    private Integer level; // Hierarchy level (1, 2, 3...)

    @Column("path")
    private String path; // Full path: "Sports > Basketball > NBA"

    @Column("display_order")
    private Integer displayOrder;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
