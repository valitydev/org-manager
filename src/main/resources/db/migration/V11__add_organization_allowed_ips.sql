ALTER TABLE org_manager.organization
    ADD COLUMN IF NOT EXISTS allowed_ips CHARACTER VARYING[] NOT NULL DEFAULT '{}';
