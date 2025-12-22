# Markets API Documentation

## Overview

Complete Cassandra-based prediction markets system with WebFlux functional endpoints.

**Base URL**: `http://localhost:8080`
**API Version**: `v1`
**Response Format**: JSON with standardized `ApiResponse` wrapper

## ✅ Implementation Status

### **COMPLETED** ✓

All core functionality has been implemented and compiled successfully:

- ✅ Cassandra entities (13 models)
- ✅ Reactive repositories (9 repositories)
- ✅ Service layer (MarketService, CategoryService)
- ✅ WebFlux handlers (MarketHandler, CategoryHandler)
- ✅ Router configuration
- ✅ Request/Response DTOs
- ✅ Error handling with standardized response codes
- ✅ **BUILD SUCCESS** - All 126 source files compiled

---

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.4.11 + WebFlux (Reactive)
- **Database**: DataStax Astra DB (Cassandra)
- **Java**: 21
- **API Style**: Functional endpoints with RouterFunctions

### Database Strategy
- **PostgreSQL (R2DBC)**: Users, authentication, payments
- **Cassandra (Astra DB)**: Markets, outcomes, orders, trades, positions
- **Redis**: Caching and real-time data

---

## 📋 API Endpoints

### Market Endpoints

#### 1. Create Market
```http
POST /api/v1/markets
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "Will Bitcoin reach $100k by end of 2024?",
  "description": "Market resolves based on CoinMarketCap closing price on Dec 31, 2024",
  "categoryId": "33333333-3333-3333-3333-333333333333",
  "marketType": "BINARY",
  "marketClose": "2024-12-31T23:59:59Z",
  "resolutionTime": "2025-01-01T12:00:00Z",
  "resolutionCriteria": "CoinMarketCap BTC/USD price at midnight UTC on Jan 1, 2025",
  "outcomes": [
    {"name": "Yes", "displayOrder": 0, "color": "#22c55e"},
    {"name": "No", "displayOrder": 1, "color": "#ef4444"}
  ],
  "minOrderSize": 1.00,
  "tags": ["crypto", "bitcoin", "price prediction"]
}
```

**Response**: `201 Created`
```json
{
  "code": 2300,
  "message": "Market created successfully",
  "success": true,
  "timestamp": "2024-12-18T10:00:00Z",
  "data": {
    "marketId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "title": "Will Bitcoin reach $100k by end of 2024?",
    "slug": "will-bitcoin-reach-100k-by-end-of-2024-a1b2c3d4",
    "status": "OPEN",
    "outcomes": [...]
  }
}
```

---

#### 2. Get Market by ID
```http
GET /api/v1/markets/{marketId}
```

**Response**: `200 OK`
```json
{
  "code": 2000,
  "message": "Operation completed successfully",
  "success": true,
  "data": {
    "marketId": "uuid",
    "title": "Market title",
    "description": "Market description",
    "status": "OPEN",
    "outcomes": [...],
    "totalVolume": 125000.50,
    "totalTraders": 1523
  }
}
```

---

#### 3. Get Market by Slug
```http
GET /api/v1/markets/slug/{slug}
```

**Example**: `GET /api/v1/markets/slug/will-bitcoin-reach-100k-by-end-of-2024-a1b2c3d4`

---

#### 4. Get All Markets
```http
GET /api/v1/markets
```

Returns all markets with outcomes.

---

#### 5. Filter Markets by Category
```http
GET /api/v1/markets?category={categoryId}
```

**Example**: `GET /api/v1/markets?category=33333333-3333-3333-3333-333333333333`

---

#### 6. Filter Markets by Status
```http
GET /api/v1/markets?status={status}
```

**Valid statuses**: `OPEN`, `SUSPENDED`, `CLOSED`, `RESOLVED`, `CANCELLED`

**Example**: `GET /api/v1/markets?status=OPEN`

---

#### 7. Get Featured Markets
```http
GET /api/v1/markets/featured
```

Returns markets marked as featured.

---

#### 8. Get Trending Markets
```http
GET /api/v1/markets/trending
```

Returns trending markets based on recent activity.

---

#### 9. Search Markets
```http
GET /api/v1/markets/search?q={query}
```

**Example**: `GET /api/v1/markets/search?q=bitcoin`

---

#### 10. Update Market
```http
PUT /api/v1/markets/{marketId}
Content-Type: application/json
```

