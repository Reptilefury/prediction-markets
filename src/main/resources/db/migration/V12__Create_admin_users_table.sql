-- Create admin_users table
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    role_id UUID NOT NULL REFERENCES admin_roles(id) ON DELETE RESTRICT,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes on admin_users
CREATE INDEX idx_admin_users_keycloak_id ON admin_users(keycloak_user_id);
CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_role_id ON admin_users(role_id);
CREATE INDEX idx_admin_users_status ON admin_users(status);
CREATE INDEX idx_admin_users_name ON admin_users(name);
CREATE INDEX idx_admin_users_created_at ON admin_users(created_at);

-- Add comments for documentation
COMMENT ON TABLE admin_users IS 'Admin users with roles and permissions for system administration';
COMMENT ON COLUMN admin_users.keycloak_user_id IS 'The UUID of the user in Keycloak (nullable for users not yet synced)';
COMMENT ON COLUMN admin_users.role_id IS 'References the admin role assigned to this user';
COMMENT ON COLUMN admin_users.status IS 'Current status of the admin user account';
COMMENT ON COLUMN admin_users.two_factor_enabled IS 'Whether 2FA is enabled for this user';
COMMENT ON COLUMN admin_users.login_attempts IS 'Number of failed login attempts';
COMMENT ON COLUMN admin_users.locked_until IS 'Account locked until this timestamp (null if not locked)';
