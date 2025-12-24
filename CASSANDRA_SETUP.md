# Cassandra / Astra DB Setup Guide

## Prerequisites

Your Astra DB credentials have been configured:
- **Database ID**: `ce5ae66b-7bd1-4532-933a-7b1d82c5ea50`
- **Region**: `us-east-1`
- **Keyspace**: `oregon_markets`
- **Client ID**: `eLqZGoWzgkZZJWUALsfaaIQf`
- **Client Secret**: (set via environment variable)
- **Token**: (for direct API access)

## Step 1: Set Environment Variables

```bash
export ASTRA_DB_CLIENT_ID="eLqZGoWzgkZZJWUALsfaaIQf"
export ASTRA_DB_CLIENT_SECRET="68v0y0B0QCoNzL8ke-la9Z+ZP898IXOvFBtKrlug.3Z+TZSR2SR8s.WXy+xuZyr9P-YIorzvGOQOXgSqhzX,ac-+lmenx9c5nTjtncnONLZBMFkfkXDTukcs7MX_HWKk"
```

## Step 2: Initialize Schema

### Option A: Using Astra DB CQL Console (Web UI)

1. Go to [DataStax Astra Console](https://astra.datastax.com/)
2. Navigate to your database: `ce5ae66b-7bd1-4532-933a-7b1d82c5ea50`
3. Click on **"CQL Console"** tab
4. Copy and paste the contents of:
   - `src/main/resources/cassandra/schema/01_create_keyspace_and_tables.cql`
5. Execute the script
6. Verify tables created: `DESCRIBE TABLES;`

### Option B: Using cqlsh (Command Line)

```bash
# Install cqlsh (if not already installed)
pip install cqlsh

# Connect to Astra DB
cqlsh ce5ae66b-7bd1-4532-933a-7b1d82c5ea50-us-east1.apps.astra.datastax.com 29042 \
  -u eLqZGoWzgkZZJWUALsfaaIQf \
  -p 'YOUR_CLIENT_SECRET' \
  --ssl

# Once connected, run:
SOURCE 'src/main/resources/cassandra/schema/01_create_keyspace_and_tables.cql';
```

### Option C: Using DataStax Astra CLI

```bash
# Install Astra CLI
curl -Ls "https://dtsx.io/get-astra-cli" | bash

# Configure
astra setup

# Execute schema
astra db cqlsh oregon_markets \
  -f src/main/resources/cassandra/schema/01_create_keyspace_and_tables.cql
```

## Step 3: Initialize Reference Data

After schema creation, initialize reference data:

### Using CQL Console:

1. Copy contents of `src/main/resources/cassandra/schema/02_initialize_reference_data.cql`
2. Paste into CQL Console
3. Execute

### Using cqlsh:

```bash
SOURCE 'src/main/resources/cassandra/schema/02_initialize_reference_data.cql';
```

## Step 4: Verify Setup

Run these queries in CQL Console to verify:

```cql
USE oregon_markets;

-- Check languages
SELECT * FROM languages;

-- Check countries
SELECT * FROM countries LIMIT 10;

-- Check market types
SELECT * FROM market_types;

-- Check categories
SELECT * FROM categories;

-- Check subcategories (for Sports category)
SELECT * FROM subcategories
WHERE category_id = 11111111-1111-1111-1111-111111111111;

-- Check view templates
SELECT * FROM view_templates;
```

Expected results:
- **Languages**: 10 rows
- **Countries**: 16 rows
- **Market Types**: 6 rows
- **Categories**: 8 rows
- **Subcategories**: ~20+ rows
- **View Templates**: 4 rows

## Step 5: Test Connection from Application

```bash
# Make sure environment variables are set
export ASTRA_DB_CLIENT_ID="eLqZGoWzgkZZJWUALsfaaIQf"
export ASTRA_DB_CLIENT_SECRET="YOUR_SECRET"

# Also set other required env vars (PostgreSQL, Redis, etc.)
export DATABASE_URL="r2dbc:postgresql://localhost:5432/prediction_markets"
export JDBC_DATABASE_URL="jdbc:postgresql://localhost:5432/prediction_markets"
export DATABASE_USERNAME="your_pg_user"
export DATABASE_PASSWORD="your_pg_password"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

# Run the application
mvn spring-boot:run
```

Look for log messages:
```
✓ Cassandra connection established
✓ Connected to keyspace: oregon_markets
✓ Reference data validation passed
```

## Step 6: Create a Test Market (Optional)

Once the application is running, you can test market creation:

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
      {"name": "Yes"},
      {"name": "No"}
    ]
  }'
