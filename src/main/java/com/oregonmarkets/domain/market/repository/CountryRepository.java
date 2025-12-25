package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.Country;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for Country entity (countries table)
 */
@Repository
public interface CountryRepository extends ReactiveCassandraRepository<Country, String> {

    /**
     * Find all enabled countries
     */
    @Query("SELECT * FROM countries WHERE enabled = true ALLOW FILTERING")
    Flux<Country> findAllEnabled();

    /**
     * Find countries by currency code
     */
    @Query("SELECT * FROM countries WHERE currency_code = ?0 ALLOW FILTERING")
    Flux<Country> findByCurrencyCode(String currencyCode);
}
