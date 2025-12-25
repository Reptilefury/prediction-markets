package com.oregonmarkets.domain.market.model;

/**
 * Resolution source enumeration
 */
public enum ResolutionSource {
  /** Manual resolution by admin */
  MANUAL,

  /** Automated oracle resolution */
  ORACLE,

  /** API data source resolution */
  API,

  /** Community voting resolution */
  VOTING,

  /** Blockchain-based resolution */
  BLOCKCHAIN
}
