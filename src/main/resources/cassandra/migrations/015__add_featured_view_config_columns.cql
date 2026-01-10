-- Add featured view configuration columns to markets_by_id

ALTER TABLE markets_by_id ADD featured_view_template_id UUID;
ALTER TABLE markets_by_id ADD featured_view_config TEXT;
ALTER TABLE markets_by_id ADD featured_rank INT;
