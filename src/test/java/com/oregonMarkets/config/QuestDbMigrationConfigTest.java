package com.oregonmarkets.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

class QuestDbMigrationConfigTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private DatabaseClient databaseClient;
  @Mock private DatabaseClient.GenericExecuteSpec executeSpec;

  private QuestDbMigrationConfig config;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(applicationContext.getBean("questdbDatabaseClient", DatabaseClient.class))
        .thenReturn(databaseClient);
    when(databaseClient.sql(anyString())).thenReturn(executeSpec);
    when(executeSpec.then()).thenReturn(Mono.empty());

    config = new QuestDbMigrationConfig(new DefaultResourceLoader(), applicationContext);
  }

  @Test
  void runMigrations_Disabled_DoesNothing() {
    ReflectionTestUtils.setField(config, "migrationEnabled", false);

    config.runMigrations();

    verify(databaseClient, never()).sql(anyString());
  }

  @Test
  void runMigrations_ExecutesScripts() {
    ReflectionTestUtils.setField(config, "migrationEnabled", true);
    ReflectionTestUtils.setField(config, "scriptsLocation", "questdb/migrations");

    config.runMigrations();

    verify(databaseClient, atLeastOnce()).sql(anyString());
    verify(executeSpec, atLeastOnce()).then();
  }
}
