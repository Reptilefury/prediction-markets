package com.oregonMarkets.domain.market.model;

/**
 * Market type enumeration
 */
public enum MarketType {
  /** Binary outcome market (Yes/No) */
  BINARY,

  /** Multiple choice market with one winner */
  MULTIPLE_CHOICE,

  /** Scalar/range market (e.g., "What will be the price?") */
  SCALAR,

  /** Categorical market with multiple categories */
  CATEGORICAL,

  /** Combinatorial market (multiple related outcomes) */
  COMBINATORIAL,

  /** Prediction pool with fixed odds */
  POOL
}
