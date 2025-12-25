package com.oregonmarkets.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import reactor.core.publisher.Mono;

class DefaultR2dbcConfigTest {

  private final DefaultR2dbcConfig config = new DefaultR2dbcConfig();

  @Test
  @SuppressWarnings("unchecked")
  void r2dbcEntityTemplate_CreatesTemplateWithMetadata() {
    ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    ConnectionFactoryMetadata metadata = () -> "PostgreSQL";
    Connection connection = mock(Connection.class);

    when(connectionFactory.getMetadata()).thenReturn(metadata);
    when(connectionFactory.create()).thenReturn((Mono) Mono.just(connection));

    R2dbcEntityTemplate template = config.r2dbcEntityTemplate(connectionFactory);

    assertNotNull(template);
  }
}
