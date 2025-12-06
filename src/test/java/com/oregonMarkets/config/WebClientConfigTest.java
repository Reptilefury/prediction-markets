package com.oregonMarkets.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientConfigTest {

  @Test
  void magicWebClient_ShouldReturnValidClient() {
    WebClientConfig config = new WebClientConfig();
    WebClient client = config.magicWebClient("http://localhost:8080");
    assertNotNull(client);
  }

  @Test
  void enclaveWebClient_ShouldReturnValidClient() {
    WebClientConfig config = new WebClientConfig();
    WebClient client = config.enclaveWebClient("http://localhost:8081");
    assertNotNull(client);
  }

  @Test
  void blnkWebClient_ShouldReturnValidClient() {
    WebClientConfig config = new WebClientConfig();
    WebClient client = config.blnkWebClient("http://localhost:8082");
    assertNotNull(client);
  }

  @Test
  void keycloakAdminWebClient_ShouldReturnValidClient() {
    WebClientConfig config = new WebClientConfig();
    WebClient client = config.keycloakAdminWebClient("http://localhost:8083");
    assertNotNull(client);
  }
}