```

## Troubleshooting

### Connection Issues

**Problem**: Can't connect to Astra DB

**Solutions**:
1. Verify credentials are correct
2. Check if contact-point URL is correct (should end with `.apps.astra.datastax.com`)
3. Ensure port is 29042
4. Verify SSL is enabled in application.yml
5. Check firewall/network settings

### Schema Creation Errors

**Problem**: Tables already exist

**Solution**: Drop and recreate:
```cql
DROP KEYSPACE IF EXISTS oregon_markets;
-- Then run schema creation again
```

**Problem**: Permission denied

**Solution**: Verify your Astra DB token has admin permissions

### Application Startup Errors

**Problem**: `NoHostAvailableException`

**Solutions**:
1. Check network connectivity
2. Verify contact-point URL format
3. Ensure local-datacenter matches your Astra DB region
4. Check logs for specific error messages

**Problem**: `AuthenticationException`

**Solutions**:
1. Verify ASTRA_DB_CLIENT_ID is set correctly
2. Verify ASTRA_DB_CLIENT_SECRET is set correctly
3. Regenerate token if needed from Astra Console

## Reference Data IDs

For convenience, here are the fixed UUIDs used for reference data:

### Categories
- **Sports**: `11111111-1111-1111-1111-111111111111`
- **Politics**: `22222222-2222-2222-2222-222222222222`
- **Cryptocurrency**: `33333333-3333-3333-3333-333333333333`
- **Business**: `44444444-4444-4444-4444-444444444444`
- **Entertainment**: `55555555-5555-5555-5555-555555555555`
- **Science & Technology**: `66666666-6666-6666-6666-666666666666`
- **Climate & Environment**: `77777777-7777-7777-7777-777777777777`
- **Economics**: `88888888-8888-8888-8888-888888888888`

### View Templates
- **Default**: `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`
- **Sports**: `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb`
- **Elections**: `cccccccc-cccc-cccc-cccc-cccccccccccc`
- **Crypto**: `dddddddd-dddd-dddd-dddd-dddddddddddd`

### Key Subcategories (Examples)
- **Basketball**: `11111111-1111-0001-0000-000000000001`
- **NBA**: `11111111-1111-0001-0000-000000000002`
- **NBA Finals**: `11111111-1111-0001-0000-000000000003`
- **American Football**: `11111111-1111-0002-0000-000000000001`
- **NFL**: `11111111-1111-0002-0000-000000000002`
- **Super Bowl**: `11111111-1111-0002-0000-000000000003`

## Next Steps

After setup is complete:

1. ✅ Schema created
2. ✅ Reference data initialized
3. ✅ Application connects successfully
4. ⏭️ Create Market entity classes
5. ⏭️ Create Market repositories
6. ⏭️ Implement Market service layer
7. ⏭️ Create Market APIs
8. ⏭️ Test market creation flow

## Additional Resources

- [DataStax Astra DB Documentation](https://docs.datastax.com/en/astra/home/astra.html)
- [Spring Data Cassandra](https://docs.spring.io/spring-data/cassandra/docs/current/reference/html/)
- [Cassandra Query Language (CQL)](https://cassandra.apache.org/doc/latest/cassandra/cql/)
- [DataStax Java Driver](https://docs.datastax.com/en/developer/java-driver/latest/)

---

**Questions?** Check the logs or reach out for support!
