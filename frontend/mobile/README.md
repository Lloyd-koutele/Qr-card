# MadeQrCard mobile

Application Flutter (Android et iOS) pour l'usage personnel : ma carte et ma carte d'entreprise, QR code en grand,
scanner, carnet, réponse aux invitations. L'administration (générale ou d'une entreprise) reste sur le site.

## Lancer

Le serveur doit tourner (`docker compose up -d` à la racine du projet).

```bash
flutter run --dart-define=API_URL=http://localhost:8088          # simulateur iPhone
flutter run --dart-define=API_URL=http://10.0.2.2:8088           # émulateur Android
```

`SITE_URL` (par défaut `https://madeqrcard.sn`) est l'adresse mise dans les liens partagés et les QR codes.

## Structure

Mêmes modules que le site et le serveur : `lib/app` (routes, thème, navigation), `lib/commun` (API, stockage sécurisé,
widgets), `lib/modules/{compte,carte,carnet,scan,entreprise}`, chacun avec `donnees` (appels à l'API), `modeles` et
`ecrans`.

- Session : jeton JWT dans le Keychain (iOS) ou le Keystore (Android).
- Connexion Google : navigateur intégré, retour par `madeqrcard://auth` avec un code à usage unique échangé contre la
  session grâce à un vérificateur que seule l'application connaît. Elle n'aboutit que si l'adresse de l'API est celle
  déclarée dans la console Google (`<API_URL>/login/oauth2/code/google`).

## Tests

```bash
flutter analyze
flutter test
```
