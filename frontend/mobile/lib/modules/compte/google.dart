import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

import '../../commun/api.dart';
import '../../commun/config.dart';
import 'donnees/compte_api.dart';
import 'modeles/profil.dart';

const _messages = {
  'google': 'La connexion avec Google a échoué. Réessayez.',
  'compte-existant': 'Cet email est déjà lié à un autre compte Google.',
  'email-non-verifie': "Votre adresse Google n'est pas vérifiée.",
  'bloque': "Ce compte est bloqué. Contactez l'administrateur.",
};

/// Google s'ouvre dans un navigateur integre. Le serveur renvoie vers madeqrcard://auth un code
/// a usage unique, echange ici avec le verificateur que seule cette application connait.
/// Renvoie null si l'utilisateur ferme la fenetre.
Future<ResultatConnexion?> connexionGoogle(CompteApi api) async {
  final hasard = Random.secure();
  final verificateur = base64Url
      .encode(List<int>.generate(32, (_) => hasard.nextInt(256)))
      .replaceAll('=', '');
  final defi = base64Url
      .encode(sha256.convert(ascii.encode(verificateur)).bytes)
      .replaceAll('=', '');

  final String retour;
  try {
    retour = await FlutterWebAuth2.authenticate(
      url: '${Config.apiUrl}/oauth2/authorization/google?defi=$defi',
      callbackUrlScheme: 'madeqrcard',
    );
  } on PlatformException catch (e) {
    if (e.code == 'CANCELED') return null;
    throw ErreurApi(_messages['google']!);
  }

  final parametres = Uri.parse(retour).queryParameters;
  final erreur = parametres['erreur'];
  if (erreur != null)
    throw ErreurApi(_messages[erreur] ?? _messages['google']!);
  final code = parametres['code'];
  if (code == null) throw ErreurApi(_messages['google']!);
  return api.echangeGoogle(code, verificateur);
}
