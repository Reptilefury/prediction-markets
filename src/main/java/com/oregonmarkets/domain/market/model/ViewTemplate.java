package com.oregonmarkets.domain.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * View template reference data entity
 * Table: view_templates
 * Defines UI layout and components for different market types
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("view_templates")
public class ViewTemplate {

    @PrimaryKey
    @Column("template_id")
    private UUID templateId;

    @Column("name")
    private String name; // e.g., "Default", "Sports", "Elections"

    @Column("description")
    private String description;

    @Column("layout_config")
    private String layoutConfig; // JSON configuration

    @Column("enabled_components")
    private List<String> enabledComponents; // List of component IDs

    @Column("custom_styles")
    private Map<String, String> customStyles;

    @Column("default_for_categories")
    private List<UUID> defaultForCategories;

    @Column("enabled")
    private Boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
