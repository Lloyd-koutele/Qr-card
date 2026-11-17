# MadeQrCard

Cartes de visite numériques au format carte d'identité, partagées par QR code.

- **Client** : React (Vite), servi par Caddy
- **Serveur** : Spring Boot 3 (Java 21, Gradle), JWT, connexion Google, double authentification (TOTP)
- **Base** : PostgreSQL 16, migrations Flyway

## Lancer en local

```bash
cp .env.example .env      # puis remplir les valeurs (secrets : openssl rand -base64 48)
docker compose up -d --build
```

Application : http://localhost:8088

En HTTPS : https://madeqrcard.sn, servi par le Traefik du projet (service `traefik`, configuration
dans `traefik/dynamic.yml`). Il faut sur le poste :
- l'entrée `127.0.0.1 madeqrcard.sn` dans `/etc/hosts` et `APP_URL=https://madeqrcard.sn` dans `.env` ;
- le certificat local : `cd traefik/certs && mkcert madeqrcard.sn` (non versionné).

Traefik occupe les ports 80 et 443 : un autre proxy qui les utilise (celui de MadeArchive par
exemple) doit être arrêté, ou ces ports changés avec `TRAEFIK_HTTP_PORT` et `TRAEFIK_HTTPS_PORT`.

Au premier démarrage, le compte administrateur est créé avec `ADMIN_EMAIL` et `ADMIN_PASSWORD`.
À sa première connexion, il doit changer ce mot de passe puis activer la double authentification
(Google Authenticator, Microsoft Authenticator ou Authy) avant d'accéder à l'administration.

Développement sans Docker :
- API : `cd backend && ./gradlew bootRun` (Java 21 est téléchargé automatiquement si besoin ;
  les variables `DB_URL`, `JWT_SECRET`, `APP_CLE_CHIFFREMENT`… doivent être définies) ;
- front : `cd frontend/web && npm install && npm run dev` (Vite transmet `/api` à `localhost:8080`).

Numéros de téléphone et WhatsApp : le serveur vérifie le nombre de chiffres et les préfixes de chaque
pays (bibliothèque libphonenumber). Le site et l'application mobile partagent la liste des pays
(nom, indicatif, longueurs), générée par `cd frontend/web && npm run pays` : à relancer après une mise à
jour de `libphonenumber-js`.
- mobile : voir [frontend/mobile/README.md](frontend/mobile/README.md) (Flutter, simulateur iPhone ou émulateur Android).

## Structure du code

Le code est découpé par module fonctionnel, côté serveur comme côté client :

| Module | Rôle | Peut dépendre de |
|---|---|---|
| `commun` | Configuration, erreurs, chiffrement, limitation de requêtes | rien |
| `compte` | Comptes, connexion, Google, 2FA, sécurité, profil | `commun` |
| `carte` | Carte, contacts, photo, lien public, vCard | `compte`, `commun` |
| `carnet` | Cartes enregistrées | `carte`, `compte`, `commun` |
| `organisation` | Organisations, invitations, membres, identité des cartes professionnelles | `carte`, `compte`, `commun` |
| `admin` | Supervision des comptes et des organisations, blocage, journal | `organisation`, `carte`, `compte`, `commun` |

- **Serveur** (`backend/src/main/java/made/qrcard/<module>/`) : `api` (contrôleurs), `dto`, `service`, `domaine` (entités
  et dépôts). Un module bas ne connaît pas les modules au-dessus de lui : il publie un événement (`CompteCree`) ou
  déclare une interface (`EnregistrementsDeCarte`). `./gradlew test` échoue si ces règles sont enfreintes.
- **Application mobile** (`frontend/mobile/lib/`) : mêmes modules, en Flutter ; l'administration reste sur le site.
- **Client** (`frontend/web/src/`) : `app` (routes et mise en page), `commun`, `modules/<module>`. Un module n'utilise un
  autre module que par son `index.js`.

