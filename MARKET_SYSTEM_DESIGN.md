# Market System - Architecture Design Document

**Status:** Design Phase
**Date:** December 18, 2025
**Database Strategy:** Cassandra ONLY (DataStax Astra) for Markets System
**Note:** PostgreSQL remains for users, payments, and financial transactions

---

## Table of Contents

1. [Design Philosophy](#design-philosophy)
2. [Data Hierarchy & Dependencies](#data-hierarchy--dependencies)
3. [PostgreSQL vs Cassandra Split](#postgresql-vs-cassandra-split)
4. [Reference Data Initialization](#reference-data-initialization)
5. [Market Creation Flow](#market-creation-flow)
6. [Data Synchronization Strategy](#data-synchronization-strategy)
7. [Cassandra Schema Design](#cassandra-schema-design)
8. [Event-Driven Architecture](#event-driven-architecture)
9. [API Design](#api-design)
10. [Implementation Phases](#implementation-phases)

---

## Design Philosophy

### Core Principles

1. **CQRS (Command Query Responsibility Segregation)**
   - **Commands** → PostgreSQL (transactional, ACID)
   - **Queries** → Cassandra (optimized for reads, denormalized)

2. **Event Sourcing**
   - All state changes produce domain events
   - Events propagate from PostgreSQL → Cassandra
   - Cassandra as materialized view of PostgreSQL state

3. **Reference Data First**
   - System cannot create markets without pre-configured reference data
   - Reference data is immutable or admin-only
   - Loaded at system startup or via admin API

4. **Hybrid Database Strategy**
   - **PostgreSQL**: Source of truth for business entities
   - **Cassandra**: High-performance read layer for real-time operations

---

## Data Hierarchy & Dependencies

### Dependency Tree

```
┌─────────────────────────────────────────────────────────┐
│ LEVEL 0: REFERENCE DATA (Must exist first)             │
├─────────────────────────────────────────────────────────┤
│ ✓ languages                                             │
│ ✓ countries                                             │
│ ✓ market_types (BINARY, CATEGORICAL, etc.)             │
│ ✓ view_templates                                        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ LEVEL 1: TAXONOMY (Depends on Level 0)                 │
├─────────────────────────────────────────────────────────┤
│ ✓ categories (Sports, Politics, Crypto, etc.)          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ LEVEL 2: SUB-TAXONOMY (Depends on Level 1)             │
├─────────────────────────────────────────────────────────┤
│ ✓ subcategories (Basketball, NBA, etc.)                │
│ ✓ market_subtypes (Over/Under, Winner, Spread)         │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ LEVEL 3: MARKETS (Depends on Level 0-2)                │
├─────────────────────────────────────────────────────────┤
│ ✓ markets                                               │
│ ✓ market_subcategories (junction table)                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ LEVEL 4: MARKET EXTENSIONS (Depends on Level 3)        │
├─────────────────────────────────────────────────────────┤
│ ✓ outcomes                                              │
│ ✓ market_configurations                                 │
│ ✓ oracle_config                                         │
│ ✓ blockchain_config                                     │
│ ✓ localization                                          │
│ ✓ sports_market_details (optional)                      │
│ ✓ election_market_details (optional)                    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ LEVEL 5: RUNTIME DATA (Created during operations)      │
├─────────────────────────────────────────────────────────┤
│ ✓ orders (Cassandra primary)                           │
│ ✓ trades (Cassandra primary)                           │
│ ✓ positions (Cassandra primary)                         │
│ ✓ order_book_snapshots (Cassandra only)                │
│ ✓ market_state (Cassandra only)                        │
└─────────────────────────────────────────────────────────┘
```

### Initialization Order

**Before any market can be created, the system must have:**

1. ✅ At least 1 language (e.g., English)
2. ✅ At least 1 country (e.g., United States)
3. ✅ All market_types loaded (BINARY, CATEGORICAL, etc.)
4. ✅ At least 1 category (e.g., Sports)
5. ✅ At least 1 view_template (default UI config)

**Optional but recommended:**
- Subcategories for common topics
- Market subtypes for common patterns
- Multiple languages for i18n

---

## PostgreSQL vs Cassandra Split

### PostgreSQL (Source of Truth)

**Use Cases:**
- Transactional consistency required
- Complex joins needed
- Data integrity constraints
- Admin/configuration data
- Financial transactions

**Tables:**

```
REFERENCE DATA (Read-heavy, rarely updated):
  ✓ languages
  ✓ countries
  ✓ categories
  ✓ subcategories
  ✓ market_types
  ✓ market_subtypes
  ✓ view_templates

MARKET METADATA (Write-once, read occasionally):
  ✓ markets
  ✓ market_subcategories
  ✓ outcomes
  ✓ market_configurations
  ✓ oracle_config
  ✓ blockchain_config
  ✓ localization
  ✓ sports_market_details
  ✓ election_market_details

USER & PAYMENT DATA:
  ✓ users
  ✓ deposits
  ✓ withdrawals
  ✓ payment_methods
  ✓ enclave_chain_addresses
  ✓ blockchain_chains
```

### Cassandra (Astra DB) - High-Performance Layer

**Use Cases:**
- High write throughput (orders, trades)
- Time-series data
- Real-time queries
- Denormalized for fast reads
- No complex joins needed

**Tables (Keyspace: `oregon_markets`):**

```
REAL-TIME TRADING DATA:
  ✓ order_book (partition by market_id, cluster by price/time)
  ✓ orders (partition by user_id, cluster by created_at DESC)
  ✓ trades (partition by market_id, cluster by executed_at DESC)
  ✓ user_positions (partition by user_id, cluster by market_id)

MARKET STATE:
  ✓ market_state_live (partition by market_id, TTL snapshots)
  ✓ market_prices (partition by market_id, cluster by timestamp)
  ✓ liquidity_snapshots (partition by market_id, cluster by timestamp)

SPORTS/LIVE EVENTS:
  ✓ live_scores (partition by market_id, cluster by timestamp)
  ✓ event_feed (partition by market_id, cluster by timestamp)

ANALYTICS:
  ✓ trade_history_by_user (partition by user_id, cluster by timestamp)
  ✓ trade_history_by_market (partition by market_id, cluster by timestamp)
  ✓ daily_volume (partition by date, cluster by market_id)
```

---

## Reference Data Initialization

### Seed Data SQL (PostgreSQL)

**V11__Initialize_market_reference_data.sql**

```sql
-- ============================================
-- LANGUAGES
-- ============================================
INSERT INTO languages (id, code, name) VALUES
  (gen_random_uuid(), 'en', 'English'),
  (gen_random_uuid(), 'es', 'Spanish'),
  (gen_random_uuid(), 'fr', 'French'),
  (gen_random_uuid(), 'de', 'German'),
  (gen_random_uuid(), 'zh', 'Chinese'),
  (gen_random_uuid(), 'ja', 'Japanese'),
  (gen_random_uuid(), 'ar', 'Arabic'),
  (gen_random_uuid(), 'hi', 'Hindi'),
  (gen_random_uuid(), 'pt', 'Portuguese'),
  (gen_random_uuid(), 'ru', 'Russian')
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- COUNTRIES
-- ============================================
INSERT INTO countries (id, iso_code, name, flag_emoji) VALUES
  (gen_random_uuid(), 'US', 'United States', '🇺🇸'),
  (gen_random_uuid(), 'GB', 'United Kingdom', '🇬🇧'),
  (gen_random_uuid(), 'CA', 'Canada', '🇨🇦'),
  (gen_random_uuid(), 'AU', 'Australia', '🇦🇺'),
  (gen_random_uuid(), 'DE', 'Germany', '🇩🇪'),
  (gen_random_uuid(), 'FR', 'France', '🇫🇷'),
  (gen_random_uuid(), 'ES', 'Spain', '🇪🇸'),
  (gen_random_uuid(), 'IT', 'Italy', '🇮🇹'),
  (gen_random_uuid(), 'JP', 'Japan', '🇯🇵'),
  (gen_random_uuid(), 'CN', 'China', '🇨🇳'),
  (gen_random_uuid(), 'IN', 'India', '🇮🇳'),
  (gen_random_uuid(), 'BR', 'Brazil', '🇧🇷'),
  (gen_random_uuid(), 'MX', 'Mexico', '🇲🇽'),
  (gen_random_uuid(), 'ZA', 'South Africa', '🇿🇦'),
  (gen_random_uuid(), 'KE', 'Kenya', '🇰🇪'),
  (gen_random_uuid(), 'NG', 'Nigeria', '🇳🇬')
ON CONFLICT (iso_code) DO NOTHING;

-- ============================================
-- MARKET TYPES
-- ============================================
INSERT INTO market_types (id, type) VALUES
  ('type-binary', 'BINARY'),
  ('type-categorical', 'CATEGORICAL'),
  ('type-scalar', 'SCALAR'),
  ('type-range', 'RANGE'),
  ('type-quadratic', 'QUADRATIC'),
  ('type-free-response', 'FREE_RESPONSE')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- VIEW TEMPLATES
-- ============================================
INSERT INTO view_templates (id, name, json_config) VALUES
  ('template-default', 'Default Market View', '{
    "layout": "standard",
    "showOrderBook": true,
    "showChart": true,
    "showLiveFeed": false,
    "chartType": "line",
    "defaultTimeRange": "24h"
  }'::jsonb),
  ('template-sports', 'Sports Match View', '{
    "layout": "sports",
    "showOrderBook": true,
    "showChart": true,
    "showLiveFeed": true,
    "showScoreboard": true,
    "chartType": "line",
    "defaultTimeRange": "live"
  }'::jsonb),
  ('template-elections', 'Election Results View', '{
    "layout": "elections",
    "showOrderBook": true,
    "showChart": true,
    "showLiveFeed": true,
    "showMap": true,
    "chartType": "bar",
    "defaultTimeRange": "7d"
  }'::jsonb),
  ('template-crypto', 'Crypto Price View', '{
    "layout": "financial",
    "showOrderBook": true,
    "showChart": true,
    "showLiveFeed": true,
    "chartType": "candlestick",
    "defaultTimeRange": "1h"
  }'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- CATEGORIES
-- ============================================
INSERT INTO categories (id, name, description) VALUES
  ('cat-sports', 'Sports', 'Sports events, games, and competitions'),
  ('cat-politics', 'Politics', 'Elections, policy decisions, and political events'),
  ('cat-crypto', 'Cryptocurrency', 'Crypto prices, DeFi, and blockchain events'),
  ('cat-business', 'Business', 'Company performance, earnings, and business metrics'),
  ('cat-entertainment', 'Entertainment', 'Movies, TV, music, and pop culture'),
  ('cat-science', 'Science & Technology', 'Scientific discoveries and tech developments'),
  ('cat-climate', 'Climate & Environment', 'Weather, climate change, and environmental events'),
  ('cat-economics', 'Economics', 'Economic indicators, GDP, inflation, and markets')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SUBCATEGORIES - SPORTS
-- ============================================
INSERT INTO subcategories (id, category_id, name, description) VALUES
  -- Basketball
  ('sub-basketball', 'cat-sports', 'Basketball', 'All basketball markets'),
  ('sub-nba', 'cat-sports', 'NBA', 'National Basketball Association'),
  ('sub-nba-finals', 'cat-sports', 'NBA Finals', 'NBA Championship Finals'),
  ('sub-ncaa-basketball', 'cat-sports', 'NCAA Basketball', 'College basketball'),

  -- Football (American)
  ('sub-american-football', 'cat-sports', 'American Football', 'All American football'),
  ('sub-nfl', 'cat-sports', 'NFL', 'National Football League'),
  ('sub-super-bowl', 'cat-sports', 'Super Bowl', 'NFL Championship'),
  ('sub-ncaa-football', 'cat-sports', 'NCAA Football', 'College football'),

  -- Soccer
  ('sub-soccer', 'cat-sports', 'Soccer/Football', 'Association football'),
  ('sub-world-cup', 'cat-sports', 'World Cup', 'FIFA World Cup'),
  ('sub-premier-league', 'cat-sports', 'Premier League', 'English Premier League'),
  ('sub-champions-league', 'cat-sports', 'Champions League', 'UEFA Champions League'),

  -- Baseball
  ('sub-baseball', 'cat-sports', 'Baseball', 'All baseball markets'),
  ('sub-mlb', 'cat-sports', 'MLB', 'Major League Baseball'),
  ('sub-world-series', 'cat-sports', 'World Series', 'MLB Championship'),

  -- Other Sports
  ('sub-boxing', 'cat-sports', 'Boxing', 'Professional boxing matches'),
  ('sub-mma', 'cat-sports', 'MMA/UFC', 'Mixed martial arts'),
  ('sub-tennis', 'cat-sports', 'Tennis', 'Professional tennis'),
  ('sub-olympics', 'cat-sports', 'Olympics', 'Olympic Games')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SUBCATEGORIES - POLITICS
-- ============================================
INSERT INTO subcategories (id, category_id, name, description) VALUES
  ('sub-us-politics', 'cat-politics', 'US Politics', 'United States political events'),
  ('sub-us-presidential', 'cat-politics', 'US Presidential', 'US Presidential elections'),
  ('sub-us-congress', 'cat-politics', 'US Congress', 'Congressional elections'),
  ('sub-uk-politics', 'cat-politics', 'UK Politics', 'United Kingdom political events'),
  ('sub-european-politics', 'cat-politics', 'European Politics', 'European Union and member states'),
  ('sub-global-politics', 'cat-politics', 'Global Politics', 'International political events')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SUBCATEGORIES - CRYPTO
-- ============================================
INSERT INTO subcategories (id, category_id, name, description) VALUES
  ('sub-bitcoin', 'cat-crypto', 'Bitcoin', 'Bitcoin price and events'),
  ('sub-ethereum', 'cat-crypto', 'Ethereum', 'Ethereum price and events'),
  ('sub-defi', 'cat-crypto', 'DeFi', 'Decentralized finance protocols'),
  ('sub-nft', 'cat-crypto', 'NFTs', 'Non-fungible tokens'),
  ('sub-altcoins', 'cat-crypto', 'Altcoins', 'Alternative cryptocurrencies')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- MARKET SUBTYPES - SPORTS
-- ============================================
INSERT INTO market_subtypes (id, type_id, name, description) VALUES
  -- Binary Sports Markets
  ('subtype-match-winner', 'type-binary', 'Match Winner', 'Will Team A beat Team B?'),
  ('subtype-will-qualify', 'type-binary', 'Will Qualify', 'Will team qualify for next round?'),
  ('subtype-over-under', 'type-binary', 'Over/Under', 'Will score be over/under threshold?'),

  -- Categorical Sports Markets
  ('subtype-tournament-winner', 'type-categorical', 'Tournament Winner', 'Who will win the tournament?'),
  ('subtype-podium-finish', 'type-categorical', 'Podium Finish', 'Which teams will finish in top 3?'),

  -- Scalar Sports Markets
  ('subtype-final-score', 'type-scalar', 'Final Score', 'What will be the final score?'),
  ('subtype-total-points', 'type-scalar', 'Total Points', 'Total points scored in game')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- MARKET SUBTYPES - POLITICS
-- ============================================
INSERT INTO market_subtypes (id, type_id, name, description) VALUES
  ('subtype-election-winner', 'type-categorical', 'Election Winner', 'Who will win the election?'),
  ('subtype-vote-share', 'type-scalar', 'Vote Share %', 'What percentage of votes will candidate receive?'),
  ('subtype-will-win-state', 'type-binary', 'Will Win State', 'Will candidate win specific state?')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- MARKET SUBTYPES - CRYPTO
-- ============================================
INSERT INTO market_subtypes (id, type_id, name, description) VALUES
  ('subtype-price-above', 'type-binary', 'Price Above', 'Will price be above threshold by date?'),
  ('subtype-price-range', 'type-range', 'Price Range', 'What range will price be in?'),
  ('subtype-exact-price', 'type-scalar', 'Exact Price', 'What will the exact price be?')
ON CONFLICT (id) DO NOTHING;
```

### Reference Data Validation

**Application startup should validate:**

```java
@Component
@RequiredArgsConstructor
public class ReferenceDataValidator implements ApplicationRunner {

    private final LanguageRepository languageRepository;
    private final CountryRepository countryRepository;
    private final MarketTypeRepository marketTypeRepository;
    private final CategoryRepository categoryRepository;
    private final ViewTemplateRepository viewTemplateRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating reference data...");

        // Check required reference data exists
        validateLanguages();
        validateCountries();
        validateMarketTypes();
        validateCategories();
        validateViewTemplates();

        log.info("✅ Reference data validation complete");
    }

    private void validateLanguages() {
        long count = languageRepository.count().block();
        if (count == 0) {
            throw new IllegalStateException(
                "No languages found in database. Run V11__Initialize_market_reference_data.sql");
        }
        log.info("✓ Languages: {} loaded", count);
    }

    private void validateMarketTypes() {
        long count = marketTypeRepository.count().block();
        if (count < 6) {
            throw new IllegalStateException(
                "Missing market types. Expected 6, found: " + count);
        }
        log.info("✓ Market Types: {} loaded", count);
    }

    // Similar checks for other reference data...
}
```

---

## Market Creation Flow

### Prerequisites Check

```
┌──────────────────────────────────────────────┐
│ 1. Validate Request                          │
│    - Check category_id exists               │
│    - Check type_id exists                   │
│    - Check subcategory_ids exist            │
│    - Check language_ids exist               │
│    - Validate dates (close < resolution)    │
│    - Validate outcomes (at least 2)         │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 2. Create Market (PostgreSQL Transaction)   │
│    - INSERT into markets                    │
│    - INSERT into outcomes                   │
│    - INSERT into market_subcategories       │
│    - INSERT into market_configurations      │
│    - INSERT into oracle_config              │
│    - INSERT into blockchain_config          │
│    - INSERT into localization               │
│    - INSERT into sports/election details    │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 3. Publish MarketCreatedEvent               │
│    - Event contains full market payload     │
│    - Published to GCP Pub/Sub               │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 4. Cassandra Materialization                │
│    - Create market_state_live record        │
│    - Create initial order_book partitions   │
│    - Create liquidity_snapshots record      │
│    - Create market_prices record            │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 5. Blockchain Deployment (Async)            │
│    - Deploy market smart contract           │
│    - Mint outcome tokens                    │
│    - Update blockchain_config with address  │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 6. Return MarketCreatedResponse             │
│    - Market ID                              │
│    - Status: OPEN                           │
│    - All metadata                           │
└──────────────────────────────────────────────┘
```

---

## Data Synchronization Strategy

### Event Flow

```
PostgreSQL (Source of Truth)
        ↓
   Domain Event Published
        ↓
   GCP Pub/Sub Topic
        ↓
   Event Consumer (Async)
        ↓
Cassandra (Materialized View)
```

### Key Events

```java
// Market lifecycle events
- MarketCreatedEvent → Cassandra: create market_state
- MarketClosedEvent → Cassandra: update market_state
- MarketResolvedEvent → Cassandra: update market_state, calculate payouts

// Trading events (originate in Cassandra)
- OrderPlacedEvent → PostgreSQL: record for compliance
- OrderMatchedEvent → PostgreSQL: update balances
- TradeExecutedEvent → PostgreSQL: record settlement

// Oracle events
- OracleUpdateEvent → Cassandra: update live scores/data
- ResolutionProposedEvent → PostgreSQL: store resolution
```

### Consistency Strategy

**Eventual Consistency Model:**
- PostgreSQL changes are authoritative
- Cassandra updates are asynchronous (within 100ms target)
- Reconciliation jobs run hourly to detect drift
- Critical operations (withdrawals, settlements) read from PostgreSQL

**Strong Consistency When Needed:**
- User balance checks before order placement
- Market state before accepting orders
- Resolution finalization

---

## Cassandra Schema Design

### Keyspace

```cql
CREATE KEYSPACE IF NOT EXISTS oregon_markets
WITH REPLICATION = {
  'class': 'NetworkTopologyStrategy',
  'us-east-1': 3
}
AND DURABLE_WRITES = true;
```

### Core Tables

#### 1. Order Book (High-Write, Time-Series)

```cql
CREATE TABLE oregon_markets.order_book (
    market_id UUID,
    outcome_id UUID,
    side TEXT,              -- 'buy' or 'sell'
    price_e4 BIGINT,        -- Price * 10000 (e.g., 0.65 = 6500)
    size DECIMAL,
    order_id UUID,
    user_id UUID,
    timestamp TIMESTAMP,
    PRIMARY KEY ((market_id, outcome_id, side), price_e4, timestamp, order_id)
) WITH CLUSTERING ORDER BY (price_e4 DESC, timestamp ASC)
  AND default_time_to_live = 604800;  -- 7 days TTL
```

**Query patterns:**
- Get all buy orders for outcome at price level
- Get best bid/ask for outcome
- Get order book depth for market

#### 2. User Orders

```cql
CREATE TABLE oregon_markets.orders (
    user_id UUID,
    order_id UUID,
    market_id UUID,
    outcome_id UUID,
    side TEXT,
    order_type TEXT,        -- 'limit' or 'market'
    price_e4 BIGINT,
    size DECIMAL,
    filled_size DECIMAL,
    status TEXT,            -- 'open', 'filled', 'cancelled', 'partially_filled'
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    signature TEXT,
    PRIMARY KEY (user_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

**Query patterns:**
- Get all orders for user (recent first)
- Get open orders for user
- Get order history

#### 3. Trades

```cql
CREATE TABLE oregon_markets.trades (
    market_id UUID,
    trade_id UUID,
    buy_order_id UUID,
    sell_order_id UUID,
    buy_user_id UUID,
    sell_user_id UUID,
    outcome_id UUID,
    price_e4 BIGINT,
    size DECIMAL,
    fee DECIMAL,
    executed_at TIMESTAMP,
    tx_hash TEXT,
    PRIMARY KEY (market_id, executed_at, trade_id)
) WITH CLUSTERING ORDER BY (executed_at DESC);
```

**Query patterns:**
- Get recent trades for market
- Get trade history for time range

#### 4. User Positions

```cql
CREATE TABLE oregon_markets.user_positions (
    user_id UUID,
    market_id UUID,
    outcome_id UUID,
    shares DECIMAL,
    average_price_e4 BIGINT,
    realized_pnl DECIMAL,
    unrealized_pnl DECIMAL,
    last_updated TIMESTAMP,
    PRIMARY KEY (user_id, market_id, outcome_id)
);
```

**Query patterns:**
- Get all positions for user
- Get position in specific market

#### 5. Market State (Live)

```cql
CREATE TABLE oregon_markets.market_state_live (
    market_id UUID,
    timestamp TIMESTAMP,
    state TEXT,             -- 'open', 'suspended', 'closed', 'resolved'
    total_volume DECIMAL,
    total_liquidity DECIMAL,
    unique_traders INT,
    last_trade_price_e4 BIGINT,
    PRIMARY KEY (market_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 86400;  -- 24 hours TTL
```

**Query patterns:**
- Get current state of market
- Get state history for market

#### 6. Market Prices (Time-Series)

```cql
CREATE TABLE oregon_markets.market_prices (
    market_id UUID,
    outcome_id UUID,
    timestamp TIMESTAMP,
    price_e4 BIGINT,
    volume_24h DECIMAL,
    PRIMARY KEY ((market_id, outcome_id), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 2592000;  -- 30 days TTL
```

**Query patterns:**
- Get price chart for outcome
- Get current price for outcome

#### 7. Live Scores (Sports Markets)

```cql
CREATE TABLE oregon_markets.live_scores (
    market_id UUID,
    timestamp TIMESTAMP,
    team_a_score INT,
    team_b_score INT,
    period TEXT,            -- 'Q1', 'Q2', 'Half', etc.
    game_time TEXT,
    status TEXT,
    PRIMARY KEY (market_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 604800;  -- 7 days TTL
```

#### 8. Trade History by User

```cql
CREATE TABLE oregon_markets.trade_history_by_user (
    user_id UUID,
    executed_at TIMESTAMP,
    trade_id UUID,
    market_id UUID,
    outcome_id UUID,
    side TEXT,
    price_e4 BIGINT,
    size DECIMAL,
    fee DECIMAL,
    pnl DECIMAL,
    PRIMARY KEY (user_id, executed_at, trade_id)
) WITH CLUSTERING ORDER BY (executed_at DESC);
```

---

## Event-Driven Architecture

### Event Schema

```java
@Data
@Builder
public class MarketCreatedEvent {
    private UUID marketId;
    private String title;
    private UUID categoryId;
    private UUID typeId;
    private UUID subtypeId;
    private List<UUID> subcategoryIds;
    private LocalDateTime marketClose;
    private LocalDateTime resolutionTime;
    private List<OutcomeDTO> outcomes;
    private MarketStatus status;
    private BigDecimal initialLiquidity;
    private OracleConfig oracleConfig;
    private BlockchainConfig blockchainConfig;
    private LocalDateTime eventTimestamp;
}

@Data
@Builder
public class MarketClosedEvent {
    private UUID marketId;
    private MarketStatus previousStatus;
    private MarketStatus newStatus;
    private LocalDateTime closedAt;
    private LocalDateTime eventTimestamp;
}

@Data
@Builder
public class MarketResolvedEvent {
    private UUID marketId;
    private UUID winningOutcomeId;
    private String resolutionSource;
    private String resolutionProof;
    private LocalDateTime resolvedAt;
    private LocalDateTime eventTimestamp;
}

@Data
@Builder
public class OrderPlacedEvent {
    private UUID orderId;
    private UUID userId;
    private UUID marketId;
    private UUID outcomeId;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal size;
    private String signature;
    private LocalDateTime placedAt;
}
```

### Event Handlers

```java
@Service
@RequiredArgsConstructor
public class CassandraMarketEventHandler {

    private final CassandraTemplate cassandraTemplate;
    private final MarketStateLiveRepository cassandraMarketStateRepo;

    @EventListener
    public void handleMarketCreated(MarketCreatedEvent event) {
        // Materialize market state in Cassandra
        MarketStateLive state = MarketStateLive.builder()
            .marketId(event.getMarketId())
            .timestamp(event.getEventTimestamp())
            .state(MarketStatus.OPEN.name())
            .totalVolume(BigDecimal.ZERO)
            .totalLiquidity(event.getInitialLiquidity())
            .uniqueTraders(0)
            .build();

        cassandraMarketStateRepo.save(state).block();

        // Create initial order book partitions for each outcome
        event.getOutcomes().forEach(outcome -> {
            createOrderBookPartition(event.getMarketId(), outcome.getId());
        });

        log.info("Market {} materialized in Cassandra", event.getMarketId());
    }

    @EventListener
    public void handleMarketClosed(MarketClosedEvent event) {
        // Update market state
        MarketStateLive state = cassandraMarketStateRepo
            .findByMarketId(event.getMarketId())
            .blockFirst();

        if (state != null) {
            state.setState(event.getNewStatus().name());
            state.setTimestamp(event.getClosedAt());
            cassandraMarketStateRepo.save(state).block();
        }

        log.info("Market {} closed in Cassandra", event.getMarketId());
    }

    // Similar handlers for other events...
}
```

---

## API Design

### Market Management APIs

#### Create Market

```
POST /api/v1/markets
Authorization: Bearer {admin_token}
Content-Type: application/json

Request Body: (see full JSON example from earlier)

Response:
{
  "code": 2300,
  "message": "Market created successfully",
  "success": true,
  "data": {
    "marketId": "mkt-...",
    "status": "OPEN",
    "blockchainTxHash": null,  // Async deployment
    "cassandraStatus": "MATERIALIZED"
  }
}
```

#### Get Market Details

```
GET /api/v1/markets/{marketId}
Authorization: Bearer {user_token}

Response: (full market JSON with all extensions)
```

#### List Markets

```
GET /api/v1/markets?category={categoryId}&status=OPEN&limit=50

Response:
{
  "code": 2000,
  "success": true,
  "data": {
    "markets": [...],
    "pagination": {
      "total": 150,
      "page": 1,
      "pageSize": 50
    }
  }
}
```

#### Close Market

```
PUT /api/v1/markets/{marketId}/close
Authorization: Bearer {admin_token}

Response:
{
  "code": 2303,
  "message": "Market closed for trading",
  "success": true
}
```

#### Resolve Market

```
PUT /api/v1/markets/{marketId}/resolve
Authorization: Bearer {admin_token}
Content-Type: application/json

Request:
{
  "winningOutcomeId": "outcome-...",
  "resolutionSource": "https://nba.com/finals/2025/results",
  "resolutionProof": "Final score: Lakers 105, Celtics 98"
}

Response:
{
  "code": 2302,
  "message": "Market resolved successfully",
  "success": true,
  "data": {
    "payoutsCalculated": 1250,
    "totalPayout": "125000.50"
  }
}
```

### Trading APIs (Cassandra-backed)

#### Get Order Book

```
GET /api/v1/markets/{marketId}/orderbook?outcome={outcomeId}

Response: (from Cassandra order_book table)
{
  "code": 2000,
  "success": true,
  "data": {
    "marketId": "mkt-...",
    "outcomeId": "outcome-...",
    "bids": [
      {"price": 0.65, "size": 1000, "orders": 5},
      {"price": 0.64, "size": 800, "orders": 3}
    ],
    "asks": [
      {"price": 0.66, "size": 500, "orders": 2},
      {"price": 0.67, "size": 1200, "orders": 7}
    ]
  }
}
```

#### Place Order

```
POST /api/v1/orders
Authorization: Bearer {user_token}
Content-Type: application/json

Request:
{
  "marketId": "mkt-...",
  "outcomeId": "outcome-...",
  "side": "buy",
  "type": "limit",
  "price": 0.65,
  "size": 100,
  "signature": "0x..."
}

Response:
{
  "code": 2500,
  "message": "Order placed successfully",
  "success": true,
  "data": {
    "orderId": "order-...",
    "status": "OPEN",
    "filledSize": 0
  }
}
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1)
✅ Create Flyway migration V11 (reference data seed)
✅ Create PostgreSQL entities (Market, Outcome, Category, etc.)
✅ Create repositories (R2DBC)
✅ Set up Cassandra connection to Astra DB
✅ Create Cassandra entities (OrderBook, Trades, Positions)
✅ Implement reference data validator

### Phase 2: Market Management (Week 2)
✅ Implement MarketService (CRUD operations)
✅ Create market DTOs (Request/Response)
✅ Create market API handlers & router
✅ Implement market validation logic
✅ Create market events (Created, Closed, Resolved)
✅ Test market creation flow

### Phase 3: Cassandra Integration (Week 3)
✅ Implement event listeners for Cassandra materialization
✅ Create Cassandra repositories (Spring Data Cassandra)
✅ Implement order book services
✅ Implement market state services
✅ Test PostgreSQL → Cassandra sync
✅ Add reconciliation jobs

### Phase 4: Trading Engine (Week 4)
✅ Implement order placement (Cassandra-first)
✅ Implement order matching engine
✅ Implement position tracking
✅ Add real-time updates (WebSocket)
✅ Test order flow end-to-end

### Phase 5: Oracle & Resolution (Week 5)
✅ Implement oracle integrations (Chainlink, UMA, HTTP)
✅ Implement market resolution logic
✅ Implement payout calculation
✅ Test resolution flow

### Phase 6: Optimization & Scale (Week 6+)
✅ Add caching (Redis)
✅ Optimize Cassandra queries
✅ Add monitoring & metrics
✅ Load testing
✅ Production deployment

---

## Astra DB Configuration

### Connection Configuration

**application.yml**

```yaml
spring:
  data:
    cassandra:
      keyspace-name: oregon_markets
      contact-points:
        - ce5ae66b-7bd1-4532-933a-7b1d82c5ea50-us-east-1.apps.astra.datastax.com:29042
      local-datacenter: us-east-1
      username: ${ASTRA_DB_CLIENT_ID}
      password: ${ASTRA_DB_CLIENT_SECRET}
      schema-action: none  # Manage schema manually
      request:
        timeout: 5s
        consistency: LOCAL_QUORUM
      connection:
        connect-timeout: 5s
        init-query-timeout: 5s
      ssl: true

# Astra-specific settings
astra:
  db:
    id: ce5ae66b-7bd1-4532-933a-7b1d82c5ea50
    region: us-east-1
    keyspace: oregon_markets
    secure-connect-bundle: ${ASTRA_SECURE_CONNECT_BUNDLE_PATH}
```

### Secure Connect Bundle

Download from Astra Console:
1. Go to your database: ce5ae66b-7bd1-4532-933a-7b1d82c5ea50
2. Click "Connect" → "Java"
3. Download secure connect bundle
4. Store in: `/config/secure-connect-bundle.zip`
5. Set env var: `ASTRA_SECURE_CONNECT_BUNDLE_PATH=/config/secure-connect-bundle.zip`

---

## Next Steps

1. **Review this design document** - Does it align with your vision?
2. **Confirm Cassandra schema** - Any changes to table structures?
3. **Choose starting point** - Which phase should we implement first?
4. **Database credentials** - Set up Astra DB client ID/secret
5. **Begin implementation** - Start with Phase 1 (Foundation)

---

## Questions to Resolve

1. ✅ Cassandra consistency level preference? (Recommend: LOCAL_QUORUM)
2. ✅ TTL for order book data? (Recommend: 7 days)
3. ✅ Should we support multiple blockchains per market?
4. ✅ Do we need versioning for market configurations?
5. ✅ Real-time WebSocket updates from Cassandra directly or via cache?

---

**Ready to proceed?** Let me know if you want to:
- Adjust any design decisions
- Start implementation (which phase?)
- Generate additional documentation
- Discuss specific implementation details
