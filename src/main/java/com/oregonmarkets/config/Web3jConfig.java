package com.oregonmarkets.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Web3j configuration for Polygon blockchain integration Provides Web3j bean for interacting with
 * Polygon network
 */
@Configuration
@Slf4j
public class Web3jConfig {

  @Value("${blockchain.polygon.rpc-url:https://polygon-rpc.com/}")
  private String polygonRpcUrl;

  /**
   * Create Web3j bean for Polygon network Defaults to public Polygon RPC endpoint if not configured
   */
  @Bean
  public Web3j web3j() {
    log.info("Initializing Web3j with Polygon RPC: {}", polygonRpcUrl);
    return Web3j.build(new HttpService(polygonRpcUrl));
  }
}
