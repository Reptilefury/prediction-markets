package com.oregonMarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

class CassandraConfigTest {

  private com.oregonmarkets.config.CassandraConfig cassandraConfig;
  private Path tempBundle;

  @BeforeEach
  void setUp() throws IOException {
    cassandraConfig = new com.oregonmarkets.config.CassandraConfig();
    ReflectionTestUtils.setField(cassandraConfig, "keyspaceName", "test_keyspace");
    ReflectionTestUtils.setField(cassandraConfig, "username", "token");
    ReflectionTestUtils.setField(cassandraConfig, "password", "secret");
    tempBundle = Files.createTempFile("secure-connect", ".zip");
    ReflectionTestUtils.setField(cassandraConfig, "secureConnectBundlePath", tempBundle.toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempBundle);
  }

  @Test
  void cassandraSession_CreatesFactoryWhenBundleExists() {
    CqlSessionFactoryBean factoryBean = cassandraConfig.cassandraSession();

    assertNotNull(factoryBean);
    String keyspaceName = (String) ReflectionTestUtils.getField(factoryBean, "keyspaceName");
    assertEquals("test_keyspace", keyspaceName);
  }

  @Test
  void cassandraSession_ThrowsWhenBundleMissing() {
    ReflectionTestUtils.setField(
        cassandraConfig, "secureConnectBundlePath", "/tmp/does-not-exist.zip");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, cassandraConfig::cassandraSession);

    assertTrue(exception.getMessage().contains("Secure connect bundle not found"));
  }
}
