-- L'entreprise remplace les notions d'organisation et d'unite organisationnelle.
ALTER TABLE utilisateur DROP COLUMN organisation_id, DROP COLUMN uo_id;
DROP TABLE unite_organisationnelle;
DROP TABLE organisation;

-- Le role dans une entreprise est porte par le rattachement, pas par le compte.
UPDATE utilisateur SET role = 'USER' WHERE role = 'ORG_ADMIN';

CREATE TABLE entreprise (
    id            UUID PRIMARY KEY,
    nom           VARCHAR(150) NOT NULL,
    adresse       VARCHAR(255),
    couleur       VARCHAR(7)   NOT NULL DEFAULT '#c8102e',
    logo_version  BIGINT,
    statut        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    motif_blocage VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Logo a part pour ne pas le charger avec chaque carte
CREATE TABLE entreprise_logo (
    entreprise_id UUID PRIMARY KEY REFERENCES entreprise (id) ON DELETE CASCADE,
    donnees       BYTEA       NOT NULL,
    type          VARCHAR(50) NOT NULL
);

-- email : l'adresse invitee, la seule que voit l'entreprise. L'adresse du compte peut etre differente.
CREATE TABLE membre_entreprise (
    id             UUID PRIMARY KEY,
    entreprise_id  UUID         NOT NULL REFERENCES entreprise (id) ON DELETE CASCADE,
    utilisateur_id UUID         NOT NULL UNIQUE REFERENCES utilisateur (id) ON DELETE CASCADE,
    email          VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (entreprise_id, email)
);

-- Une ligne par adresse et par entreprise. Seule l'empreinte du jeton est gardee.
CREATE TABLE invitation_entreprise (
    id             UUID PRIMARY KEY,
    entreprise_id  UUID         NOT NULL REFERENCES entreprise (id) ON DELETE CASCADE,
    email          VARCHAR(255) NOT NULL,
    nom            VARCHAR(120),
    poste          VARCHAR(120),
    jeton_empreinte VARCHAR(64) NOT NULL UNIQUE,
    statut         VARCHAR(20)  NOT NULL,
    expire_le      TIMESTAMPTZ  NOT NULL,
    envoye_le      TIMESTAMPTZ  NOT NULL,
    UNIQUE (entreprise_id, email)
);
CREATE INDEX idx_invitation_quota ON invitation_entreprise (entreprise_id, envoye_le);

CREATE TABLE journal_entreprise (
    id            UUID PRIMARY KEY,
    entreprise_id UUID         NOT NULL REFERENCES entreprise (id) ON DELETE CASCADE,
    auteur        VARCHAR(255) NOT NULL,
    action        VARCHAR(30)  NOT NULL,
    cible         VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_journal_entreprise ON journal_entreprise (entreprise_id, created_at DESC);

-- Une personne garde sa carte personnelle et peut avoir une carte professionnelle en plus.
ALTER TABLE carte DROP CONSTRAINT carte_utilisateur_id_key;
ALTER TABLE carte ADD COLUMN entreprise_id UUID REFERENCES entreprise (id) ON DELETE CASCADE;
CREATE UNIQUE INDEX carte_personnelle_unique ON carte (utilisateur_id) WHERE entreprise_id IS NULL;
CREATE UNIQUE INDEX carte_professionnelle_unique ON carte (utilisateur_id) WHERE entreprise_id IS NOT NULL;
CREATE INDEX idx_carte_entreprise ON carte (entreprise_id);
