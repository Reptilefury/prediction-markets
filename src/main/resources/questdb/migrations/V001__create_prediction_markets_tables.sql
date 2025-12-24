-- QuestDB Migration V001: Create prediction markets tables
-- High-frequency trading data optimized for time-series queries

-- 1. TRADES TABLE (High Priority - Time Series)
CREATE TABLE IF NOT EXISTS trades (
                                      trade_id UUID,
                                      market_id UUID,
                                      outcome_id UUID,
                                      executed_at TIMESTAMP,
                                      buyer_order_id UUID,
                                      buyer_user_id UUID,
                                      seller_order_id UUID,
                                      seller_user_id UUID,
                                      price_e4 LONG,
                                      quantity DOUBLE,
                                      total_value DOUBLE,
                                      buyer_fee DOUBLE,
                                      seller_fee DOUBLE,
                                      platform_fee DOUBLE,
                                      maker_side SYMBOL,
                                      taker_side SYMBOL,
                                      maker_user_id UUID,
                                      taker_user_id UUID,
                                      settlement_status SYMBOL,
                                      created_at TIMESTAMP
) TIMESTAMP(executed_at) PARTITION BY DAY;

-- 2. ORDER BOOK TABLE (High Priority - Real-time)
CREATE TABLE IF NOT EXISTS order_book (
                                          market_id UUID,
                                          outcome_id UUID,
                                          side SYMBOL,
                                          price_e4 LONG,
                                          order_id UUID,
                                          user_id UUID,
                                          quantity DOUBLE,
                                          filled_quantity DOUBLE,
                                          remaining_quantity DOUBLE,
                                          timestamp TIMESTAMP,
                                          created_at TIMESTAMP
) TIMESTAMP(timestamp) PARTITION BY HOUR;

-- 3. MARKET STATE LIVE TABLE (High Priority - Monitoring)
CREATE TABLE IF NOT EXISTS market_state_live (
                                                 market_id UUID,
                                                 timestamp TIMESTAMP,
                                                 status SYMBOL,
                                                 total_volume DOUBLE,
                                                 volume_1h DOUBLE,
                                                 volume_24h DOUBLE,
                                                 total_liquidity DOUBLE,
                                                 open_interest DOUBLE,
                                                 total_traders LONG,
                                                 active_traders_24h LONG,
                                                 total_orders LONG,
                                                 open_orders LONG,
                                                 average_spread_e4 LONG,
                                                 order_book_depth DOUBLE,
                                                 trades_1h LONG,
                                                 trades_24h LONG,
                                                 last_trade_time TIMESTAMP,
                                                 last_trade_price_e4 LONG,
                                                 created_at TIMESTAMP
) TIMESTAMP(timestamp) PARTITION BY DAY;

-- 4. ORDERS TABLE (Medium Priority - Analytics)
CREATE TABLE IF NOT EXISTS orders (
                                      order_id UUID,
                                      user_id UUID,
                                      market_id UUID,
                                      outcome_id UUID,
                                      side SYMBOL,
                                      order_type SYMBOL,
                                      price_e4 LONG,
                                      quantity DOUBLE,
                                      filled_quantity DOUBLE,
                                      remaining_quantity DOUBLE,
                                      status SYMBOL,
                                      time_in_force SYMBOL,
                                      total_cost DOUBLE,
                                      filled_cost DOUBLE,
                                      average_fill_price_e4 LONG,
                                      fees_paid DOUBLE,
                                      created_at TIMESTAMP,
                                      filled_at TIMESTAMP,
                                      cancelled_at TIMESTAMP,
                                      updated_at TIMESTAMP
) TIMESTAMP(created_at) PARTITION BY DAY;

-- 5. POSITIONS TABLE (Medium Priority - Portfolio)
CREATE TABLE IF NOT EXISTS positions (
                                         user_id UUID,
                                         market_id UUID,
                                         outcome_id UUID,
                                         quantity DOUBLE,
                                         available_quantity DOUBLE,
                                         locked_quantity DOUBLE,
                                         average_entry_price_e4 LONG,
                                         total_cost DOUBLE,
                                         realized_pnl DOUBLE,
                                         unrealized_pnl DOUBLE,
                                         current_price_e4 LONG,
                                         current_value DOUBLE,
                                         total_trades LONG,
                                         total_bought DOUBLE,
                                         total_sold DOUBLE,
                                         total_fees_paid DOUBLE,
                                         is_winner BOOLEAN,
                                         payout_amount DOUBLE,
                                         first_trade_at TIMESTAMP,
                                         last_trade_at TIMESTAMP,
                                         created_at TIMESTAMP,
                                         updated_at TIMESTAMP
) TIMESTAMP(updated_at) PARTITION BY DAY;
