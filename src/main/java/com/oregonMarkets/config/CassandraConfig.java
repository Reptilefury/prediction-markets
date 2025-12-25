package com.oregonmarkets.config;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

/**
 * Cassandra configuration for DataStax Astra DB
 * Uses secure connect bundle for authentication
 */
@Slf4j
@Profile("!test")
@Configuration
@EnableReactiveCassandraRepositories(
    basePackages = {
        "com.oregonMarkets.domain.market.repository"
    })
public class CassandraConfig extends AbstractReactiveCassandraConfiguration {

    @Value("${app.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${app.cassandra.username}")
    private String username;

    @Value("${app.cassandra.password}")
    private String password;


    @Value("${app.cassandra.secure-connect-bundle:/app/secure-connect/secure-connect-cassandra.zip}")
    private String secureConnectBundlePath;

    @Override
    protected String getKeyspaceName() {
        log.info("Configuring Cassandra with keyspace: {}", keyspaceName);
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        // When using secure connect bundle, contact points are provided by the bundle.
        // Returning empty string prevents the warning about mutually exclusive properties.
        return "";
    }

    @Override
    public SchemaAction getSchemaAction() {
        // Disable schema actions since tables already exist in Astra DB
        return SchemaAction.NONE;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{
            "com.oregonMarkets.domain.market.model",
            "com.oregonMarkets.domain.enclave.model"
        };
    }

    @Bean
    @Override
    public CqlSessionFactoryBean cassandraSession() {
        log.info("Using Astra DB secure connect bundle: {}", secureConnectBundlePath);
        Path bundlePath = Paths.get(secureConnectBundlePath);

        if (!bundlePath.toFile().exists()) {
            log.warn("⚠️  Secure connect bundle NOT found at: {}", secureConnectBundlePath);
            log.warn("⚠️  Please set the ASTRA_SECURE_CONNECT_BUNDLE environment variable");
            log.warn("⚠️  Example: export ASTRA_SECURE_CONNECT_BUNDLE=/path/to/secure-connect-cassandra.zip");
            log.warn("⚠️  If you don't have Cassandra/Astra DB set up, you can disable it temporarily");
            throw new IllegalArgumentException(
                "Secure connect bundle not found at: " + secureConnectBundlePath +
                "\n\nTo fix this issue:" +
                "\n1. Download your secure-connect-cassandra.zip from DataStax Astra DB" +
                "\n2. Set environment variable: export ASTRA_SECURE_CONNECT_BUNDLE=/path/to/bundle.zip" +
                "\n3. Restart the application" +
                "\n\nOr to temporarily disable Cassandra, set: export SPRING_PROFILES_ACTIVE=test"
            );
        }

        // Programmatic driver configuration as a safety net in IDE/dev environments
        // This complements application.conf and ensures we don't hit PT2S timeouts during initialization
        DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
            .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(60))
            .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(60))
            .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(60))
            // Keep connections healthy to reduce "Session is closed" issues on idle links
            .withDuration(DefaultDriverOption.HEARTBEAT_INTERVAL, Duration.ofSeconds(30))
            .withDuration(DefaultDriverOption.HEARTBEAT_TIMEOUT, Duration.ofSeconds(10))
            // Try to recover automatically on transient disconnects
            .withClass(DefaultDriverOption.RECONNECTION_POLICY_CLASS,
                com.datastax.oss.driver.internal.core.connection.ExponentialReconnectionPolicy.class)
            .withDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY, Duration.ofSeconds(1))
            .withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY, Duration.ofSeconds(30))
            .build();

        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        
        // Set keyspace explicitly
        session.setKeyspaceName(keyspaceName);
        
        session.setSessionBuilderConfigurer(builder -> {
            log.info("Configuring main application session with keyspace: {}", keyspaceName);
            return builder.withCloudSecureConnectBundle(bundlePath)
                   .withAuthCredentials(username, password)
                   .withKeyspace(keyspaceName)
                   .withConfigLoader(configLoader);
        });

        log.info("Cassandra session configured with secure connect bundle and keyspace: {}", keyspaceName);
        return session;
    }
}
