
-- ============================================
-- Ensure other tables are correctly defined
-- ============================================

-- Add missing enabled column to subcategories
ALTER TABLE subcategories ADD enabled BOOLEAN;

-- Create indexes (these are idempotent)
CREATE INDEX IF NOT EXISTS subcategories_slug_idx ON subcategories (slug);
CREATE INDEX IF NOT EXISTS subcategories_enabled_idx ON subcategories (enabled);
