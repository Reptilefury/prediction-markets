-- Add external market integration columns

ALTER TABLE markets_by_id ADD source TEXT;
ALTER TABLE markets_by_id ADD source_market_id TEXT;
ALTER TABLE markets_by_id ADD markup DECIMAL;
ALTER TABLE markets_by_id ADD sync_status TEXT;
ALTER TABLE markets_by_id ADD last_synced_at TIMESTAMP;
ALTER TABLE markets_by_id ADD market_subtype TEXT;
