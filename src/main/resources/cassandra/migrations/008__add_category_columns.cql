-- ============================================
-- Add missing columns to categories table (if they don't exist)
-- Note: Most columns were already added in migration 004
-- This migration ensures any missing columns are added safely
-- ============================================

-- Cassandra doesn't support IF NOT EXISTS for ALTER TABLE ADD COLUMN
-- So we'll just ensure the indexes exist since the columns are already there

-- Create indexes for efficient queries (these are idempotent)
CREATE INDEX IF NOT EXISTS categories_slug_idx ON categories (slug);
CREATE INDEX IF NOT EXISTS categories_enabled_idx ON categories (enabled);
