import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Keychain sur iOS, Keystore sur Android. Une erreur de lecture vaut une valeur absente.
class Stockage {
  static const _coffre = FlutterSecureStorage();

  static Future<String?> lire(String cle) async {
    try {
      return await _coffre.read(key: cle);
    } catch (_) {
      return null;
    }
  }

  static Future<void> ecrire(String cle, String valeur) async {
    try {
      await _coffre.write(key: cle, value: valeur);
    } catch (_) {}
  }

  static Future<void> effacer(String cle) async {
    try {
      await _coffre.delete(key: cle);
    } catch (_) {}
  }
}
