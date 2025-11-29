-- Rename blnk_account_id column to blnk_balance_id to reflect balance-focused architecture
ALTER TABLE users RENAME COLUMN blnk_account_id TO blnk_balance_id;

-- Add comment to clarify the purpose
COMMENT ON COLUMN users.blnk_balance_id IS 'Blnk balance ID for user financial transactions';
