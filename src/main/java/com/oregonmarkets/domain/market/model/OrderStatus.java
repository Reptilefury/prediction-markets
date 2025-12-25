package com.oregonmarkets.domain.market.model;

/**
 * Order status enumeration
 */
public enum OrderStatus {
  /** Order is active and waiting to be filled */
  OPEN,

  /** Order has been partially filled */
  PARTIALLY_FILLED,

  /** Order has been completely filled */
  FILLED,

  /** Order has been cancelled by user */
  CANCELLED,

  /** Order has expired */
  EXPIRED,

  /** Order was rejected */
  REJECTED
}
