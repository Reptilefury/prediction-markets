package com.oregonmarkets.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * QuestDB Configuration using PostgreSQL wire protocol
 * QuestDB listens on port 8812 for PostgreSQL wire protocol
 */
@Slf4j
@Profile("!test")
@Configuration
public class QuestDbConfig {

    @Value("${app.questdb.host}")
    private String questdbHost;

    @Value("${app.questdb.port}")
    private int questdbPort;

    @Value("${app.questdb.database}")
    private String questdbDatabase;

    @Value("${app.questdb.username}")
    private String questdbUsername;

    @Value("${app.questdb.password}")
    private String questdbPassword;

    @Bean("questdbConnectionFactory")
    public ConnectionFactory questdbConnectionFactory() {
        log.info("Configuring QuestDB connection: {}:{}/{}", questdbHost, questdbPort, questdbDatabase);
        
        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(questdbHost)
                .port(questdbPort)
                .database(questdbDatabase)
                .username(questdbUsername)
                .password(questdbPassword)
                .build()
        );
    }

    @Bean("questdbDatabaseClient")
    public DatabaseClient questdbDatabaseClient() {
        return DatabaseClient.create(questdbConnectionFactory());
    }

    @Bean("questdbEntityTemplate")
    public R2dbcEntityTemplate questdbEntityTemplate() {
        return new R2dbcEntityTemplate(questdbConnectionFactory());
    }
}
