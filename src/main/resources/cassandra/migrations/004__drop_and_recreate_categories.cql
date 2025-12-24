
-- ============================================
-- Drop and recreate categories table to ensure correct schema
-- This is required because previous migrations may have created the table
-- without the slug column, causing index creation to fail.
-- ============================================

DROP TABLE IF EXISTS categories;

CREATE TABLE categories (
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

-- ============================================
-- Re-insert default categories
-- ============================================

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 'Sports', 'sports', 'Sports events, games, and competitions', 1, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (22222222-2222-2222-2222-222222222222, 'Politics', 'politics', 'Elections, policy decisions, and political events', 2, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (33333333-3333-3333-3333-333333333333, 'Cryptocurrency', 'cryptocurrency', 'Crypto prices, DeFi, and blockchain events', 3, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (44444444-4444-4444-4444-444444444444, 'Business', 'business', 'Company performance, earnings, and business metrics', 4, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (55555555-5555-5555-5555-555555555555, 'Entertainment', 'entertainment', 'Movies, TV, music, and pop culture', 5, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (66666666-6666-6666-6666-666666666666, 'Science & Technology', 'science-tech', 'Scientific discoveries and tech developments', 6, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (77777777-7777-7777-7777-777777777777, 'Climate & Environment', 'climate-environment', 'Weather, climate change, and environmental events', 7, true, toTimestamp(now()));

INSERT INTO categories (category_id, name, slug, description, display_order, enabled, created_at)
VALUES (88888888-8888-8888-8888-888888888888, 'Economics', 'economics', 'Economic indicators, GDP, inflation, and markets', 8, true, toTimestamp(now()));
