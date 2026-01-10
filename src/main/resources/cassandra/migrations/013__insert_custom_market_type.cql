-- Ensure custom market type metadata is populated

UPDATE market_types SET
    name = 'Custom',
    description = 'Custom market type',
    icon = 'mdi:tune-variant',
    color = '6366f1',
    min_outcomes = 2,
    max_outcomes = 20,
    enabled = true,
    updated_at = toTimestamp(now())
WHERE type = 'CUSTOM';
