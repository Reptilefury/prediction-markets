package com.oregonMarkets.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.r2dbc.core.DatabaseClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/**
 * QuestDB Migration Configuration
 * Runs SQL migration scripts on application startup
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuestDbMigrationConfig {

    private final ResourceLoader resourceLoader;
    private final ApplicationContext applicationContext;

    @Value("${app.questdb.migration.enabled:true}")
    private boolean migrationEnabled;

    @Value("${app.questdb.migration.scripts-location:questdb/migrations}")
    private String scriptsLocation;

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        if (!migrationEnabled) {
            log.info("QuestDB migrations are disabled");
            return;
        }

        log.info("==================================================");
        log.info("Starting QuestDB Migrations");
        log.info("Scripts Location: {}", scriptsLocation);
        log.info("==================================================");

        try {
            // Get QuestDB DatabaseClient bean
            DatabaseClient questdbDatabaseClient = applicationContext.getBean("questdbDatabaseClient", DatabaseClient.class);
            // Get all migration files
            String locationPattern = "classpath:" + scriptsLocation + "/*.sql";
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                .getResources(locationPattern);

            // Sort by filename (V001, V002, etc.)
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            log.info("Found {} migration scripts", resources.length);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                log.info("Executing migration: {}", filename);

                String sql = resource.getContentAsString(StandardCharsets.UTF_8);
                
                // Execute migration
                questdbDatabaseClient.sql(sql)
                    .then()
                    .block();

                log.info("✅ Migration completed: {}", filename);
            }

            log.info("==================================================");
            log.info("QuestDB Migrations Completed Successfully");
            log.info("==================================================");

        } catch (IOException e) {
            log.error("Failed to load migration scripts", e);
            throw new RuntimeException("QuestDB migration failed", e);
        } catch (Exception e) {
            log.error("Failed to execute QuestDB migrations", e);
            throw new RuntimeException("QuestDB migration failed", e);
        }
    }
}
