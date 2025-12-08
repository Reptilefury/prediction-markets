package com.oregonMarkets.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {



  @Bean("magicWebClient")
  public WebClient magicWebClient(@Value("${app.magic.api-url}") String baseUrl) {
    return WebClient.builder().baseUrl(baseUrl).build();
  }

  @Bean("enclaveWebClient")
  public WebClient enclaveWebClient(@Value("${app.enclave.api-url}") String baseUrl) {
    return WebClient.builder().baseUrl(baseUrl).build();
  }

  @Bean("blnkWebClient")
  public WebClient blnkWebClient(@Value("${app.blnk.api-url}") String baseUrl) {
    HttpClient httpClient = createBlnkHttpClient();
    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(
            new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build())
        .build();
  }

  @Bean("keycloakAdminWebClient")
  public WebClient keycloakAdminWebClient(@Value("${keycloak.admin.base-url}") String baseUrl) {
    HttpClient httpClient = createCloudRunHttpClient();
    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(
            new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build())
        .build();
  }



  private HttpClient createBlnkHttpClient() {
    // Optimized for Cloud Run: HTTP/1.1 + no pooling to prevent premature close
    return createCloudRunHttpClient();
  }

  private HttpClient createCloudRunHttpClient() {
    // Optimized for Cloud Run services: HTTP/1.1 + no pooling to prevent premature close
    return HttpClient.create()
        .protocol(HttpProtocol.HTTP11) // Force HTTP/1.1 for Cloud Run compatibility
        .keepAlive(false) // Disable connection pooling
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.TCP_NODELAY, true)
        .responseTimeout(Duration.ofSeconds(45)) // Longer timeout for Cloud Run
        .doOnConnected(
            conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(45000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(30000, TimeUnit.MILLISECONDS)))
        .compress(true);
  }
}
