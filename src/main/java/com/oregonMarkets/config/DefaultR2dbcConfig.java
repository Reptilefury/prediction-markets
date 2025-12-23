package com.oregonMarkets.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Default R2DBC Configuration for PostgreSQL
 * Enables repositories for R2DBC domains
 * Disabled in test profile to allow test configuration to take precedence
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
@EnableR2dbcRepositories(
    basePackages = {
        "com.oregonMarkets.domain.blockchain.repository",
        "com.oregonMarkets.domain.user.repository",
        "com.oregonMarkets.domain.enclave.repository",
        "com.oregonMarkets.domain.payment.repository"
    }
)
public class DefaultR2dbcConfig {

    @Primary
    @Bean
    @ConditionalOnMissingBean(R2dbcEntityTemplate.class)
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        log.info("Creating primary R2dbcEntityTemplate with ConnectionFactory: {}", connectionFactory.getMetadata().getName());
        return new R2dbcEntityTemplate(connectionFactory);
    }
}