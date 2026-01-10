-- Add missing columns that should have been in 001 but weren't applied
ALTER TABLE subcategories ADD updated_at TIMESTAMP;
ALTER TABLE subcategories_by_id ADD updated_at TIMESTAMP;
