-- ============================================
-- REFERENCE DATA TABLES
-- ============================================

-- Languages
CREATE TABLE IF NOT EXISTS languages (
    code TEXT PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMP
);

-- Countries
CREATE TABLE IF NOT EXISTS countries (
    iso_code TEXT PRIMARY KEY,
    name TEXT,
    flag_emoji TEXT,
    created_at TIMESTAMP
);

-- Market Types
CREATE TABLE IF NOT EXISTS market_types (
    type TEXT PRIMARY KEY,
    description TEXT,
    created_at TIMESTAMP
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    category_id UUID PRIMARY KEY,
    name TEXT,
    slug TEXT,
    description TEXT,
    icon TEXT,
    color TEXT,
    display_order INT,
    enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS categories_name_idx ON categories (name);
CREATE INDEX IF NOT EXISTS categories_slug_idx ON categories (slug);
CREATE INDEX IF NOT EXISTS categories_enabled_idx ON categories (enabled);

-- Subcategories by Category
CREATE TABLE IF NOT EXISTS subcategories (
    category_id UUID,
    subcategory_id UUID,
    name TEXT,
    slug TEXT,
    description TEXT,
    display_order INT,
    enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (category_id, subcategory_id)
);

CREATE INDEX IF NOT EXISTS subcategories_slug_idx ON subcategories (slug);
CREATE INDEX IF NOT EXISTS subcategories_enabled_idx ON subcategories (enabled);

-- Subcategories by ID (for lookups)
CREATE TABLE IF NOT EXISTS subcategories_by_id (
    subcategory_id UUID PRIMARY KEY,
    category_id UUID,
    name TEXT,
    slug TEXT,
    description TEXT,
    display_order INT,
    enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- View Templates
CREATE TABLE IF NOT EXISTS view_templates (
    template_id UUID PRIMARY KEY,
    name TEXT,
    json_config TEXT,
    description TEXT,
    enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS view_templates_name_idx ON view_templates (name);

-- ============================================
-- MARKETS TABLES
-- ============================================

-- Markets by ID (Primary market table with ALL metadata)
CREATE TABLE IF NOT EXISTS markets_by_id (
    market_id UUID PRIMARY KEY,

    -- Basic info
    title TEXT,
    description TEXT,

    -- Classification
    category_id UUID,
    category_name TEXT,
    market_type TEXT,
    subtype_name TEXT,

    -- Subcategories (JSON array string)
    subcategory_ids TEXT,
    subcategory_names TEXT,

    -- Dates
    market_close TIMESTAMP,
    resolution_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    -- Resolution
    resolution_criteria TEXT,
    official_source TEXT,
    winning_outcome_id UUID,
    resolved_at TIMESTAMP,

    -- Configuration
    fee_percent DECIMAL,
    min_bet DECIMAL,
    max_bet DECIMAL,
    initial_liquidity DECIMAL,

    -- Status
    status TEXT,
    publicly_visible BOOLEAN,
    allow_early_trading BOOLEAN,
    auto_resolve BOOLEAN,

    -- Oracle
    oracle_type TEXT,
    oracle_endpoint TEXT,
    oracle_config TEXT,

    -- Blockchain
    blockchain_network TEXT,
    contract_address TEXT,
    proxy_wallet_address TEXT,

    -- Analytics
    total_volume DECIMAL,
    total_liquidity DECIMAL,
    unique_traders INT,
    total_orders INT,

    -- View config
    view_template_id UUID,
    view_config TEXT,
    featured_view_template_id UUID,
    featured_view_config TEXT,
    featured_rank INT
);

-- Markets by Category and Status
CREATE TABLE IF NOT EXISTS markets_by_category (
    category_id UUID,
    status TEXT,
    created_at TIMESTAMP,
    market_id UUID,

    -- Denormalized data
    title TEXT,
    description TEXT,
    category_name TEXT,
    market_type TEXT,
    market_close TIMESTAMP,
    resolution_time TIMESTAMP,
    fee_percent DECIMAL,
    total_volume DECIMAL,
    total_liquidity DECIMAL,
    publicly_visible BOOLEAN,

    PRIMARY KEY ((category_id, status), created_at, market_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Markets by Status
CREATE TABLE IF NOT EXISTS markets_by_status (
    status TEXT,
    created_at TIMESTAMP,
    market_id UUID,

    -- Denormalized data
    title TEXT,
    category_id UUID,
    category_name TEXT,
    market_type TEXT,
    market_close TIMESTAMP,
    total_volume DECIMAL,
    publicly_visible BOOLEAN,

    PRIMARY KEY (status, created_at, market_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Markets by Close Date
CREATE TABLE IF NOT EXISTS markets_by_close_date (
    close_date DATE,
    market_close TIMESTAMP,
    market_id UUID,

    -- Denormalized data
    title TEXT,
    status TEXT,
    category_name TEXT,
    category_id UUID,

    PRIMARY KEY (close_date, market_close, market_id)
) WITH CLUSTERING ORDER BY (market_close ASC);

-- ============================================
-- OUTCOMES TABLES
-- ============================================

-- Outcomes by Market
CREATE TABLE IF NOT EXISTS outcomes_by_market (
    market_id UUID,
    outcome_id UUID,
    name TEXT,
    display_order INT,

    -- For scalar/range markets
    numeric_lower_bound DECIMAL,
    numeric_upper_bound DECIMAL,
    decimal_precision INT,

    -- Current state
    current_price_e4 BIGINT,
    last_trade_price_e4 BIGINT,
    volume_24h DECIMAL,
    liquidity DECIMAL,

    -- Timestamps
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    PRIMARY KEY (market_id, outcome_id)
);

-- Outcome by ID (for lookups)
CREATE TABLE IF NOT EXISTS outcomes_by_id (
    outcome_id UUID PRIMARY KEY,
    market_id UUID,
    name TEXT,
    display_order INT,
    numeric_lower_bound DECIMAL,
    numeric_upper_bound DECIMAL,
    decimal_precision INT,
    current_price_e4 BIGINT,
    created_at TIMESTAMP
);

-- ============================================
-- MARKET EXTENSIONS
-- ============================================

-- Sports Market Details
CREATE TABLE IF NOT EXISTS sports_market_details (
    market_id UUID PRIMARY KEY,
    sport_type TEXT,
    league TEXT, team_a TEXT, team_b TEXT,
    scheduled_start_time TIMESTAMP,
    score_api_endpoint TEXT,
    created_at TIMESTAMP
);

-- Election Market Details
CREATE TABLE IF NOT EXISTS election_market_details (
    market_id UUID PRIMARY KEY,
    country_code TEXT,
    election_type TEXT,
    candidate_list TEXT,
    polling_data_api TEXT,
    created_at TIMESTAMP
);

-- Market Localization
CREATE TABLE IF NOT EXISTS market_localization (
    market_id UUID,
    language_code TEXT,
    title_localized TEXT,
    description_localized TEXT,
    PRIMARY KEY (market_id, language_code)
);

-- ============================================
-- ORDER BOOK
-- ============================================

CREATE TABLE IF NOT EXISTS order_book (
    market_id UUID,
    outcome_id UUID,
    side TEXT,
    price_e4 BIGINT,
    order_id UUID,
    user_id UUID,
    size DECIMAL,
    filled_size DECIMAL,
    timestamp TIMESTAMP,

    PRIMARY KEY ((market_id, outcome_id, side), price_e4, timestamp, order_id)
) WITH CLUSTERING ORDER BY (price_e4 DESC, timestamp ASC)
  AND default_time_to_live = 604800;

-- ============================================
-- ORDERS
-- ============================================

-- Orders by User
CREATE TABLE IF NOT EXISTS orders_by_user (
    user_id UUID,
    created_at TIMESTAMP,
    order_id UUID,

    -- Order details
    market_id UUID,
    market_title TEXT,
    outcome_id UUID,
    outcome_name TEXT,
    side TEXT,
    order_type TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    filled_size DECIMAL,
    status TEXT,

    -- Metadata
    signature TEXT,
    updated_at TIMESTAMP,

    PRIMARY KEY (user_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Orders by Market
CREATE TABLE IF NOT EXISTS orders_by_market (
    market_id UUID,
    created_at TIMESTAMP,
    order_id UUID,

    -- Order details
    user_id UUID,
    outcome_id UUID,
    outcome_name TEXT,
    side TEXT,
    order_type TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    filled_size DECIMAL,
    status TEXT,

    PRIMARY KEY (market_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Order by ID
CREATE TABLE IF NOT EXISTS orders_by_id (
    order_id UUID PRIMARY KEY,
    user_id UUID,
    market_id UUID,
    outcome_id UUID,
    side TEXT,
    order_type TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    filled_size DECIMAL,
    status TEXT,
    signature TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============================================
-- TRADES
-- ============================================

-- Trades by Market
CREATE TABLE IF NOT EXISTS trades_by_market (
    market_id UUID,
    executed_at TIMESTAMP,
    trade_id UUID,

    -- Trade details
    buy_order_id UUID,
    sell_order_id UUID,
    buy_user_id UUID,
    sell_user_id UUID,
    outcome_id UUID,
    outcome_name TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    fee DECIMAL,
    tx_hash TEXT,

    PRIMARY KEY (market_id, executed_at, trade_id)
) WITH CLUSTERING ORDER BY (executed_at DESC)
  AND default_time_to_live = 2592000;

-- Trades by User
CREATE TABLE IF NOT EXISTS trades_by_user (
    user_id UUID,
    executed_at TIMESTAMP,
    trade_id UUID,

    -- Trade details
    market_id UUID,
    market_title TEXT,
    outcome_id UUID,
    outcome_name TEXT,
    side TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    fee DECIMAL,
    pnl DECIMAL,

    PRIMARY KEY (user_id, executed_at, trade_id)
) WITH CLUSTERING ORDER BY (executed_at DESC);

-- ============================================
-- POSITIONS
-- ============================================

-- User Positions
CREATE TABLE IF NOT EXISTS user_positions (
    user_id UUID,
    market_id UUID,
    outcome_id UUID,

    -- Position details
    market_title TEXT,
    outcome_name TEXT,
    shares DECIMAL,
    average_price_e4 BIGINT,
    realized_pnl DECIMAL,
    unrealized_pnl DECIMAL,

    -- Timestamps
    first_opened TIMESTAMP,
    last_updated TIMESTAMP,

    PRIMARY KEY (user_id, market_id, outcome_id)
);

-- Positions by Market
CREATE TABLE IF NOT EXISTS positions_by_market (
    market_id UUID,
    user_id UUID,
    outcome_id UUID,
    shares DECIMAL,
    average_price_e4 BIGINT,
    PRIMARY KEY (market_id, user_id, outcome_id)
);

-- ============================================
-- MARKET STATE & ANALYTICS
-- ============================================

-- Market State Live
CREATE TABLE IF NOT EXISTS market_state_live (
    market_id UUID,
    timestamp TIMESTAMP,

    -- State
    status TEXT,
    total_volume DECIMAL,
    total_liquidity DECIMAL,
    unique_traders INT,
    total_orders INT,
    last_trade_price_e4 BIGINT,

    PRIMARY KEY (market_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 86400;

-- Market Prices (Time-Series)
CREATE TABLE IF NOT EXISTS market_prices (
    market_id UUID,
    outcome_id UUID,
    timestamp TIMESTAMP,
    price_e4 BIGINT,
    volume_1h DECIMAL,
    PRIMARY KEY ((market_id, outcome_id), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 2592000;

-- Live Scores (Sports)
CREATE TABLE IF NOT EXISTS live_scores (
    market_id UUID,
    timestamp TIMESTAMP,
    team_a_score INT,
    team_b_score INT,
    period TEXT,
    game_time TEXT,
    status TEXT,
    PRIMARY KEY (market_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 604800;

-- ============================================
-- DAILY ANALYTICS
-- ============================================

-- Daily Volume by Market
CREATE TABLE IF NOT EXISTS daily_volume_by_market (
    date DATE,
    market_id UUID,
    category_id UUID,
    volume DECIMAL,
    trades INT,
    unique_traders INT,
    PRIMARY KEY (date, market_id)
);

-- Daily Volume by Category
CREATE TABLE IF NOT EXISTS daily_volume_by_category (
    date DATE,
    category_id UUID,
    total_volume DECIMAL,
    total_trades INT,
    unique_traders INT,
    PRIMARY KEY (date, category_id)
);
