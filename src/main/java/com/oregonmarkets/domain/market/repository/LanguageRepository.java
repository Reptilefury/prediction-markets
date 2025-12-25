package com.oregonmarkets.domain.market.repository;

import com.oregonmarkets.domain.market.model.Language;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for Language entity (languages table)
 */
@Repository
public interface LanguageRepository extends ReactiveCassandraRepository<Language, String> {

    /**
     * Find all enabled languages
     */
    @Query("SELECT * FROM languages WHERE enabled = true ALLOW FILTERING")
    Flux<Language> findAllEnabled();
}
