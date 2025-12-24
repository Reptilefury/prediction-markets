# Oregon Markets - Cassandra-First Architecture

**Database:** Cassandra ONLY (DataStax Astra DB)
**Astra DB ID:** `ce5ae66b-7bd1-4532-933a-7b1d82c5ea50`
**Region:** us-east-1
**Keyspace:** `oregon_markets`
**Design Philosophy:** Query-first, denormalized, high-performance

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Why Cassandra for Markets](#why-cassandra-for-markets)
3. [Data Modeling Strategy](#data-modeling-strategy)
4. [Complete Cassandra Schema](#complete-cassandra-schema)
5. [Reference Data Initialization](#reference-data-initialization)
6. [Market Creation Flow](#market-creation-flow)
7. [Query Patterns](#query-patterns)
8. [Consistency & CAP Trade-offs](#consistency--cap-trade-offs)
9. [Application Configuration](#application-configuration)
10. [Implementation Plan](#implementation-plan)

---

## Architecture Overview

### System Boundary

```
┌─────────────────────────────────────────────────────────────┐
│ PostgreSQL (Existing)                                       │
│ - Users, authentication                                     │
│ - Deposits, withdrawals                                     │
│ - Payment methods                                           │
│ - Wallets (Magic, Enclave, Biconomy)                       │
│ - Financial transactions                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Cassandra (DataStax Astra) - NEW                           │
│ - Reference data (categories, market types, etc.)          │
│ - Markets (all metadata)                                    │
│ - Outcomes                                                  │
│ - Orders                                                    │
│ - Trades                                                    │
│ - Positions                                                 │
│ - Order books                                               │
│ - Market state & analytics                                 │
│ - Oracle data                                               │
│ - Sports/election extensions                                │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

**Cassandra → PostgreSQL:**
- User ID references (no FK, application-level validation)
- Balance checks before order placement (read from PostgreSQL)
- Settlement triggers balance updates (write to PostgreSQL)

**PostgreSQL → Cassandra:**
- None (markets system is isolated in Cassandra)

---

## Why Cassandra for Markets

### Advantages

✅ **Horizontal Scalability**
- Add nodes without downtime
- Linear performance scaling
- Global distribution ready

✅ **Write Performance**
- 10,000+ writes/sec per node
- Perfect for high-frequency order placement
- No write locks or contention

✅ **Time-Series Optimization**
- Clustering keys for time-ordered data
- TTL for automatic data expiration
- Efficient range queries

✅ **Always Available**
- Multi-datacenter replication
- No single point of failure
- Tunable consistency

✅ **Denormalization is Natural**
- Cassandra encourages denormalization
- Query patterns drive table design
- No complex joins needed

### Trade-offs

⚠️ **No ACID Transactions**
- Solution: Design for eventual consistency
- Critical operations use BATCH statements with LOGGED mode
- Application-level validation

⚠️ **No Foreign Keys**
- Solution: Validate references in application layer
- Defensive coding for missing references

⚠️ **Data Duplication**
- Solution: Accept duplication for read performance
- Use repair jobs for consistency (if needed)

⚠️ **Schema Changes Are Costly**
- Solution: Design schema carefully upfront
- Test query patterns before production

---

## Data Modeling Strategy

### Cassandra Modeling Rules

1. **One table per query pattern**
   - Don't think relational
   - Denormalize aggressively
   - Optimize for reads

2. **Partition keys determine data locality**
   - Choose partition keys to distribute load evenly
   - Avoid hot partitions

3. **Clustering keys determine sort order**
   - Use for time-series, ranges, sorting
   - Physical storage order on disk

4. **Static columns for partition-level data**
   - Shared across all rows in partition
   - Example: market metadata in order book table

5. **Materialized views or denormalized tables**
   - For alternative query patterns
   - Accept data duplication

### Query-First Design

For each use case, ask:
1. What is the primary query?
2. What is the partition key? (to locate data)
3. What is the clustering key? (to sort within partition)
4. What columns are needed in the result?

---

## Complete Cassandra Schema

### Keyspace Definition

```cql
CREATE KEYSPACE IF NOT EXISTS oregon_markets
WITH REPLICATION = {
  'class': 'NetworkTopologyStrategy',
  'us-east-1': 3
}
AND DURABLE_WRITES = true;

USE oregon_markets;
```

---

### 1. Reference Data Tables

#### 1.1 Languages

```cql
CREATE TABLE languages (
    code TEXT PRIMARY KEY,  -- 'en', 'es', 'fr'
    name TEXT,
    created_at TIMESTAMP
);
```

**Query:** Get language by code
**Partition Key:** code

#### 1.2 Countries

```cql
CREATE TABLE countries (
    iso_code TEXT PRIMARY KEY,  -- 'US', 'GB', 'KE'
    name TEXT,
    flag_emoji TEXT,
    created_at TIMESTAMP
);
```

**Query:** Get country by ISO code
**Partition Key:** iso_code

#### 1.3 Market Types

```cql
CREATE TABLE market_types (
    type TEXT PRIMARY KEY,  -- 'BINARY', 'CATEGORICAL', 'SCALAR'
    description TEXT,
    created_at TIMESTAMP
);
```

**Query:** Get market type details
**Partition Key:** type

#### 1.4 Categories

```cql
CREATE TABLE categories (
    category_id UUID PRIMARY KEY,
    name TEXT,
    description TEXT,
    display_order INT,
    created_at TIMESTAMP
);

-- Secondary index for querying by name
CREATE INDEX ON categories (name);
```

**Query 1:** Get category by ID
**Query 2:** List all categories (scan)

#### 1.5 Subcategories

```cql
CREATE TABLE subcategories (
    category_id UUID,
    subcategory_id UUID,
    name TEXT,
    description TEXT,
    display_order INT,
    created_at TIMESTAMP,
    PRIMARY KEY (category_id, subcategory_id)
);

-- Alternative query pattern: get subcategory by ID
CREATE TABLE subcategories_by_id (
    subcategory_id UUID PRIMARY KEY,
    category_id UUID,
    name TEXT,
    description TEXT,
    display_order INT,
    created_at TIMESTAMP
);
```

**Query 1:** Get all subcategories for a category
**Query 2:** Get subcategory by ID

#### 1.6 View Templates

```cql
CREATE TABLE view_templates (
    template_id UUID PRIMARY KEY,
    name TEXT,
    json_config TEXT,  -- JSON string
    created_at TIMESTAMP
);

CREATE INDEX ON view_templates (name);
```

---

### 2. Markets Tables

#### 2.1 Markets (by ID)

**Primary market table - ALL market metadata denormalized**

```cql
CREATE TABLE markets_by_id (
    market_id UUID PRIMARY KEY,

    -- Basic info
    title TEXT,
    description TEXT,

    -- Classification
    category_id UUID,
    category_name TEXT,  -- Denormalized
    market_type TEXT,    -- 'BINARY', 'CATEGORICAL', etc.
    subtype_name TEXT,

    -- Subcategories (denormalized as comma-separated or JSON)
    subcategory_ids TEXT,    -- '["uuid1","uuid2"]' JSON array string
    subcategory_names TEXT,  -- 'Basketball,NBA,Finals'

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
    status TEXT,  -- 'OPEN', 'SUSPENDED', 'CLOSED', 'RESOLVED', 'CANCELLED'
    publicly_visible BOOLEAN,
    allow_early_trading BOOLEAN,
    auto_resolve BOOLEAN,

    -- Oracle
    oracle_type TEXT,  -- 'CHAINLINK', 'UMA', 'CUSTOM_HTTP', 'MANUAL'
    oracle_endpoint TEXT,
    oracle_config TEXT,  -- JSON string

    -- Blockchain
    blockchain_network TEXT,  -- 'POLYGON', 'ETHEREUM', etc.
    contract_address TEXT,
    proxy_wallet_address TEXT,

    -- Analytics (updated periodically)
    total_volume DECIMAL,
    total_liquidity DECIMAL,
    unique_traders INT,
    total_orders INT,

    -- View config
    view_template_id UUID,
    view_config TEXT  -- JSON string
);
```

**Query:** Get market by ID with ALL data in one read

#### 2.2 Markets by Category

```cql
CREATE TABLE markets_by_category (
    category_id UUID,
    status TEXT,  -- For filtering
    created_at TIMESTAMP,
    market_id UUID,

    -- Denormalized market data (same as markets_by_id)
    title TEXT,
    description TEXT,
    category_name TEXT,
    market_type TEXT,
    market_close TIMESTAMP,
    resolution_time TIMESTAMP,
    fee_percent DECIMAL,
    total_volume DECIMAL,
    total_liquidity DECIMAL,

    PRIMARY KEY ((category_id, status), created_at, market_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

**Query:** Get all OPEN markets in "Sports" category, recent first

#### 2.3 Markets by Status

```cql
CREATE TABLE markets_by_status (
    status TEXT,
    created_at TIMESTAMP,
    market_id UUID,

    -- Denormalized market data
    title TEXT,
    category_id UUID,
    category_name TEXT,
    market_type TEXT,
    market_close TIMESTAMP,
    total_volume DECIMAL,

    PRIMARY KEY (status, created_at, market_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

**Query:** Get all OPEN markets, recent first

#### 2.4 Markets by Close Date

```cql
CREATE TABLE markets_by_close_date (
    close_date DATE,  -- Date only, for partitioning
    market_close TIMESTAMP,
    market_id UUID,

    -- Denormalized market data
    title TEXT,
    status TEXT,
    category_name TEXT,

    PRIMARY KEY (close_date, market_close, market_id)
) WITH CLUSTERING ORDER BY (market_close ASC);
```

**Query:** Get markets closing today or this week

---

### 3. Outcomes Tables

#### 3.1 Outcomes by Market

```cql
CREATE TABLE outcomes_by_market (
    market_id UUID,
    outcome_id UUID,
    name TEXT,
    display_order INT,

    -- For scalar/range markets
    numeric_lower_bound DECIMAL,
    numeric_upper_bound DECIMAL,
    decimal_precision INT,

    -- Current state (updated frequently)
    current_price_e4 BIGINT,  -- Price * 10000
    last_trade_price_e4 BIGINT,
    volume_24h DECIMAL,
    liquidity DECIMAL,

    -- Timestamps
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    PRIMARY KEY (market_id, outcome_id)
);
```

**Query:** Get all outcomes for a market

#### 3.2 Outcome by ID

```cql
CREATE TABLE outcomes_by_id (
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
```

**Query:** Get outcome by ID (for lookups)

---

### 4. Market Extensions

#### 4.1 Sports Market Details

```cql
CREATE TABLE sports_market_details (
    market_id UUID PRIMARY KEY,
    sport_type TEXT,  -- 'basketball', 'football', 'soccer'
    league TEXT,      -- 'NBA', 'NFL', 'Premier League'
    team_a TEXT,
    team_b TEXT,
    scheduled_start_time TIMESTAMP,
    score_api_endpoint TEXT,
    created_at TIMESTAMP
);
```

**Query:** Get sports details for a market

#### 4.2 Election Market Details

```cql
CREATE TABLE election_market_details (
    market_id UUID PRIMARY KEY,
    country_code TEXT,
    election_type TEXT,  -- 'Presidential', 'Parliamentary'
    candidate_list TEXT, -- JSON array string
    polling_data_api TEXT,
    created_at TIMESTAMP
);
```

**Query:** Get election details for a market

#### 4.3 Market Localization

```cql
CREATE TABLE market_localization (
    market_id UUID,
    language_code TEXT,
    title_localized TEXT,
    description_localized TEXT,
    PRIMARY KEY (market_id, language_code)
);
```

**Query:** Get localized market data for a language

---

### 5. Order Book

```cql
CREATE TABLE order_book (
    market_id UUID,
    outcome_id UUID,
    side TEXT,  -- 'buy' or 'sell'
    price_e4 BIGINT,  -- Price * 10000 (e.g., 0.65 = 6500)
    order_id UUID,
    user_id UUID,
    size DECIMAL,
    filled_size DECIMAL,
    timestamp TIMESTAMP,

    PRIMARY KEY ((market_id, outcome_id, side), price_e4, timestamp, order_id)
) WITH CLUSTERING ORDER BY (price_e4 DESC, timestamp ASC)
  AND default_time_to_live = 604800;  -- 7 days
```

**Query:** Get all buy orders for outcome, sorted by price DESC
**Partition:** (market_id, outcome_id, side)
**Clustering:** price DESC, then time ASC

---

### 6. Orders

#### 6.1 Orders by User

```cql
CREATE TABLE orders_by_user (
    user_id UUID,
    created_at TIMESTAMP,
    order_id UUID,

    -- Order details
    market_id UUID,
    market_title TEXT,  -- Denormalized
    outcome_id UUID,
    outcome_name TEXT,  -- Denormalized
    side TEXT,
    order_type TEXT,  -- 'limit', 'market'
    price_e4 BIGINT,
    size DECIMAL,
    filled_size DECIMAL,
    status TEXT,  -- 'open', 'filled', 'cancelled', 'partially_filled'

    -- Metadata
    signature TEXT,
    updated_at TIMESTAMP,

    PRIMARY KEY (user_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

**Query:** Get all orders for user, recent first

#### 6.2 Orders by Market

```cql
CREATE TABLE orders_by_market (
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
```

**Query:** Get all orders for market, recent first

#### 6.3 Order by ID

```cql
CREATE TABLE orders_by_id (
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
```

**Query:** Get order by ID

---

### 7. Trades

#### 7.1 Trades by Market

```cql
CREATE TABLE trades_by_market (
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
  AND default_time_to_live = 2592000;  -- 30 days
```

**Query:** Get recent trades for market

#### 7.2 Trades by User

```cql
CREATE TABLE trades_by_user (
    user_id UUID,
    executed_at TIMESTAMP,
    trade_id UUID,

    -- Trade details
    market_id UUID,
    market_title TEXT,
    outcome_id UUID,
    outcome_name TEXT,
    side TEXT,  -- 'buy' or 'sell'
    price_e4 BIGINT,
    size DECIMAL,
    fee DECIMAL,
    pnl DECIMAL,  -- Realized P&L for this trade

    PRIMARY KEY (user_id, executed_at, trade_id)
) WITH CLUSTERING ORDER BY (executed_at DESC);
```

**Query:** Get trade history for user

---

### 8. Positions

#### 8.1 User Positions

```cql
CREATE TABLE user_positions (
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
```

**Query:** Get all positions for user

#### 8.2 Positions by Market

```cql
CREATE TABLE positions_by_market (
    market_id UUID,
    user_id UUID,
    outcome_id UUID,
    shares DECIMAL,
    average_price_e4 BIGINT,
    PRIMARY KEY (market_id, user_id, outcome_id)
);
```

**Query:** Get all positions in a market (for settlement)

---

### 9. Market State & Analytics

#### 9.1 Market State (Live)

```cql
CREATE TABLE market_state_live (
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
  AND default_time_to_live = 86400;  -- 24 hours
```

**Query:** Get current state and recent history for market

#### 9.2 Market Prices (Time-Series)

```cql
CREATE TABLE market_prices (
    market_id UUID,
    outcome_id UUID,
    timestamp TIMESTAMP,
    price_e4 BIGINT,
    volume_1h DECIMAL,
    PRIMARY KEY ((market_id, outcome_id), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 2592000;  -- 30 days
```

**Query:** Get price history for outcome (chart data)

#### 9.3 Live Scores (Sports)

```cql
CREATE TABLE live_scores (
    market_id UUID,
    timestamp TIMESTAMP,
    team_a_score INT,
    team_b_score INT,
    period TEXT,  -- 'Q1', 'Q2', 'Half', 'Final'
    game_time TEXT,
    status TEXT,
    PRIMARY KEY (market_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND default_time_to_live = 604800;  -- 7 days
```

**Query:** Get live score updates for sports market

---

### 10. Daily Analytics

#### 10.1 Daily Volume by Market

```cql
CREATE TABLE daily_volume_by_market (
    date DATE,
    market_id UUID,
    category_id UUID,
    volume DECIMAL,
    trades INT,
    unique_traders INT,
    PRIMARY KEY (date, market_id)
);
```

**Query:** Get volume for all markets on a specific date

#### 10.2 Daily Volume by Category

```cql
CREATE TABLE daily_volume_by_category (
    date DATE,
    category_id UUID,
    total_volume DECIMAL,
    total_trades INT,
    unique_traders INT,
    PRIMARY KEY (date, category_id)
);
```

**Query:** Get volume by category for a date

---

## Reference Data Initialization

### Startup Script (CQL)

**init-cassandra-reference-data.cql**

```cql
USE oregon_markets;

-- ============================================
-- LANGUAGES
-- ============================================
INSERT INTO languages (code, name, created_at)
VALUES ('en', 'English', toTimestamp(now()));

INSERT INTO languages (code, name, created_at)
VALUES ('es', 'Spanish', toTimestamp(now()));

INSERT INTO languages (code, name, created_at)
VALUES ('fr', 'French', toTimestamp(now()));

INSERT INTO languages (code, name, created_at)
VALUES ('de', 'German', toTimestamp(now()));

INSERT INTO languages (code, name, created_at)
VALUES ('zh', 'Chinese', toTimestamp(now()));

-- ============================================
-- COUNTRIES
-- ============================================
INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('US', 'United States', '🇺🇸', toTimestamp(now()));

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('GB', 'United Kingdom', '🇬🇧', toTimestamp(now()));

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('KE', 'Kenya', '🇰🇪', toTimestamp(now()));

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('NG', 'Nigeria', '🇳🇬', toTimestamp(now()));

-- ============================================
-- MARKET TYPES
-- ============================================
INSERT INTO market_types (type, description, created_at)
VALUES ('BINARY', 'Yes/No markets with two outcomes', toTimestamp(now()));

INSERT INTO market_types (type, description, created_at)
VALUES ('CATEGORICAL', 'Multiple discrete outcomes', toTimestamp(now()));

INSERT INTO market_types (type, description, created_at)
VALUES ('SCALAR', 'Numeric outcome within a range', toTimestamp(now()));

INSERT INTO market_types (type, description, created_at)
VALUES ('RANGE', 'Outcome falls within one of several ranges', toTimestamp(now()));

-- ============================================
-- CATEGORIES
-- ============================================
INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (uuid(), 'Sports', 'Sports events and competitions', 1, toTimestamp(now()));

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (uuid(), 'Politics', 'Elections and political events', 2, toTimestamp(now()));

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (uuid(), 'Cryptocurrency', 'Crypto prices and DeFi events', 3, toTimestamp(now()));

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (uuid(), 'Business', 'Company performance and business metrics', 4, toTimestamp(now()));

-- ============================================
-- VIEW TEMPLATES
-- ============================================
INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (uuid(), 'Default', '{"layout":"standard","showOrderBook":true,"showChart":true}', toTimestamp(now()));

INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (uuid(), 'Sports', '{"layout":"sports","showOrderBook":true,"showChart":true,"showLiveFeed":true}', toTimestamp(now()));
```

### Application-Level Initialization

```java
@Service
@RequiredArgsConstructor
public class CassandraReferenceDataService {

    private final CassandraTemplate cassandraTemplate;

    @PostConstruct
    public void initializeReferenceData() {
        log.info("Checking Cassandra reference data...");

        // Check if reference data exists
        Long languageCount = cassandraTemplate
            .select("SELECT COUNT(*) FROM languages", Long.class)
            .first()
            .block();

        if (languageCount == null || languageCount == 0) {
            log.warn("No reference data found, initializing...");
            initializeFromScript();
        } else {
            log.info("✓ Reference data present ({} languages)", languageCount);
        }
    }

    private void initializeFromScript() {
        // Load and execute init-cassandra-reference-data.cql
        // Or programmatically insert reference data
        insertLanguages();
        insertCountries();
        insertMarketTypes();
        insertCategories();
        insertViewTemplates();
    }

    private void insertLanguages() {
        // Batch insert languages
        var languages = List.of(
            new Language("en", "English"),
            new Language("es", "Spanish"),
            new Language("fr", "French")
        );

        languages.forEach(lang ->
            cassandraTemplate.insert(lang).block()
        );
    }

    // Similar methods for other reference data...
}
```

---

## Market Creation Flow

### Step-by-Step Process

```
1. Validate Request
   ↓
   - Check category_id exists in Cassandra
   - Check market_type exists
   - Check user_id exists in PostgreSQL (application-level)
   - Validate dates
   - Validate outcomes (at least 2)

2. Generate UUIDs
   ↓
   - market_id = UUID.randomUUID()
   - outcome_ids = [UUID.randomUUID() for each outcome]

3. Denormalize Data
   ↓
   - Fetch category_name from categories table
   - Fetch market_type description
   - Prepare denormalized JSON for subcategories

4. Insert into Cassandra (Multiple Tables)
   ↓
   BATCH (LOGGED for consistency):
     - INSERT INTO markets_by_id (...)
     - INSERT INTO markets_by_category (...)
     - INSERT INTO markets_by_status (...)
     - INSERT INTO markets_by_close_date (...)
     - INSERT INTO outcomes_by_market (...) x N
     - INSERT INTO outcomes_by_id (...) x N
     - INSERT INTO sports_market_details (...) [if sports]
     - INSERT INTO market_localization (...) [for each language]
   APPLY BATCH;

5. Initialize Market State
   ↓
   - INSERT INTO market_state_live (market_id, timestamp, status='OPEN', ...)
   - INSERT INTO order_book partitions (empty, ready for orders)

6. Return Response
   ↓
   - Return market_id and success status
```

### Example Java Service

```java
@Service
@RequiredArgsConstructor
public class MarketService {

    private final CassandraTemplate cassandraTemplate;
    private final UserRepository postgresUserRepo;  // For validation

    public Mono<Market> createMarket(CreateMarketRequest request) {
        return validateRequest(request)
            .flatMap(this::denormalizeData)
            .flatMap(this::insertIntoCassandra)
            .flatMap(this::initializeMarketState)
            .doOnSuccess(market -> log.info("Market created: {}", market.getId()));
    }

    private Mono<CreateMarketRequest> validateRequest(CreateMarketRequest request) {
        return Mono.just(request)
            .filterWhen(req -> categoryExists(req.getCategoryId()))
            .switchIfEmpty(Mono.error(
                new BusinessException(ResponseCode.NOT_FOUND, "Category not found")))
            .filterWhen(req -> marketTypeExists(req.getMarketType()))
            .switchIfEmpty(Mono.error(
                new BusinessException(ResponseCode.VALIDATION_ERROR, "Invalid market type")))
            .filter(req -> req.getMarketClose().isAfter(LocalDateTime.now()))
            .switchIfEmpty(Mono.error(
                new BusinessException(ResponseCode.VALIDATION_ERROR, "Market close must be in future")));
    }

    private Mono<Market> insertIntoCassandra(Market market) {
        // Use BatchStatement for atomic writes
        BatchStatement batch = BatchStatement.builder(DefaultBatchType.LOGGED)
            .addStatement(insertIntoMarketsById(market))
            .addStatement(insertIntoMarketsByCategory(market))
            .addStatement(insertIntoMarketsByStatus(market))
            .addStatement(insertIntoMarketsByCloseDate(market))
            .build();

        // Insert outcomes separately (or add to batch)
        market.getOutcomes().forEach(outcome -> {
            batch.addStatement(insertIntoOutcomesByMarket(outcome));
            batch.addStatement(insertIntoOutcomesById(outcome));
        });

        return Mono.fromFuture(
            cassandraTemplate.getCqlOperations()
                .getSession()
                .executeAsync(batch)
                .toCompletableFuture()
        ).thenReturn(market);
    }

    // Helper methods...
}
```

---

## Query Patterns

### Common Queries

#### 1. Get Market by ID

```java
public Mono<Market> getMarketById(UUID marketId) {
    return cassandraTemplate
        .selectOne(
            Query.query(Criteria.where("market_id").is(marketId)),
            Market.class
        );
}
```

**CQL:**
```cql
SELECT * FROM markets_by_id WHERE market_id = ?;
```

#### 2. List Markets by Category (OPEN only)

```java
public Flux<Market> getOpenMarketsByCategory(UUID categoryId, int limit) {
    return cassandraTemplate
        .select(
            Query.query(
                Criteria.where("category_id").is(categoryId)
                    .and("status").is("OPEN")
            ).limit(limit),
            Market.class
        );
}
```

**CQL:**
```cql
SELECT * FROM markets_by_category
WHERE category_id = ? AND status = 'OPEN'
ORDER BY created_at DESC
LIMIT ?;
```

#### 3. Get Order Book for Outcome

```java
public Flux<OrderBookEntry> getOrderBook(UUID marketId, UUID outcomeId, String side, int limit) {
    return cassandraTemplate
        .select(
            Query.query(
                Criteria.where("market_id").is(marketId)
                    .and("outcome_id").is(outcomeId)
                    .and("side").is(side)
            ).limit(limit),
            OrderBookEntry.class
        );
}
```

**CQL:**
```cql
SELECT * FROM order_book
WHERE market_id = ? AND outcome_id = ? AND side = ?
ORDER BY price_e4 DESC, timestamp ASC
LIMIT ?;
```

#### 4. Get User's Open Orders

```java
public Flux<Order> getUserOpenOrders(UUID userId) {
    return cassandraTemplate
        .select(
            Query.query(Criteria.where("user_id").is(userId)),
            Order.class
        )
        .filter(order -> "open".equals(order.getStatus()) ||
                        "partially_filled".equals(order.getStatus()));
}
```

**CQL:**
```cql
SELECT * FROM orders_by_user
WHERE user_id = ?
ORDER BY created_at DESC;
-- Filter in application layer for status
```

#### 5. Get Recent Trades for Market

```java
public Flux<Trade> getRecentTrades(UUID marketId, int limit) {
    return cassandraTemplate
        .select(
            Query.query(Criteria.where("market_id").is(marketId))
                .limit(limit),
            Trade.class
        );
}
```

**CQL:**
```cql
SELECT * FROM trades_by_market
WHERE market_id = ?
ORDER BY executed_at DESC
LIMIT ?;
```

---

## Consistency & CAP Trade-offs

### CAP Theorem Choice

Cassandra is **AP** (Available + Partition-tolerant)
- Sacrifices strong consistency for availability
- Tunable consistency levels

### Consistency Levels

**For Reads:**
- `LOCAL_QUORUM` (default) - Majority of replicas in local DC
- `ONE` (fast, eventually consistent)
- `ALL` (slow, strong consistency)

**For Writes:**
- `LOCAL_QUORUM` (default) - Majority of replicas in local DC
- `ONE` (fast, eventual consistency)
- `ALL` (slow, strong consistency)

### Recommendations

| Operation | Read Level | Write Level | Rationale |
|-----------|------------|-------------|-----------|
| Market creation | N/A | QUORUM | Critical, must be consistent |
| Market query | LOCAL_QUORUM | N/A | Balance consistency & speed |
| Order placement | ONE | QUORUM | Fast write, must be durable |
| Order book read | ONE | N/A | Speed over consistency |
| Trade execution | QUORUM | QUORUM | Critical financial operation |
| Analytics read | ONE | N/A | Eventual consistency OK |

### Handling Conflicts

**Lightweight Transactions (LWT) for Critical Operations:**

```cql
-- Conditional insert (only if doesn't exist)
INSERT INTO orders_by_id (order_id, user_id, ...)
VALUES (?, ?, ...)
IF NOT EXISTS;
```

**In Java:**
```java
boolean success = cassandraTemplate.getCqlOperations()
    .execute(
        "INSERT INTO orders_by_id (...) VALUES (...) IF NOT EXISTS",
        params
    );

if (!success) {
    throw new BusinessException(ResponseCode.DUPLICATE_ORDER, "Order already exists");
}
```

---

## Application Configuration

### application.yml

```yaml
spring:
  data:
    cassandra:
      keyspace-name: oregon_markets
      contact-points: ce5ae66b-7bd1-4532-933a-7b1d82c5ea50-us-east-1.apps.astra.datastax.com
      port: 29042
      local-datacenter: us-east-1
      username: ${ASTRA_DB_CLIENT_ID}
      password: ${ASTRA_DB_CLIENT_SECRET}
      schema-action: none  # Manage schema manually via CQL
      request:
        timeout: 10s
        consistency: LOCAL_QUORUM
      connection:
        connect-timeout: 10s
        init-query-timeout: 10s
      ssl:
        enabled: true

# DataStax Astra specific
datastax:
  astra:
    secure-connect-bundle: ${ASTRA_SECURE_CONNECT_BUNDLE_PATH:/config/secure-connect-bundle.zip}
```

### Cassandra Configuration Bean

```java
@Configuration
@EnableReactiveCassandraRepositories(basePackages = "com.oregonMarkets.domain.market.repository")
public class CassandraConfig extends AbstractReactiveCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port}")
    private int port;

    @Value("${spring.data.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.data.cassandra.username}")
    private String username;

    @Value("${spring.data.cassandra.password}")
    private String password;

    @Value("${datastax.astra.secure-connect-bundle}")
    private String secureConnectBundle;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    public CassandraClusterFactoryBean cluster() {
        CassandraClusterFactoryBean bean = super.cluster();

        // Astra DB uses secure connect bundle
        bean.setContactPoints(contactPoints);
        bean.setPort(port);
        bean.setUsername(username);
        bean.setPassword(password);
        bean.setSslEnabled(true);
        bean.setJdkSslOptions(new JdkSSLOptions());

        return bean;
    }

    @Bean
    public CassandraConverter cassandraConverter() {
        MappingCassandraConverter converter = new MappingCassandraConverter(cassandraMapping());
        converter.setUserTypeResolver(new SimpleUserTypeResolver(cluster().getObject(), getKeyspaceName()));
        return converter;
    }
}
```

### Environment Variables

```bash
# Astra DB credentials
export ASTRA_DB_CLIENT_ID="your-client-id"
export ASTRA_DB_CLIENT_SECRET="your-client-secret"
export ASTRA_SECURE_CONNECT_BUNDLE_PATH="/path/to/secure-connect-bundle.zip"
```

---

## Implementation Plan

### Phase 1: Setup (Week 1)

✅ **Day 1-2: Astra DB Setup**
- Download secure connect bundle from Astra console
- Create application token (Client ID + Secret)
- Test connection from local machine
- Create keyspace: `oregon_markets`

✅ **Day 3-4: Schema Creation**
- Run all CQL CREATE TABLE statements
- Verify tables created in Astra console
- Run reference data initialization script
- Verify data inserted

✅ **Day 5: Spring Configuration**
- Add Spring Data Cassandra dependencies
- Configure application.yml
- Create CassandraConfig bean
- Test connection from application

### Phase 2: Reference Data (Week 2)

✅ **Entities:**
- Language
- Country
- MarketType
- Category
- Subcategory
- ViewTemplate

✅ **Repositories:**
- LanguageRepository
- CountryRepository
- MarketTypeRepository
- CategoryRepository
- SubcategoryRepository
- ViewTemplateRepository

✅ **Services:**
- ReferenceDataService (CRUD)
- ReferenceDataValidator (startup check)

✅ **APIs:**
- GET /api/v1/reference/languages
- GET /api/v1/reference/countries
- GET /api/v1/reference/categories
- GET /api/v1/reference/categories/{id}/subcategories

### Phase 3: Market Management (Week 3)

✅ **Entities:**
- Market (with all fields)
- Outcome
- SportsMarketDetails
- ElectionMarketDetails
- MarketLocalization

✅ **Repositories:**
- MarketRepository (markets_by_id)
- MarketsByCategoryRepository
- MarketsByStatusRepository
- OutcomeRepository

✅ **Services:**
- MarketService (create, update, close, resolve)
- MarketQueryService (list, search, filter)
- MarketValidationService

✅ **APIs:**
- POST /api/v1/markets (create)
- GET /api/v1/markets/{id}
- GET /api/v1/markets (list with filters)
- PUT /api/v1/markets/{id}/close
- PUT /api/v1/markets/{id}/resolve

### Phase 4: Trading Engine (Week 4)

✅ **Entities:**
- Order
- Trade
- Position
- OrderBookEntry

✅ **Repositories:**
- OrderRepository (orders_by_user, orders_by_id)
- TradeRepository (trades_by_market, trades_by_user)
- PositionRepository
- OrderBookRepository

✅ **Services:**
- OrderService (place, cancel, update)
- OrderMatchingService
- PositionService (update, calculate P&L)
- OrderBookService (get depth, best bid/ask)

✅ **APIs:**
- POST /api/v1/orders (place order)
- DELETE /api/v1/orders/{id} (cancel)
- GET /api/v1/orders (user's orders)
- GET /api/v1/markets/{id}/orderbook
- GET /api/v1/positions (user's positions)

### Phase 5: Analytics & State (Week 5)

✅ **Entities:**
- MarketStateLive
- MarketPrice
- LiveScore

✅ **Services:**
- MarketStateService (update state, metrics)
- AnalyticsService (volume, liquidity)
- LiveScoreService (update sports scores)

✅ **Schedulers:**
- MarketStateUpdateScheduler (every 10 seconds)
- AnalyticsAggregationScheduler (every hour)

✅ **APIs:**
- GET /api/v1/markets/{id}/state
- GET /api/v1/markets/{id}/prices (chart data)
- GET /api/v1/markets/{id}/live-scores (sports)

### Phase 6: Testing & Optimization (Week 6)

✅ **Testing:**
- Unit tests for services
- Integration tests with Testcontainers (Cassandra)
- Load testing with JMeter
- Stress testing order placement

✅ **Optimization:**
- Add caching (Redis) for hot markets
- Optimize query patterns
- Add secondary indexes where needed
- Tune consistency levels

✅ **Monitoring:**
- Add metrics (Micrometer + Prometheus)
- Add distributed tracing (Sleuth + Zipkin)
- Add Cassandra health checks
- Add alert rules

---

## Next Steps

### Immediate Actions

1. **Get Astra DB Credentials**
   - Log into DataStax Astra Console
   - Go to database: `ce5ae66b-7bd1-4532-933a-7b1d82c5ea50`
   - Generate application token
   - Download secure connect bundle

2. **Create Keyspace**
   - Open Astra CQL Console
   - Run: `CREATE KEYSPACE oregon_markets WITH REPLICATION = {...}`

3. **Run Schema Creation**
   - Copy all CREATE TABLE statements
   - Execute in Astra CQL Console
   - Verify tables created

4. **Initialize Reference Data**
   - Run init-cassandra-reference-data.cql
   - Verify data with SELECT queries

5. **Configure Application**
   - Update application.yml with credentials
   - Test connection from Spring Boot app

### Questions for You

1. ✅ Do you have Astra DB credentials? (Client ID + Secret)
2. ✅ Is the keyspace `oregon_markets` already created?
3. ✅ Do you want to start with Phase 1 (setup) or jump to Phase 2 (reference data)?
4. ✅ Any schema changes needed? (additional tables, fields, indexes?)
5. ✅ Preferred consistency level? (LOCAL_QUORUM recommended)

---

**Ready to implement?** Let me know which phase to start with!
