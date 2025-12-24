# Cassandra Migrations

This directory contains versioned Cassandra CQL migration scripts that are automatically executed on application startup.

## Migration Tool

We use **Cassandra Migration** by Cognitor - a Flyway-like migration tool for Cassandra.

- GitHub: https://github.com/patka/cassandra-migration
- Maven: `org.cognitor.cassandra:cassandra-migration:2.6.1_v4`

## How It Works

1. **Automatic Execution**: Migrations run automatically when the application starts (via `CassandraMigrationConfig`)
2. **Version Tracking**: Applied migrations are tracked in `cassandra_migration_version` table
3. **Idempotent**: Safe to run multiple times - only new migrations are executed
4. **Ordered Execution**: Migrations are executed in version order (001 → 002 → 003...)

## Naming Convention

Migration files must follow a numeric, zero-padded version pattern:

```
{version}__{description}.cql
```

Examples:
- `001__create_keyspace_and_tables.cql`
- `002__initialize_reference_data.cql`
- `003__add_user_preferences_table.cql`
- `004__add_market_tags_column.cql`

Notes and rules:
- Versions are numeric and zero-padded (001, 002, 003, ...). This guarantees stable sorting across tools.
- Double underscore `__` separates version from description.
- Description uses underscores for spaces.
- File extension must be `.cql`.
- Flyway-style prefixes like `V1__...` are NOT supported by `org.cognitor.cassandra.migration` and will fail to parse. Use numeric-only versions.

## Creating a New Migration

1. **Create the file** in `src/main/resources/cassandra/migrations/`:
   ```bash
   touch src/main/resources/cassandra/migrations/003__your_migration_name.cql
   ```

2. **Write your CQL** (each statement must end with `;`):
   ```sql
   -- 003__add_market_tags.cql

   USE oregon_markets;

   ALTER TABLE markets_by_id ADD tags SET<TEXT>;

   CREATE INDEX IF NOT EXISTS markets_tags_idx ON markets_by_id (tags);
   ```

3. **Restart the application** - migration runs automatically

## Existing Migrations

| Version | File | Description |
|---------|------|-------------|
| 001 | `001__create_keyspace_and_tables.cql` | Creates keyspace and all tables |
| 002 | `002__initialize_reference_data.cql` | Inserts initial categories, languages, countries |

## Checking Migration Status

Migrations are tracked in the `cassandra_migration_version` table:

```sql
SELECT * FROM oregon_markets.cassandra_migration_version;
```

Output:
```
 version | script_name                         | applied_on               | success
---------+-------------------------------------+--------------------------+---------
       1 | 001__create_keyspace_and_tables.cql | 2025-12-19 13:30:00.000 | true
       2 | 002__initialize_reference_data.cql  | 2025-12-19 13:30:01.000 | true
```

## Configuration

Located in `application.yml`:

```yaml
cassandra:
  migration:
    enabled: true                      # Enable/disable migrations
    scripts-location: cassandra/migrations  # Migration files location
```

## Best Practices

### ✅ DO

- **Use IF NOT EXISTS** for idempotent operations:
  ```sql
  CREATE TABLE IF NOT EXISTS my_table (...);
  CREATE INDEX IF NOT EXISTS my_index ON my_table (column);
  ```

- **Use IF EXISTS** for safe deletions:
  ```sql
  DROP TABLE IF EXISTS old_table;
  DROP INDEX IF EXISTS old_index;
  ```

- **Break large migrations** into multiple versions for easier troubleshooting

- **Test migrations locally** before deploying to production

- **Document complex changes** with comments in the CQL file

### ❌ DON'T

- **Never modify existing migration files** after they've been applied
  - Create a new migration instead (V3, V4, etc.)

- **Don't use DROP KEYSPACE** in migrations
  - Extremely dangerous in production

- **Don't create tables without IF NOT EXISTS**
  - Migration will fail if run twice

- **Avoid heavy data migrations** in the same file as schema changes
  - Split into separate versions

## Cassandra-Specific Considerations

### Adding Columns
```sql
-- ✅ Safe - Cassandra allows adding columns
ALTER TABLE markets_by_id ADD new_column TEXT;
```

### Dropping Columns
```sql
-- ⚠️ Careful - Can't be undone
ALTER TABLE markets_by_id DROP old_column;
```

### Modifying Columns
```sql
-- ❌ NOT SUPPORTED - Cassandra doesn't allow type changes
-- Instead: Create new column, migrate data, drop old column

ALTER TABLE markets_by_id ADD new_column_v2 BIGINT;
-- Migrate data separately
-- ALTER TABLE markets_by_id DROP old_column;
```

### Creating Indexes
```sql
-- ✅ Use IF NOT EXISTS
CREATE INDEX IF NOT EXISTS idx_name ON table_name (column);

-- ⚠️ For large tables, create indexes during low-traffic periods
```

## Troubleshooting

### Migration Failed

1. Check application logs:
   ```
   ERROR CassandraMigrationConfig - FAILED to execute Cassandra migrations
   ```

2. Check `cassandra_migration_version` table to see which migration failed:
   ```sql
   SELECT * FROM cassandra_migration_version ORDER BY version DESC LIMIT 5;
   ```

3. If a migration failed:
   - Fix the CQL in the migration file
   - Manually mark the migration as failed in the tracking table
   - Restart the application

### Migration Already Applied

If you need to re-run a migration:
```sql
-- Delete the migration version record (CAUTION!)
DELETE FROM cassandra_migration_version WHERE version = 3;
-- Restart application - migration will re-run
```

### Disable Migrations Temporarily

```yaml
# application.yml
cassandra:
  migration:
    enabled: false  # Disable auto-migration
```

## Production Deployment

1. **Test migrations locally** with production-like data
2. **Backup the database** (if possible)
3. **Review each migration** before deployment
4. **Monitor application startup** for migration errors
5. **Have a rollback plan** ready

## Migration Examples

### Example 1: Add a New Column
```sql
-- V3__add_market_visibility.cql

USE oregon_markets;

ALTER TABLE markets_by_id
ADD is_visible BOOLEAN;

-- Set default value for existing rows
UPDATE markets_by_id SET is_visible = true WHERE market_id IN (...);
```

### Example 2: Create a New Table
```sql
-- V4__create_user_preferences.cql

USE oregon_markets;

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY,
    theme TEXT,
    language_code TEXT,
    notifications_enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS user_prefs_language_idx
ON user_preferences (language_code);
```

### Example 3: Data Migration
```sql
-- V5__migrate_legacy_data.cql

USE oregon_markets;

-- Create new table
CREATE TABLE IF NOT EXISTS markets_by_status_v2 (
    status TEXT,
    created_at TIMESTAMP,
    market_id UUID,
    title TEXT,
    total_volume DECIMAL,
    PRIMARY KEY (status, created_at, market_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Data migration would typically be done via application code
-- Not in CQL migration due to performance concerns
```

## Additional Resources

- [Cassandra Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/data_modeling/)
- [CQL Reference](https://cassandra.apache.org/doc/latest/cassandra/cql/)
- [Cassandra Migration Library](https://github.com/patka/cassandra-migration)
