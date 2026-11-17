-- Mot de passe oublie : code a 6 chiffres envoye par email.
CREATE TABLE reinitialisation_mot_de_passe (
    utilisateur_id UUID PRIMARY KEY REFERENCES utilisateur (id) ON DELETE CASCADE,
    code           VARCHAR(100) NOT NULL,
    essais         INT          NOT NULL DEFAULT 0,
    expire_le      TIMESTAMPTZ  NOT NULL,
    envoye_le      TIMESTAMPTZ  NOT NULL
);

-- Les jetons emis avant cette date sont refuses (apres une reinitialisation).
ALTER TABLE utilisateur ADD COLUMN sessions_valides_depuis TIMESTAMPTZ;
