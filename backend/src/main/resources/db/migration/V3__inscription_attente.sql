-- Inscription en attente : le compte n'est cree qu'une fois l'email verifie par code.
CREATE TABLE inscription_attente (
    email        VARCHAR(255) PRIMARY KEY,
    jeton        VARCHAR(64)  NOT NULL UNIQUE,
    nom_affiche  VARCHAR(120) NOT NULL,
    mot_de_passe VARCHAR(100) NOT NULL,
    code         VARCHAR(100) NOT NULL,
    essais       INT          NOT NULL DEFAULT 0,
    expire_le    TIMESTAMPTZ  NOT NULL,
    envoye_le    TIMESTAMPTZ  NOT NULL
);
