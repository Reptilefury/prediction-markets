-- ============================================
-- Fix subcategories table schema safely
-- ============================================

-- Since Cassandra doesn't support IF NOT EXISTS for ALTER TABLE ADD,
-- we'll recreate the table with the correct schema if needed

-- First, let's ensure the indexes exist (these are idempotent)
CREATE INDEX IF NOT EXISTS subcategories_slug_idx ON subcategories (slug);
CREATE INDEX IF NOT EXISTS subcategories_enabled_idx ON subcategories (enabled);
