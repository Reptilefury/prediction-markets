package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.util.ReflectionTestUtils;

class QuestDbConfigTest {

  private QuestDbConfig questDbConfig;

  @BeforeEach
  void setUp() {
    questDbConfig = new QuestDbConfig();
    ReflectionTestUtils.setField(questDbConfig, "questdbHost", "localhost");
    ReflectionTestUtils.setField(questDbConfig, "questdbPort", 8812);
    ReflectionTestUtils.setField(questDbConfig, "questdbDatabase", "qdb");
    ReflectionTestUtils.setField(questDbConfig, "questdbUsername", "admin");
    ReflectionTestUtils.setField(questDbConfig, "questdbPassword", "quest");
  }

  @Test
  void questdbConnectionFactory_ReturnsFactory() {
    ConnectionFactory factory = questDbConfig.questdbConnectionFactory();

    assertNotNull(factory);
  }

  @Test
  void questdbDatabaseClient_ReturnsClient() {
    DatabaseClient client = questDbConfig.questdbDatabaseClient();

    assertNotNull(client);
  }

  @Test
  void questdbEntityTemplate_ReturnsTemplate() {
    assertNotNull(questDbConfig.questdbEntityTemplate());
  }
}
