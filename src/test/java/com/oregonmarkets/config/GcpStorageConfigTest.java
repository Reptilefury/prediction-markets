package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GcpStorageConfigTest {

  @Test
  void storage_WithProjectId_CreatesStorageClient() {
    GcpStorageConfig config = new GcpStorageConfig();
    ReflectionTestUtils.setField(config, "gcpProjectId", "test-project-id");

    Storage storage = config.storage();

    assertNotNull(storage, "Storage client should not be null");
  }

  @Test
  void storage_WithNullProjectId_CreatesDefaultStorageClient() {
    GcpStorageConfig config = new GcpStorageConfig();
    ReflectionTestUtils.setField(config, "gcpProjectId", null);

    Storage storage = config.storage();

    assertNotNull(storage, "Storage client should not be null even with null project ID");
  }

  @Test
  void storage_WithEmptyProjectId_CreatesDefaultStorageClient() {
    GcpStorageConfig config = new GcpStorageConfig();
    ReflectionTestUtils.setField(config, "gcpProjectId", "");

    Storage storage = config.storage();

    assertNotNull(storage, "Storage client should not be null even with empty project ID");
  }

  @Test
  void storage_WithBlankProjectId_CreatesDefaultStorageClient() {
    GcpStorageConfig config = new GcpStorageConfig();
    ReflectionTestUtils.setField(config, "gcpProjectId", "   ");

    Storage storage = config.storage();

    assertNotNull(storage, "Storage client should not be null even with blank project ID");
  }
}
