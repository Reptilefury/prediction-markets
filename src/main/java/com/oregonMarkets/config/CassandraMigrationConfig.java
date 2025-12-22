package com.oregonMarkets.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cognitor.cassandra.migration.Database;
import org.cognitor.cassandra.migration.MigrationConfiguration;
import org.cognitor.cassandra.migration.MigrationRepository;
import org.cognitor.cassandra.migration.MigrationTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Cassandra Migration Configuration
 * Automatically runs CQL migration scripts on application startup
 *
 * Migration files naming: {numericVersion}__{description}.cql
 * Example: 001__create_keyspace_and_tables.cql
 *
 * Features:
 * - Version tracking (stores applied migrations in cassandra_migration_version table)
 * - Only runs new migrations
 * - Idempotent (safe to run multiple times)
 * - Supports rollback scenarios
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CassandraMigrationConfig {

    // Note: CqlSession is NOT injected here to avoid sharing the main application session.
    // We create a dedicated, short-lived session for migrations to ensure isolation and
    // prevent the migration tool from potentially closing the main session.

    @Value("${app.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${app.cassandra.username}")
    private String username;

    @Value("${app.cassandra.password}")
    private String password;

    @Value("${app.cassandra.secure-connect-bundle:/home/user/Downloads/secure-connect-cassandra.zip}")
    private String secureConnectBundlePath;

    @Value("${cassandra.migration.scripts-location:cassandra/migrations}")
    private String scriptsLocation;

    @Value("${cassandra.migration.enabled:false}")
    private boolean migrationEnabled;

    /**
     * Run migrations after application is fully started
     * Uses a standalone CqlSession to avoid side effects on the main application session.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        if (!migrationEnabled) {
            log.info("Cassandra migrations are disabled");
            return;
        }

        log.info("==================================================");
        log.info("Starting Cassandra Migrations");
        log.info("Keyspace: {}", keyspaceName);
        log.info("Scripts Location: {}", scriptsLocation);
        log.info("Using separate session for migration to ensure isolation");
        log.info("==================================================");

        Path bundlePath = Paths.get(secureConnectBundlePath);
        if (!bundlePath.toFile().exists()) {
            log.error("Secure connect bundle not found at: {}", secureConnectBundlePath);
            log.error("Migration skipped due to missing bundle.");
            return;
        }

        // Configure driver specifically for migration (robust timeouts)
        DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
            .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(60))
            .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(60))
            .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(60))
            .build();

        // Use try-with-resources to automatically close the migration session
        try (CqlSession migrationSession = CqlSession.builder()
                .withCloudSecureConnectBundle(bundlePath)
                .withAuthCredentials(username, password)
                .withKeyspace(keyspaceName)
                .withConfigLoader(configLoader)
                .build()) {

            log.info("Migration session established successfully");
            log.info("Connected to keyspace: {}", migrationSession.getKeyspace().map(Object::toString).orElse("NONE"));

            // Create migration configuration for Astra DB
            MigrationConfiguration config = new MigrationConfiguration()
                .withKeyspaceName(keyspaceName);

            // Create database wrapper
            Database database = new Database(migrationSession, config);

            // Create migration repository (reads migration scripts from classpath)
            MigrationRepository repository = new MigrationRepository(scriptsLocation);
            
            try {
                // This method is not public in all versions, but if available, useful for debugging
                // int scriptCount = repository.getMigrations(database).size(); 
                // log.info("Found migration scripts in {}: (count check skipped)", scriptsLocation);
            } catch (Exception ignored) {}

            // Create and execute migration task
            MigrationTask migration = new MigrationTask(database, repository);

            // Execute migrations
            migration.migrate();

            log.info("==================================================");
            log.info("Cassandra Migrations Completed Successfully");
            
            // List tables to verify creation
            try {
                log.info("Verifying tables in keyspace '{}':", keyspaceName);
                migrationSession.getMetadata()
                    .getKeyspace(keyspaceName)
                    .ifPresentOrElse(
                        ks -> ks.getTables().forEach((id, table) -> log.info(" - Table found: {}", table.getName())),
                        () -> log.warn("Keyspace '{}' not found in metadata!", keyspaceName)
                    );
            } catch (Exception e) {
                log.warn("Could not verify tables: {}", e.getMessage());
            }
            
            log.info("==================================================");

        } catch (Exception e) {
            log.error("==================================================");
            log.error("FAILED to execute Cassandra migrations", e);
            log.error("==================================================");
            
            // Fail fast to prevent application from starting with an invalid database state
            throw new RuntimeException("Cassandra migration failed", e);
        }
    }
}