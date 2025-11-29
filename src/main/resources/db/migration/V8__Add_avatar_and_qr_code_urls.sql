-- Add avatar and QR code URL columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS proxy_wallet_qr_code_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS enclave_uda_qr_code_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS evm_deposit_qr_codes VARCHAR(2000);
ALTER TABLE users ADD COLUMN IF NOT EXISTS solana_deposit_qr_code_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bitcoin_deposit_qr_codes VARCHAR(2000);
