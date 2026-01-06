package com.oregonmarkets.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Default R2DBC Configuration for PostgreSQL (Supabase)
 * Enables repositories for R2DBC domains
 * Disabled in test profile to allow test configuration to take precedence
 *
 * IMPORTANT: Explicitly configures the schema to 'public' to ensure tables are found.
 * This is critical when multiple databases (QuestDB + Supabase) use the same R2DBC driver.
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
@EnableR2dbcRepositories(
    basePackages = {
        "com.oregonmarkets.domain.blockchain.repository",
        "com.oregonmarkets.domain.user.repository",
        "com.oregonmarkets.domain.enclave.repository",
        "com.oregonmarkets.domain.payment.repository",
        "com.oregonmarkets.domain.admin.repository"
    }
)
public class DefaultR2dbcConfig {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    /**
     * Primary ConnectionFactory for Supabase PostgreSQL
     * Explicitly sets schema to 'public' via PostgreSQL connection initialization
     */
    @Primary
    @Bean
    public ConnectionFactory connectionFactory() {
        log.info("Creating primary PostgreSQL ConnectionFactory for Supabase");
        log.info("R2DBC URL: {}", r2dbcUrl.replaceAll(":.*@", ":***@"));

        // Parse R2DBC URL to extract host, port, database
        // Format: r2dbc:postgresql://[user:password@]host:port/database?params
        String cleanUrl = r2dbcUrl.replace("r2dbc:postgresql://", "");

        // Remove credentials if present (format: user:password@)
        if (cleanUrl.contains("@")) {
            cleanUrl = cleanUrl.substring(cleanUrl.indexOf("@") + 1);
        }

        String[] hostAndRest = cleanUrl.split("/");
        String[] hostPort = hostAndRest[0].split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
        String database = hostAndRest.length > 1 ? hostAndRest[1].split("\\?")[0] : "postgres";

        log.info("Connecting to PostgreSQL: host={}, port={}, database={}, schema=public", host, port, database);

        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .schema("public")  // Explicitly set schema to 'public'
                .build()
        );
    }

    @Primary
    @Bean
    @ConditionalOnMissingBean(R2dbcEntityTemplate.class)
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        log.info("Creating primary R2dbcEntityTemplate with ConnectionFactory: {}", connectionFactory.getMetadata().getName());
        return new R2dbcEntityTemplate(connectionFactory);
    }
}