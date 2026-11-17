-- Une personne peut appartenir a plusieurs organisations, avec une carte professionnelle pour chacune.
ALTER TABLE membre_organisation DROP CONSTRAINT membre_organisation_utilisateur_id_key;
ALTER TABLE membre_organisation ADD CONSTRAINT membre_organisation_utilisateur_unique UNIQUE (organisation_id, utilisateur_id);
CREATE INDEX idx_membre_utilisateur ON membre_organisation (utilisateur_id);

DROP INDEX carte_professionnelle_unique;
CREATE UNIQUE INDEX carte_professionnelle_unique ON carte (utilisateur_id, organisation_id) WHERE organisation_id IS NOT NULL;
