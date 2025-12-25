package com.oregonmarkets.domain.market.model;

/**
 * Market status enumeration
 */
public enum MarketStatus {
  /** Market is open for trading */
  OPEN,

  /** Market is temporarily suspended */
  SUSPENDED,

  /** Market is closed, no more trading allowed */
  CLOSED,

  /** Market has been resolved with a winner */
  RESOLVED,

  /** Market has been cancelled */
  CANCELLED
}
