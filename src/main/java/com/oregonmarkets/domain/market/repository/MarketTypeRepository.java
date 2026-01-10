package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.MarketTypeEntity;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketTypeRepository extends ReactiveCassandraRepository<MarketTypeEntity, String> {
}
