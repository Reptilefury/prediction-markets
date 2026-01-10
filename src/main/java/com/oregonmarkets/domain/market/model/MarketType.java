package com.oregonmarkets.domain.market.model;

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

  /** Range market with bucketed outcomes */
  RANGE,

  /** Categorical market with multiple categories */
  CATEGORICAL,

  /** Custom market type (UI-defined) */
  CUSTOM,

  /** Combinatorial market (multiple related outcomes) */
  COMBINATORIAL,

  /** Prediction pool with fixed odds */
  POOL
}
