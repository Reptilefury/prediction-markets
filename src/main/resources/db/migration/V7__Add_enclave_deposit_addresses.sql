-- Add column to store Enclave deposit addresses JSON
-- Deposit addresses are returned by Enclave API as Map<String, Object> containing chain-specific addresses
ALTER TABLE users ADD COLUMN enclave_deposit_addresses TEXT;
