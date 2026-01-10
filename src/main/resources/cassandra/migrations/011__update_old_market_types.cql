-- Update old market types with icons and colors

UPDATE market_types SET 
    name = 'Free Response',
    icon = 'mdi:text-box-outline',
    color = 'f97316',
    min_outcomes = 0,
    max_outcomes = 0,
    enabled = true,
    updated_at = toTimestamp(now())
WHERE type = 'FREE_RESPONSE';

UPDATE market_types SET 
    name = 'Range',
    icon = 'mdi:chart-box-outline',
    color = '14b8a6',
    min_outcomes = 2,
    max_outcomes = 20,
    enabled = true,
    updated_at = toTimestamp(now())
WHERE type = 'RANGE';

UPDATE market_types SET 
    name = 'Quadratic',
    icon = 'mdi:function-variant',
    color = 'a855f7',
    min_outcomes = 2,
    max_outcomes = 10,
    enabled = true,
    updated_at = toTimestamp(now())
WHERE type = 'QUADRATIC';
