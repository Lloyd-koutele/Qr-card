/// Adresses choisies au lancement, par exemple :
/// flutter run --dart-define=API_URL=http://localhost:8088
class Config {
  /// Serveur MadeQrCard (Caddy). Simulateur iPhone : localhost ; emulateur Android : 10.0.2.2.
  static const apiUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://localhost:8088',
  );

  /// Site public : les liens partages et les QR codes pointent vers lui, pas vers l'API de developpement.
  static const siteUrl = String.fromEnvironment(
    'SITE_URL',
    defaultValue: 'https://madeqrcard.sn',
  );

  static String lienCarte(String jeton) => '$siteUrl/c/$jeton';
}
