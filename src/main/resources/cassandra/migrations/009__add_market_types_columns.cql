-- Add missing columns to market_types table

ALTER TABLE market_types ADD name TEXT;
ALTER TABLE market_types ADD icon TEXT;
ALTER TABLE market_types ADD color TEXT;
ALTER TABLE market_types ADD min_outcomes INT;
ALTER TABLE market_types ADD max_outcomes INT;
ALTER TABLE market_types ADD enabled BOOLEAN;
ALTER TABLE market_types ADD updated_at TIMESTAMP;
