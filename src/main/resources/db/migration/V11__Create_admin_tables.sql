-- Create admin_roles table
CREATE TABLE IF NOT EXISTS admin_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_role_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_composite BOOLEAN DEFAULT FALSE,
    priority INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_synced_at TIMESTAMP WITH TIME ZONE
);

-- Create index on keycloak_role_id for faster lookups
CREATE INDEX idx_admin_roles_keycloak_id ON admin_roles(keycloak_role_id);
CREATE INDEX idx_admin_roles_name ON admin_roles(name);
CREATE INDEX idx_admin_roles_active ON admin_roles(is_active);

-- Create admin_permissions table
CREATE TABLE IF NOT EXISTS admin_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_role_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_synced_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes on admin_permissions
CREATE INDEX idx_admin_permissions_keycloak_id ON admin_permissions(keycloak_role_id);
CREATE INDEX idx_admin_permissions_name ON admin_permissions(name);
CREATE INDEX idx_admin_permissions_module ON admin_permissions(module);
CREATE INDEX idx_admin_permissions_action ON admin_permissions(action);
CREATE INDEX idx_admin_permissions_module_action ON admin_permissions(module, action);
CREATE INDEX idx_admin_permissions_active ON admin_permissions(is_active);

-- Create role_permission_mappings table (many-to-many relationship)
CREATE TABLE IF NOT EXISTS role_permission_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES admin_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES admin_permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(role_id, permission_id)
);

-- Create indexes on role_permission_mappings
CREATE INDEX idx_rpm_role_id ON role_permission_mappings(role_id);
CREATE INDEX idx_rpm_permission_id ON role_permission_mappings(permission_id);

-- Add comments for documentation
COMMENT ON TABLE admin_roles IS 'Caches Keycloak realm roles for admin users';
COMMENT ON TABLE admin_permissions IS 'Caches Keycloak client roles (permissions) for admin users';
COMMENT ON TABLE role_permission_mappings IS 'Maps which permissions belong to which roles';
COMMENT ON COLUMN admin_roles.keycloak_role_id IS 'The UUID of the role in Keycloak';
COMMENT ON COLUMN admin_permissions.keycloak_role_id IS 'The UUID of the client role in Keycloak';
COMMENT ON COLUMN admin_roles.last_synced_at IS 'Last time this role was synced from Keycloak';
COMMENT ON COLUMN admin_permissions.last_synced_at IS 'Last time this permission was synced from Keycloak';
