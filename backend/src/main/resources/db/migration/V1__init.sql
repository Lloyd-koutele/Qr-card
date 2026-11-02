-- Version entreprise (premium) : tables posees des maintenant, utilisees plus tard
CREATE TABLE organisation (
    id             UUID PRIMARY KEY,
    nom            VARCHAR(150) NOT NULL,
    logo           BYTEA,
    logo_type      VARCHAR(50),
    offre          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    statut         VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    fin_abonnement DATE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE unite_organisationnelle (
    id              UUID PRIMARY KEY,
    organisation_id UUID         NOT NULL REFERENCES organisation (id) ON DELETE CASCADE,
    parent_id       UUID REFERENCES unite_organisationnelle (id) ON DELETE CASCADE,
    nom             VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE utilisateur (
    id                   UUID PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    nom_affiche          VARCHAR(120) NOT NULL,
    mot_de_passe         VARCHAR(100),
    google_id            VARCHAR(100) UNIQUE,
    role                 VARCHAR(20)  NOT NULL,
    statut               VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    motif_blocage        VARCHAR(500),
    mode_affichage       VARCHAR(20)  NOT NULL DEFAULT 'SYSTEME',
    totp_secret          VARCHAR(255),
    totp_actif           BOOLEAN      NOT NULL DEFAULT FALSE,
    codes_secours        TEXT,
    echecs_2fa           INT          NOT NULL DEFAULT 0,
    verrouille_jusqua    TIMESTAMPTZ,
    changer_mot_de_passe BOOLEAN      NOT NULL DEFAULT FALSE,
    organisation_id      UUID REFERENCES organisation (id) ON DELETE SET NULL,
    uo_id                UUID REFERENCES unite_organisationnelle (id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    derniere_connexion   TIMESTAMPTZ
);

CREATE SEQUENCE carte_numero_seq START 1;

CREATE TABLE carte (
    id             UUID PRIMARY KEY,
    utilisateur_id UUID        NOT NULL UNIQUE REFERENCES utilisateur (id) ON DELETE CASCADE,
    jeton          VARCHAR(32) NOT NULL UNIQUE,
    numero         VARCHAR(30) NOT NULL UNIQUE,
    nom            VARCHAR(80),
    prenom         VARCHAR(80),
    profession     VARCHAR(120),
    bio            VARCHAR(300),
    theme          VARCHAR(20) NOT NULL DEFAULT 'ROUGE',
    photo_version  BIGINT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Photo a part pour ne pas la charger avec chaque carte
CREATE TABLE carte_photo (
    carte_id UUID PRIMARY KEY REFERENCES carte (id) ON DELETE CASCADE,
    donnees  BYTEA       NOT NULL,
    type     VARCHAR(50) NOT NULL
);

CREATE TABLE contact (
    id       UUID PRIMARY KEY,
    carte_id UUID         NOT NULL REFERENCES carte (id) ON DELETE CASCADE,
    type     VARCHAR(20)  NOT NULL,
    valeur   VARCHAR(255) NOT NULL,
    ordre    INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_contact_carte ON contact (carte_id);

CREATE TABLE carte_enregistree (
    id             UUID PRIMARY KEY,
    utilisateur_id UUID        NOT NULL REFERENCES utilisateur (id) ON DELETE CASCADE,
    carte_id       UUID        NOT NULL REFERENCES carte (id) ON DELETE CASCADE,
    note           VARCHAR(120),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (utilisateur_id, carte_id)
);

CREATE TABLE audit_admin (
    id          UUID PRIMARY KEY,
    admin_id    UUID REFERENCES utilisateur (id) ON DELETE SET NULL,
    admin_email VARCHAR(255) NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    cible_id    UUID,
    cible_email VARCHAR(255),
    motif       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_date ON audit_admin (created_at DESC);
