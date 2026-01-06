-- Add firstName, lastName, and username columns to admin_users table
ALTER TABLE admin_users 
ADD COLUMN first_name VARCHAR(255),
ADD COLUMN last_name VARCHAR(255),
ADD COLUMN username VARCHAR(255);

-- Update existing records to split name into firstName and lastName
UPDATE admin_users 
SET 
    first_name = SPLIT_PART(name, ' ', 1),
    last_name = CASE 
        WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name FROM POSITION(' ' IN name) + 1)
        ELSE name
    END,
    username = LOWER(SUBSTRING(SPLIT_PART(name, ' ', 1), 1, 1) || 
               CASE 
                   WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name FROM POSITION(' ' IN name) + 1)
                   ELSE SUBSTRING(name FROM 2)
               END)
WHERE name IS NOT NULL;

-- Make the new columns NOT NULL after populating them
ALTER TABLE admin_users 
ALTER COLUMN first_name SET NOT NULL,
ALTER COLUMN last_name SET NOT NULL,
ALTER COLUMN username SET NOT NULL;

-- Add unique constraint on username
ALTER TABLE admin_users ADD CONSTRAINT uk_admin_users_username UNIQUE (username);

-- Create indexes
CREATE INDEX idx_admin_users_username ON admin_users(username);
CREATE INDEX idx_admin_users_first_name ON admin_users(first_name);
CREATE INDEX idx_admin_users_last_name ON admin_users(last_name);

-- Drop the old name column
ALTER TABLE admin_users DROP COLUMN name;

-- Add comments
COMMENT ON COLUMN admin_users.first_name IS 'Admin user first name';
COMMENT ON COLUMN admin_users.last_name IS 'Admin user last name';
COMMENT ON COLUMN admin_users.username IS 'Auto-generated username (first letter of first name + last name)';
