-- Add metadata columns to view_templates

ALTER TABLE view_templates ADD description TEXT;
ALTER TABLE view_templates ADD enabled BOOLEAN;
ALTER TABLE view_templates ADD updated_at TIMESTAMP;
