-- ============================================
-- Oregon Markets - Reference Data Initialization
-- ============================================

USE oregon_markets;

-- ============================================
-- LANGUAGES
-- ============================================
INSERT INTO languages (code, name, created_at)
VALUES ('en', 'English', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('es', 'Spanish', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('fr', 'French', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('de', 'German', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('zh', 'Chinese', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('ja', 'Japanese', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('ar', 'Arabic', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('hi', 'Hindi', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('pt', 'Portuguese', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO languages (code, name, created_at)
VALUES ('ru', 'Russian', toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- COUNTRIES
-- ============================================
INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('US', 'United States', '🇺🇸', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('GB', 'United Kingdom', '🇬🇧', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('CA', 'Canada', '🇨🇦', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('AU', 'Australia', '🇦🇺', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('DE', 'Germany', '🇩🇪', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('FR', 'France', '🇫🇷', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('ES', 'Spain', '🇪🇸', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('IT', 'Italy', '🇮🇹', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('JP', 'Japan', '🇯🇵', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('CN', 'China', '🇨🇳', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('IN', 'India', '🇮🇳', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('BR', 'Brazil', '🇧🇷', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('MX', 'Mexico', '🇲🇽', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('ZA', 'South Africa', '🇿🇦', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('KE', 'Kenya', '🇰🇪', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO countries (iso_code, name, flag_emoji, created_at)
VALUES ('NG', 'Nigeria', '🇳🇬', toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- MARKET TYPES
-- ============================================
INSERT INTO market_types (type, description, created_at)
VALUES ('BINARY', 'Yes/No markets with two outcomes', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('CATEGORICAL', 'Multiple discrete outcomes', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('SCALAR', 'Numeric outcome within a range', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('RANGE', 'Outcome falls within one of several ranges', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('CUSTOM', 'Custom market type', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('QUADRATIC', 'Quadratic voting mechanism', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO market_types (type, description, created_at)
VALUES ('FREE_RESPONSE', 'Open-ended responses', toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- CATEGORIES (Using fixed UUIDs for consistency)
-- ============================================
INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 'Sports', 'Sports events, games, and competitions', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (22222222-2222-2222-2222-222222222222, 'Politics', 'Elections, policy decisions, and political events', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (33333333-3333-3333-3333-333333333333, 'Cryptocurrency', 'Crypto prices, DeFi, and blockchain events', 3, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (44444444-4444-4444-4444-444444444444, 'Business', 'Company performance, earnings, and business metrics', 4, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (55555555-5555-5555-5555-555555555555, 'Entertainment', 'Movies, TV, music, and pop culture', 5, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (66666666-6666-6666-6666-666666666666, 'Science & Technology', 'Scientific discoveries and tech developments', 6, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (77777777-7777-7777-7777-777777777777, 'Climate & Environment', 'Weather, climate change, and environmental events', 7, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO categories (category_id, name, description, display_order, created_at)
VALUES (88888888-8888-8888-8888-888888888888, 'Economics', 'Economic indicators, GDP, inflation, and markets', 8, toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- SUBCATEGORIES - SPORTS
-- ============================================
-- Basketball
INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0001-0000-000000000001, 'Basketball', 'All basketball markets', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0001-0000-000000000001, 11111111-1111-1111-1111-111111111111, 'Basketball', 'All basketball markets', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0001-0000-000000000002, 'NBA', 'National Basketball Association', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0001-0000-000000000002, 11111111-1111-1111-1111-111111111111, 'NBA', 'National Basketball Association', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0001-0000-000000000003, 'NBA Finals', 'NBA Championship Finals', 3, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0001-0000-000000000003, 11111111-1111-1111-1111-111111111111, 'NBA Finals', 'NBA Championship Finals', 3, toTimestamp(now())) IF NOT EXISTS;

-- American Football
INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0002-0000-000000000001, 'American Football', 'All American football', 10, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0002-0000-000000000001, 11111111-1111-1111-1111-111111111111, 'American Football', 'All American football', 10, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0002-0000-000000000002, 'NFL', 'National Football League', 11, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0002-0000-000000000002, 11111111-1111-1111-1111-111111111111, 'NFL', 'National Football League', 11, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0002-0000-000000000003, 'Super Bowl', 'NFL Championship', 12, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0002-0000-000000000003, 11111111-1111-1111-1111-111111111111, 'Super Bowl', 'NFL Championship', 12, toTimestamp(now())) IF NOT EXISTS;

-- Soccer/Football
INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0003-0000-000000000001, 'Soccer', 'Association football', 20, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0003-0000-000000000001, 11111111-1111-1111-1111-111111111111, 'Soccer', 'Association football', 20, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0003-0000-000000000002, 'World Cup', 'FIFA World Cup', 21, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0003-0000-000000000002, 11111111-1111-1111-1111-111111111111, 'World Cup', 'FIFA World Cup', 21, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (11111111-1111-1111-1111-111111111111, 11111111-1111-0003-0000-000000000003, 'Premier League', 'English Premier League', 22, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (11111111-1111-0003-0000-000000000003, 11111111-1111-1111-1111-111111111111, 'Premier League', 'English Premier League', 22, toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- SUBCATEGORIES - POLITICS
-- ============================================
INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (22222222-2222-2222-2222-222222222222, 22222222-2222-0001-0000-000000000001, 'US Politics', 'United States political events', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (22222222-2222-0001-0000-000000000001, 22222222-2222-2222-2222-222222222222, 'US Politics', 'United States political events', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (22222222-2222-2222-2222-222222222222, 22222222-2222-0001-0000-000000000002, 'US Presidential', 'US Presidential elections', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (22222222-2222-0001-0000-000000000002, 22222222-2222-2222-2222-222222222222, 'US Presidential', 'US Presidential elections', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (22222222-2222-2222-2222-222222222222, 22222222-2222-0002-0000-000000000001, 'UK Politics', 'United Kingdom political events', 10, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (22222222-2222-0002-0000-000000000001, 22222222-2222-2222-2222-222222222222, 'UK Politics', 'United Kingdom political events', 10, toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- SUBCATEGORIES - CRYPTOCURRENCY
-- ============================================
INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (33333333-3333-3333-3333-333333333333, 33333333-3333-0001-0000-000000000001, 'Bitcoin', 'Bitcoin price and events', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (33333333-3333-0001-0000-000000000001, 33333333-3333-3333-3333-333333333333, 'Bitcoin', 'Bitcoin price and events', 1, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (33333333-3333-3333-3333-333333333333, 33333333-3333-0002-0000-000000000001, 'Ethereum', 'Ethereum price and events', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (33333333-3333-0002-0000-000000000001, 33333333-3333-3333-3333-333333333333, 'Ethereum', 'Ethereum price and events', 2, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories (category_id, subcategory_id, name, description, display_order, created_at)
VALUES (33333333-3333-3333-3333-333333333333, 33333333-3333-0003-0000-000000000001, 'DeFi', 'Decentralized finance protocols', 3, toTimestamp(now())) IF NOT EXISTS;

INSERT INTO subcategories_by_id (subcategory_id, category_id, name, description, display_order, created_at)
VALUES (33333333-3333-0003-0000-000000000001, 33333333-3333-3333-3333-333333333333, 'DeFi', 'Decentralized finance protocols', 3, toTimestamp(now())) IF NOT EXISTS;

-- ============================================
-- VIEW TEMPLATES
-- ============================================
INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa, 'Default', '{"layout":"standard","showOrderBook":true,"showChart":true,"showLiveFeed":false}', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb, 'Sports', '{"layout":"sports","showOrderBook":true,"showChart":true,"showLiveFeed":true,"showScoreboard":true}', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (cccccccc-cccc-cccc-cccc-cccccccccccc, 'Elections', '{"layout":"elections","showOrderBook":true,"showChart":true,"showLiveFeed":true,"showMap":true}', toTimestamp(now())) IF NOT EXISTS;

INSERT INTO view_templates (template_id, name, json_config, created_at)
VALUES (dddddddd-dddd-dddd-dddd-dddddddddddd, 'Crypto', '{"layout":"financial","showOrderBook":true,"showChart":true,"showLiveFeed":true,"chartType":"candlestick"}', toTimestamp(now())) IF NOT EXISTS;
