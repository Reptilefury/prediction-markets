-- ============================================
-- Sample hierarchical subcategories data
-- ============================================

-- Assuming we have a Sports category with UUID
-- You'll need to replace these UUIDs with actual category IDs from your database

-- Example: Sports > Basketball (Level 1)
INSERT INTO subcategories (category_id, subcategory_id, parent_subcategory_id, name, slug, description, level, path, display_order, enabled, created_at, updated_at)
VALUES (uuid(), uuid(), null, 'Basketball', 'basketball', 'Basketball markets', 1, 'Sports > Basketball', 1, true, toTimestamp(now()), toTimestamp(now()));

-- Example: Sports > Basketball > NBA (Level 2)
-- Note: parent_subcategory_id should be the UUID of 'Basketball' subcategory
INSERT INTO subcategories (category_id, subcategory_id, parent_subcategory_id, name, slug, description, level, path, display_order, enabled, created_at, updated_at)
VALUES (uuid(), uuid(), uuid(), 'NBA', 'nba', 'NBA games and championships', 2, 'Sports > Basketball > NBA', 1, true, toTimestamp(now()), toTimestamp(now()));

-- Example: Sports > Football (Level 1)
INSERT INTO subcategories (category_id, subcategory_id, parent_subcategory_id, name, slug, description, level, path, display_order, enabled, created_at, updated_at)
VALUES (uuid(), uuid(), null, 'Football', 'football', 'Football markets', 1, 'Sports > Football', 2, true, toTimestamp(now()), toTimestamp(now()));

-- Example: Politics > US Elections (Level 1)
INSERT INTO subcategories (category_id, subcategory_id, parent_subcategory_id, name, slug, description, level, path, display_order, enabled, created_at, updated_at)
VALUES (uuid(), uuid(), null, 'US Elections', 'us-elections', 'Federal and state elections', 1, 'Politics > US Elections', 1, true, toTimestamp(now()), toTimestamp(now()));

-- Example: Politics > US Elections > Presidential (Level 2)
INSERT INTO subcategories (category_id, subcategory_id, parent_subcategory_id, name, slug, description, level, path, display_order, enabled, created_at, updated_at)
VALUES (uuid(), uuid(), uuid(), 'Presidential', 'presidential', 'Presidential election markets', 2, 'Politics > US Elections > Presidential', 1, true, toTimestamp(now()), toTimestamp(now()));

-- Note: Replace uuid() with actual UUIDs when inserting real data
-- The parent_subcategory_id must reference an existing subcategory's subcategory_id
