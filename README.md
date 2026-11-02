# MadeQrCard

Cartes de visite numériques au format carte d'identité, partagées par QR code.

- **Client** : React (Vite), servi par Caddy
- **Serveur** : Spring Boot 3 (Java 21), JWT, connexion Google, double authentification (TOTP)
- **Base** : PostgreSQL 16, migrations Flyway

## Lancer en local

```bash
cp .env.example .env      # puis remplir les valeurs (secrets : openssl rand -base64 48)
docker compose up -d --build
```

Application : http://localhost:8088

Au premier démarrage, le compte administrateur est créé avec `ADMIN_EMAIL` et `ADMIN_PASSWORD`.
À sa première connexion, il doit changer ce mot de passe puis activer la double authentification
(Google Authenticator, Microsoft Authenticator ou Authy) avant d'accéder à l'administration.

Développement sans Docker pour le front : `cd frontend && npm install && npm run dev`
(le serveur de Vite transmet `/api` à `localhost:8080`).

## Fonctionnement

| Qui | Accès |
|---|---|
| Propriétaire | Lit et modifie sa carte une fois connecté |
| Toute personne avec le lien ou le QR code | Lecture seule, boutons qui ouvrent les applications, ajout aux contacts (vCard) |
| Utilisateur connecté | Peut enregistrer la carte d'un autre dans son carnet |
| Administrateur | Voit les comptes (email, statut, dates) et peut les bloquer, sans lire ni modifier les cartes |

- Le lien `/c/{jeton}` utilise un jeton aléatoire de 14 caractères : il ne peut pas être deviné.
- « Nouveau lien » invalide l'ancien QR code et retire la carte des carnets.
- Un compte bloqué ne peut plus se connecter et sa carte affiche « Carte indisponible ».
- Les tables `organisation` et `unite_organisationnelle` sont prêtes pour la future offre entreprise.

## Connexion avec Google (facultatif)

1. Console Google Cloud > API et services > Identifiants > Créer un ID client OAuth (application Web).
2. URI de redirection autorisée : `<APP_URL>/login/oauth2/code/google`.
3. Renseigner `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET` dans `.env`, puis `docker compose up -d`.

Le bouton « Continuer avec Google » n'apparaît que si ces deux valeurs sont renseignées.

## Déploiement sur un serveur (Oracle Cloud Always Free)

1. Créer une VM **Ampere A1 (ARM)** sous Ubuntu, ouvrir les ports 80 et 443
   (liste de sécurité du réseau **et** pare-feu de la VM).
2. Installer Docker : `curl -fsSL https://get.docker.com | sh`.
3. Faire pointer le nom de domaine (enregistrement A) vers l'IP publique de la VM.
4. Copier le projet dans `/opt/madeqrcard`, créer `.env` avec :
   ```
   APP_URL=https://madeqrcard.mon-domaine.com
   SITE_ADDRESS=madeqrcard.mon-domaine.com
   HTTP_PORT=80
   HTTPS_PORT=443
   ```
5. `docker compose up -d --build` : Caddy obtient le certificat HTTPS automatiquement.
6. Sauvegarde quotidienne (crontab) : `0 3 * * * cd /opt/madeqrcard && ./scripts/sauvegarde.sh`,
   puis copier le dossier `backups/` hors du serveur (Cloudflare R2, par exemple).

Derrière Cloudflare (proxy activé), décommenter `trusted_proxies cloudflare` dans
`frontend/Caddyfile` pour que la limitation de requêtes voie la vraie IP des visiteurs.

## Changer d'hébergeur

Tout est dans Docker et configuré par `.env`. Pour migrer :
`./scripts/sauvegarde.sh`, copier le projet, `.env` et la sauvegarde, `docker compose up -d`,
puis restaurer (commande en tête de `scripts/sauvegarde.sh`).
