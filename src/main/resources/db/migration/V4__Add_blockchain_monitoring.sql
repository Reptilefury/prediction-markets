-- Add blockchain monitoring fields to existing deposits table
ALTER TABLE deposits 
ADD COLUMN block_number BIGINT,
ADD COLUMN block_timestamp TIMESTAMP WITH TIME ZONE,
ADD COLUMN token_address VARCHAR(42),
ADD COLUMN raw_amount VARCHAR(100),
ADD COLUMN processing_status VARCHAR(20) DEFAULT 'DETECTED',
ADD COLUMN credited_to_magic BOOLEAN DEFAULT FALSE,
ADD COLUMN blnk_mirrored BOOLEAN DEFAULT FALSE;

-- Create chain configurations table
CREATE TABLE blockchain_chains (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chain_name VARCHAR(50) UNIQUE NOT NULL,
    chain_id INTEGER UNIQUE NOT NULL,
    rpc_url TEXT NOT NULL,
    usdc_token_address VARCHAR(42) NOT NULL,
    usdc_decimals INTEGER DEFAULT 6,
    required_confirmations INTEGER DEFAULT 12,
    is_active BOOLEAN DEFAULT TRUE,
    last_scanned_block BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert chain configurations
INSERT INTO blockchain_chains (chain_name, chain_id, rpc_url, usdc_token_address, required_confirmations) VALUES
('ethereum', 1, 'https://eth-mainnet.g.alchemy.com/v2/YOUR_KEY', '0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48', 12),
('polygon', 137, 'https://polygon-mainnet.g.alchemy.com/v2/YOUR_KEY', '0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174', 128),
('arbitrum', 42161, 'https://arb-mainnet.g.alchemy.com/v2/YOUR_KEY', '0xFF970A61A04b1cA14834A43f5dE4533eBDDB5CC8', 20),
('optimism', 10, 'https://opt-mainnet.g.alchemy.com/v2/YOUR_KEY', '0x7F5c764cBc14f9669B88837ca1490cCa17c31607', 20),
('base', 8453, 'https://base-mainnet.g.alchemy.com/v2/YOUR_KEY', '0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913', 20);

-- Add indexes for blockchain monitoring
CREATE INDEX idx_deposits_processing_status ON deposits(processing_status);
CREATE INDEX idx_deposits_block_number ON deposits(chain_id, block_number);
CREATE INDEX idx_enclave_addresses_chain ON enclave_chain_addresses(chain_type);
CREATE INDEX idx_blockchain_chains_active ON blockchain_chains(is_active) WHERE is_active = TRUE;