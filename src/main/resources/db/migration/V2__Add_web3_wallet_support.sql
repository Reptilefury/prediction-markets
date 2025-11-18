-- Add Web3 wallet support columns
ALTER TABLE users 
ADD COLUMN web3_wallet_address VARCHAR(42) UNIQUE,
ADD COLUMN auth_method VARCHAR(20) DEFAULT 'MAGIC',
ADD COLUMN wallet_verified_at TIMESTAMP WITH TIME ZONE;

-- Create index for Web3 wallet address
CREATE INDEX idx_users_web3_wallet_address ON users(web3_wallet_address);

-- Update existing users to have MAGIC auth method
UPDATE users SET auth_method = 'MAGIC' WHERE auth_method IS NULL;