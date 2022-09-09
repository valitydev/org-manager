--organization
ALTER TABLE org_manager.organization
    ADD CONSTRAINT party_unique UNIQUE (party);