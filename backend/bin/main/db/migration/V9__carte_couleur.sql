-- Couleur choisie pour une carte personnelle ; vide = rouge MadeQrCard. Une carte professionnelle prend celle de son organisation.
ALTER TABLE carte ADD COLUMN couleur VARCHAR(7);
