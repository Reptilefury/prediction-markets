package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.ViewTemplate;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for ViewTemplate entity (view_templates table)
 */
@Repository
public interface ViewTemplateRepository extends ReactiveCassandraRepository<ViewTemplate, UUID> {
}
