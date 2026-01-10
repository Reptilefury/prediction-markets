-- Insert market type reference data with icons and colors

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('BINARY', 'Binary', 'Yes/No outcome', 'mdi:toggle-switch', '3b82f6', 2, 2, true, toTimestamp(now()), toTimestamp(now()));

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('MULTIPLE_CHOICE', 'Multiple Choice', 'One winner from many options', 'mdi:format-list-checks', '10b981', 2, 10, true, toTimestamp(now()), toTimestamp(now()));

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('SCALAR', 'Scalar', 'Numeric range prediction', 'mdi:chart-bell-curve', 'f59e0b', 0, 0, true, toTimestamp(now()), toTimestamp(now()));

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('CATEGORICAL', 'Categorical', 'Multiple categories', 'mdi:shape', 'ec4899', 2, 20, true, toTimestamp(now()), toTimestamp(now()));

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('COMBINATORIAL', 'Combinatorial', 'Related outcomes', 'mdi:vector-combine', '8b5cf6', 2, 10, true, toTimestamp(now()), toTimestamp(now()));

INSERT INTO market_types (type, name, description, icon, color, min_outcomes, max_outcomes, enabled, created_at, updated_at)
VALUES ('POOL', 'Pool', 'Fixed odds pool', 'mdi:pool', '06b6d4', 2, 10, true, toTimestamp(now()), toTimestamp(now()));
