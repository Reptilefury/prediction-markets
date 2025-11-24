-- Add Polymarket Proxy Wallet Integration
ALTER TABLE users ADD COLUMN proxy_wallet_address VARCHAR(42);
ALTER TABLE users ADD COLUMN proxy_wallet_created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN proxy_wallet_status VARCHAR(20) DEFAULT 'PENDING';

-- Create index for proxy wallet lookups
CREATE INDEX idx_users_proxy_wallet_address ON users(proxy_wallet_address);
