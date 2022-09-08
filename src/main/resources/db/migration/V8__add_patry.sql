-- organization
ALTER TABLE org_manager.organization
    ADD COLUMN party CHARACTER VARYING NOT NULL DEFAULT 'default';