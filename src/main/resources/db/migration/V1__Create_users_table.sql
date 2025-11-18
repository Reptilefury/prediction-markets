CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Basic Profile
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) UNIQUE,
    display_name VARCHAR(100),
    
    -- Magic.link Integration
    magic_user_id VARCHAR(500) NOT NULL UNIQUE,
    magic_wallet_address VARCHAR(42) NOT NULL UNIQUE,
    magic_issuer VARCHAR(500),
    
    -- Enclave UDA Integration
    enclave_user_id VARCHAR(255) UNIQUE,
    enclave_uda_address VARCHAR(42) UNIQUE,
    enclave_uda_tag VARCHAR(50),
    enclave_uda_created_at TIMESTAMP WITH TIME ZONE,
    enclave_uda_status VARCHAR(20) DEFAULT 'PENDING',
    
    -- Location
    country_code VARCHAR(2),
    
    -- Account Status
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    
    -- KYC
    kyc_status VARCHAR(20) DEFAULT 'NOT_STARTED',
    kyc_level INTEGER DEFAULT 0,
    
    -- Trading Limits
    daily_deposit_limit DECIMAL(20,6),
    daily_withdrawal_limit DECIMAL(20,6),
    
    -- Blnk Integration
    blnk_identity_id VARCHAR(255) UNIQUE,
    blnk_account_id VARCHAR(255) UNIQUE,
    blnk_created_at TIMESTAMP WITH TIME ZONE,
    
    -- Referral
    referral_code VARCHAR(20) UNIQUE,
    referred_by_user_id UUID REFERENCES users(id),
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_magic_user_id ON users(magic_user_id);
CREATE INDEX idx_users_magic_wallet_address ON users(magic_wallet_address);
CREATE INDEX idx_users_referral_code ON users(referral_code);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();