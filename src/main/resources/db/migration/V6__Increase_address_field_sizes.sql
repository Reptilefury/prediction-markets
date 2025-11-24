-- Increase address field sizes to accommodate longer values from external services
ALTER TABLE users ALTER COLUMN magic_wallet_address TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN enclave_uda_address TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN proxy_wallet_address TYPE VARCHAR(255);
