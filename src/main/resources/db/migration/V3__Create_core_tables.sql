-- Create deposits table
CREATE TABLE deposits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(20,6) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    method VARCHAR(20) DEFAULT 'CRYPTO',
    status VARCHAR(20) DEFAULT 'PENDING',
    tx_hash VARCHAR(66),
    chain_id INTEGER,
    from_address VARCHAR(42),
    to_address VARCHAR(42),
    confirmations INTEGER DEFAULT 0,
    required_confirmations INTEGER DEFAULT 12,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create enclave_chain_addresses table
CREATE TABLE enclave_chain_addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    chain_type VARCHAR(20) NOT NULL,
    chain_id INTEGER NOT NULL,
    network VARCHAR(20) NOT NULL,
    deposit_address VARCHAR(42) NOT NULL,
    address_tag VARCHAR(50),
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create withdrawals table
CREATE TABLE withdrawals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(20,6) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    method VARCHAR(20) DEFAULT 'CRYPTO',
    status VARCHAR(20) DEFAULT 'PENDING',
    destination_address VARCHAR(100),
    tx_hash VARCHAR(66),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create payment_methods table
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    account_identifier VARCHAR(100) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_deposits_user_id ON deposits(user_id);
CREATE INDEX idx_deposits_status ON deposits(status);
CREATE INDEX idx_deposits_tx_hash ON deposits(tx_hash);
CREATE INDEX idx_enclave_addresses_user_id ON enclave_chain_addresses(user_id);
CREATE INDEX idx_withdrawals_user_id ON withdrawals(user_id);
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);