-- L'entreprise devient l'organisation : entreprise, association, ecole, administration...
ALTER TABLE entreprise RENAME TO organisation;
ALTER TABLE entreprise_logo RENAME TO organisation_logo;
ALTER TABLE membre_entreprise RENAME TO membre_organisation;
ALTER TABLE invitation_entreprise RENAME TO invitation_organisation;
ALTER TABLE journal_entreprise RENAME TO journal_organisation;

ALTER TABLE organisation_logo RENAME COLUMN entreprise_id TO organisation_id;
ALTER TABLE membre_organisation RENAME COLUMN entreprise_id TO organisation_id;
ALTER TABLE invitation_organisation RENAME COLUMN entreprise_id TO organisation_id;
ALTER TABLE journal_organisation RENAME COLUMN entreprise_id TO organisation_id;
ALTER TABLE carte RENAME COLUMN entreprise_id TO organisation_id;

-- Contraintes puis index restants : leurs noms suivent ceux des tables
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT c.conname, t.relname FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid
             WHERE c.conname LIKE '%entreprise%'
    LOOP
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I', r.relname, r.conname,
                       replace(r.conname, 'entreprise', 'organisation'));
    END LOOP;
    FOR r IN SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() AND indexname LIKE '%entreprise%'
    LOOP
        EXECUTE format('ALTER INDEX %I RENAME TO %I', r.indexname, replace(r.indexname, 'entreprise', 'organisation'));
    END LOOP;
END $$;

ALTER TABLE organisation ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'ENTREPRISE';

UPDATE audit_admin SET action = replace(action, 'ENTREPRISE', 'ORGANISATION') WHERE action LIKE '%ENTREPRISE';