**Request Body** (all fields optional):
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "featured": true,
  "trending": true,
  "tags": ["crypto", "bitcoin", "defi"]
}
```

**Response**: `200 OK`

---

#### 11. Resolve Market
```http
POST /api/v1/markets/{marketId}/resolve
Content-Type: application/json
```

**Request Body**:
```json
{
  "winningOutcomeId": "outcome-uuid",
  "resolutionNotes": "Bitcoin price was $98,500 at midnight UTC"
}
```

**Response**: `200 OK` with `MARKET_RESOLVED` code

---

#### 12. Close Market
```http
POST /api/v1/markets/{marketId}/close
```

Closes market for new trading (before resolution).

---

#### 13. Suspend Market
```http
POST /api/v1/markets/{marketId}/suspend
```

Temporarily suspends trading.

---

#### 14. Reopen Market
```http
POST /api/v1/markets/{marketId}/reopen
```

Reopens a suspended market.

---

#### 15. Cancel Market
```http
DELETE /api/v1/markets/{marketId}
```

Cancels the market (refunds all positions).

---

#### 16. Get Market Outcomes
```http
GET /api/v1/markets/{marketId}/outcomes
```

**Response**: List of outcomes with current prices, volumes, and statistics.

```json
{
  "code": 2000,
  "success": true,
  "data": [
    {
      "outcomeId": "uuid",
      "name": "Yes",
      "currentPrice": 65.50,
      "currentPriceE4": 6550,
      "totalVolume": 50000,
      "volume24h": 5000,
      "bestBid": 65.00,
      "bestAsk": 66.00
    }
  ]
}
```

---

### Category Endpoints

#### 1. Get All Categories
```http
GET /api/v1/categories
```

Returns all enabled categories.

---

#### 2. Get Category by ID
```http
GET /api/v1/categories/{categoryId}
```

---

#### 3. Get Category by Slug
```http
GET /api/v1/categories/slug/{slug}
```

**Example**: `GET /api/v1/categories/slug/cryptocurrency`

---

#### 4. Get Subcategories
```http
GET /api/v1/categories/{categoryId}/subcategories
```

Returns hierarchical subcategories for a category.

---

## 📊 Response Structure

### Success Response
```json
{
  "code": 2000,
  "message": "Operation completed successfully",
  "success": true,
  "timestamp": "2024-12-18T10:00:00Z",
  "data": { ... }
}
```

### Error Response
```json
{
  "code": 3202,
  "message": "Market not found",
  "success": false,
  "timestamp": "2024-12-18T10:00:00Z",
  "error": {
    "code": 3202,
    "message": "Market not found with ID: uuid",
    "traceId": "a1b2c3d4"
  }
}
```

---

## 🔢 Response Codes

### Success Codes (2000-2999)
- `2000` - SUCCESS
- `2001` - CREATED
- `2300` - MARKET_CREATED
- `2301` - MARKET_UPDATED
- `2302` - MARKET_RESOLVED
- `2303` - MARKET_CLOSED

### Client Error Codes (3000-3999)
- `3000` - VALIDATION_ERROR
- `3001` - INVALID_INPUT
- `3100` - UNAUTHORIZED
- `3104` - FORBIDDEN
- `3200` - NOT_FOUND
- `3202` - MARKET_NOT_FOUND
- `3302` - MARKET_CLOSED
- `3303` - MARKET_ALREADY_RESOLVED

### Server Error Codes (4000-4999)
- `4000` - INTERNAL_SERVER_ERROR
- `4001` - SERVICE_UNAVAILABLE
- `4002` - TIMEOUT_ERROR
- `4100` - DATABASE_ERROR

---

## 🗄️ Data Models

### Market
```typescript
{
  marketId: UUID,
  title: string,
  slug: string,
  description: string,
  categoryId: UUID,
  categoryName: string,
  marketType: "BINARY" | "MULTIPLE_CHOICE" | "SCALAR" | "CATEGORICAL",
  status: "OPEN" | "SUSPENDED" | "CLOSED" | "RESOLVED" | "CANCELLED",
  marketClose: timestamp,
  resolutionTime: timestamp,
  totalVolume: decimal,
  volume24h: decimal,
  totalTraders: number,
  outcomes: Outcome[],
  featured: boolean,
  trending: boolean,
  tags: string[]
}
```

### Outcome
```typescript
{
  outcomeId: UUID,
  marketId: UUID,
  name: string,
  currentPrice: number,      // Percentage (e.g., 65.50 = 65.50%)
  currentPriceE4: number,     // Basis points (6550 = 65.50%)
  bestBid: number,
  bestAsk: number,
  totalVolume: decimal,
  volume24h: decimal,
  isWinner: boolean
}
```

---

## 🔐 Authentication

**Status**: Authentication integration pending

Current endpoints use placeholder user IDs. Production integration will use:
- JWT tokens from authentication service
- User ID extraction from security context
- Role-based access control (RBAC)

---

## 🧪 Testing the API

### Using cURL

**Create a market**:
```bash
curl -X POST http://localhost:8080/api/v1/markets \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Market",
    "description": "This is a test market",
    "categoryId": "11111111-1111-1111-1111-111111111111",
    "marketType": "BINARY",
    "marketClose": "2025-12-31T23:59:59Z",
    "resolutionTime": "2026-01-01T12:00:00Z",
    "resolutionCriteria": "Test criteria",
    "outcomes": [
      {"name": "Yes", "displayOrder": 0},
      {"name": "No", "displayOrder": 1}
    ]
  }'
