-- Add Biconomy Smart Account Integration fields to users table

ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_smart_account_address VARCHAR(42);
ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_deployed BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_chain_id INTEGER DEFAULT 137;
ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_bundler_url VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_paymaster_url VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS biconomy_created_at TIMESTAMP WITH TIME ZONE;

-- Create index for smart account address lookups
CREATE INDEX IF NOT EXISTS idx_users_biconomy_smart_account ON users(biconomy_smart_account_address);

COMMENT ON COLUMN users.biconomy_smart_account_address IS 'Biconomy smart account address (AA wallet)';
COMMENT ON COLUMN users.biconomy_deployed IS 'Whether the smart account has been deployed on-chain';
COMMENT ON COLUMN users.biconomy_chain_id IS 'Chain ID where the smart account exists (default: 137 = Polygon)';
COMMENT ON COLUMN users.biconomy_bundler_url IS 'Biconomy bundler URL for this account';
COMMENT ON COLUMN users.biconomy_paymaster_url IS 'Biconomy paymaster URL for gasless transactions';
COMMENT ON COLUMN users.biconomy_created_at IS 'Timestamp when Biconomy smart account was created';
