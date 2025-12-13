package com.oregonMarkets.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GcpStorageConfig {

  @Value("${gcp.project-id}")
  private String gcpProjectId;

  @Bean
  public Storage storage() {
    log.info("[GCP-STORAGE] Initializing Storage client for project: {}", gcpProjectId);

    Storage storage =
        (gcpProjectId != null && !gcpProjectId.isBlank())
            ? StorageOptions.newBuilder().setProjectId(gcpProjectId).build().getService()
            : StorageOptions.getDefaultInstance().getService();

    log.info("[GCP-STORAGE] ✓ Storage client initialized successfully");
    return storage;
  }
}
