package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CassandraMigrationConfigTest {

  private CassandraMigrationConfig config;

  @BeforeEach
  void setUp() {
    config = new CassandraMigrationConfig();
    ReflectionTestUtils.setField(config, "keyspaceName", "test_keyspace");
    ReflectionTestUtils.setField(config, "username", "token");
    ReflectionTestUtils.setField(config, "password", "secret");
    ReflectionTestUtils.setField(config, "scriptsLocation", "cassandra/migrations");
  }

  @Test
  void runMigrations_Disabled_ReturnsImmediately() {
    ReflectionTestUtils.setField(config, "migrationEnabled", false);

    assertDoesNotThrow(() -> config.runMigrations());
  }

  @Test
  void runMigrations_MissingBundle_DoesNotThrow() {
    ReflectionTestUtils.setField(config, "migrationEnabled", true);
    ReflectionTestUtils.setField(
        config, "secureConnectBundlePath", "/tmp/non-existent-secure-connect.zip");

    assertDoesNotThrow(() -> config.runMigrations());
  }
}