```

**Get all markets**:
```bash
curl http://localhost:8080/api/v1/markets
```

**Get featured markets**:
```bash
curl http://localhost:8080/api/v1/markets/featured
```

**Search markets**:
```bash
curl "http://localhost:8080/api/v1/markets/search?q=bitcoin"
```

---

## 📁 Project Structure

```
src/main/java/com/oregonMarkets/domain/market/
├── model/              # Cassandra entities
│   ├── Market.java
│   ├── Outcome.java
│   ├── Category.java
│   ├── Order.java
│   ├── Trade.java
│   └── Position.java
├── repository/         # Reactive Cassandra repositories
│   ├── MarketRepository.java
│   ├── OutcomeRepository.java
│   └── CategoryRepository.java
├── service/           # Business logic
│   ├── MarketService.java
│   ├── CategoryService.java
│   └── impl/
│       ├── MarketServiceImpl.java
│       └── CategoryServiceImpl.java
├── handler/           # WebFlux request handlers
│   ├── MarketHandler.java
│   └── CategoryHandler.java
├── router/            # Route configuration
│   ├── MarketRouter.java
│   └── CategoryRouter.java
└── dto/              # Data transfer objects
    ├── request/
    │   ├── CreateMarketRequest.java
    │   ├── UpdateMarketRequest.java
    │   └── ResolveMarketRequest.java
    └── response/
        ├── MarketResponse.java
        └── OutcomeResponse.java
```

---

## 🚀 Next Steps

### Before Running
1. **Execute Cassandra schema** in Astra DB Console:
   - `src/main/resources/cassandra/schema/01_create_keyspace_and_tables.cql`
   - `src/main/resources/cassandra/schema/02_initialize_reference_data.cql`

2. **Set environment variables**:
   ```bash
   export ASTRA_DB_CLIENT_SECRET="your-secret"
   export DATABASE_URL="r2dbc:postgresql://localhost:5432/prediction_markets"
   export REDIS_HOST="localhost"
   ```

3. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

### Future Enhancements
- [ ] Order placement and matching engine
- [ ] Position management endpoints
- [ ] Trade history APIs
- [ ] Real-time WebSocket subscriptions
- [ ] Market analytics and statistics
- [ ] User authentication integration
- [ ] Rate limiting
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Comprehensive unit and integration tests

---

## 📚 Reference Data

### Pre-configured Category IDs
- **Sports**: `11111111-1111-1111-1111-111111111111`
- **Politics**: `22222222-2222-2222-2222-222222222222`
- **Cryptocurrency**: `33333333-3333-3333-3333-333333333333`
- **Business**: `44444444-4444-4444-4444-444444444444`
- **Entertainment**: `55555555-5555-5555-5555-555555555555`

### Market Types
- **BINARY**: Yes/No markets (2 outcomes)
- **MULTIPLE_CHOICE**: Multiple outcomes, one winner
- **SCALAR**: Numeric range prediction
- **CATEGORICAL**: Multiple categories
- **COMBINATORIAL**: Multiple related outcomes
- **POOL**: Fixed-odds betting pool

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: Cassandra connection failed
**Solution**: Verify Astra DB credentials and network connectivity

**Issue**: Market not found
**Solution**: Ensure market ID is valid UUID format

**Issue**: Validation errors on market creation
**Solution**: Check all required fields and date constraints

---

## 📞 Support

For issues or questions:
- Check application logs: `logs/spring.log`
- Review Cassandra schema: `CASSANDRA_SETUP.md`
- Consult design document: `CASSANDRA_MARKETS_DESIGN.md`

---

**Last Updated**: 2024-12-18
**API Version**: 1.0
**Build Status**: ✅ SUCCESS (126 source files)