Pour ajouter un domaine : créer un nouveau module des deux côtés, puis sa migration
Flyway dans `backend/src/main/resources/db/migration`.

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

## Organisations

Une organisation peut être une entreprise, une association ou ONG, une école ou université, une administration ou autre chose (champ « type », sans effet sur les droits). Une personne peut appartenir à plusieurs organisations (20 au plus), avec une carte professionnelle distincte dans chacune ; celle qui crée une organisation en devient l'administrateur. Les routes portent l'organisation visée : `/api/organisations/{id}/…` et `/api/me/organisations/{id}/carte` (la liste des cartes professionnelles : `/api/me/cartes-pro`).

- Sans compte : la page publique `/creer-organisation` demande l'organisation et le compte de l'administrateur. Par email, le serveur crée les deux une fois le code de vérification saisi (`POST /api/auth/inscription/organisation`). Avec Google, l'organisation est gardée dans l'onglet et créée au retour de la connexion.
- Avec un compte : onglet Organisation de l'application. L'application mobile renvoie vers le site pour la création.

L'administrateur **invite une adresse email**, il ne crée jamais de compte :

1. Il reçoit toujours la même réponse : l'envoi ne consulte pas les comptes.
2. La personne reçoit un lien valable 7 jours (seule l'empreinte du jeton est conservée) et choisit de se connecter, de créer un
   compte ou de refuser. La page du lien propose les mêmes choix à tout le monde.
3. Rien n'est rattaché tant qu'elle n'a pas accepté. Elle peut accepter avec un compte dont l'adresse est différente.

| | Propriétaire | Administrateur de l'organisation |
|---|---|---|
| Compte, adresse du compte, carte personnelle, carnet | Oui | Jamais |
| Adresse invitée, rôle dans l'organisation | Oui | Oui |
| Carte professionnelle | Modifie photo, poste, bio et contacts | Lit la carte, gère nom, logo, couleur et adresse |

- Au départ d'une personne (ou à la suppression de l'organisation), sa carte professionnelle est effacée et son QR code affiche
  « Carte indisponible ». Son compte, sa carte personnelle et son carnet ne changent pas.
- Une adresse reçoit au plus une invitation par jour d'une même organisation, et une organisation au plus 100 invitations par jour.
- Une organisation bloquée par l'administrateur général : ses cartes deviennent indisponibles et elle ne peut plus rien modifier.

## Emails : inscription et mot de passe oublié

Un code à 6 chiffres est envoyé par email pour confirmer l'adresse à l'inscription (le compte n'est créé qu'après sa
saisie) et pour choisir un nouveau mot de passe. Renseigner `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` et
`MAIL_FROM` dans `.env` avec les accès d'un fournisseur SMTP (Brevo, par exemple), puis `docker compose up -d`.

Sans `MAIL_HOST`, aucun email ne part et le message est écrit dans les logs : `docker compose logs madeqrcard-app`.
À réserver au développement.

## Connexion avec Google (facultatif)

1. Console Google Cloud > API et services > Identifiants > Créer un ID client OAuth (application Web).
2. URI de redirection autorisée : `<APP_URL>/login/oauth2/code/google`.
3. Renseigner `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET` dans `.env`, puis `docker compose up -d`.

Le bouton « Continuer avec Google » n'apparaît que si ces deux valeurs sont renseignées.
Si un compte existe déjà avec la même adresse, Google le connecte à ce compte (la 2FA reste demandée si elle est active).

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
`frontend/web/Caddyfile` pour que la limitation de requêtes voie la vraie IP des visiteurs.

## Changer d'hébergeur

Tout est dans Docker et configuré par `.env`. Pour migrer :
`./scripts/sauvegarde.sh`, copier le projet, `.env` et la sauvegarde, `docker compose up -d`,
puis restaurer (commande en tête de `scripts/sauvegarde.sh`).
