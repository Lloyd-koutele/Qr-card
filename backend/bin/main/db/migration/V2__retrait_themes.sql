-- Couleur de carte et mode clair/sombre ne sont plus geres par le serveur.
ALTER TABLE carte DROP COLUMN theme;
ALTER TABLE utilisateur DROP COLUMN mode_affichage;
