import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/api.dart';
import '../modeles/profil.dart';

class CompteApi {
  CompteApi(this._api);

  final Api _api;

  Future<bool> googleActif() async =>
      (await _api.get('/api/auth/config'))['googleActif'] == true;

  Future<ResultatConnexion> connexion(String email, String motDePasse) async =>
      ResultatConnexion.depuisJson(
        await _api.post('/api/auth/connexion', {
          'email': email,
          'motDePasse': motDePasse,
        }),
      );

  Future<ResultatConnexion> deuxFacteurs(String mfaToken, String code) async =>
      ResultatConnexion.depuisJson(
        await _api.post('/api/auth/2fa', {'mfaToken': mfaToken, 'code': code}),
      );

  Future<InscriptionAttente> inscription(
    String email,
    String motDePasse,
    String nomAffiche,
  ) async => InscriptionAttente.depuisJson(
    await _api.post('/api/auth/inscription', {
      'email': email,
      'motDePasse': motDePasse,
      'nomAffiche': nomAffiche,
    }),
  );

  Future<ResultatConnexion> verifierEmail(String jeton, String code) async =>
      ResultatConnexion.depuisJson(
        await _api.post('/api/auth/inscription/verifier', {
          'jeton': jeton,
          'code': code,
        }),
      );

  Future<InscriptionAttente> renvoyerCode(String jeton) async =>
      InscriptionAttente.depuisJson(
        await _api.post('/api/auth/inscription/renvoyer', {'jeton': jeton}),
      );

  Future<void> motDePasseOublie(String email) =>
      _api.post('/api/auth/mot-de-passe/oublie', {'email': email});

  Future<void> reinitialiser(String email, String code, String nouveau) =>
      _api.post('/api/auth/mot-de-passe/reinitialiser', {
        'email': email,
        'code': code,
        'nouveau': nouveau,
      });

  Future<ResultatConnexion> echangeGoogle(
    String code,
    String verificateur,
  ) async => ResultatConnexion.depuisJson(
    await _api.post('/api/auth/google/echange', {
      'code': code,
      'verificateur': verificateur,
    }),
  );

  Future<Profil> profil() async => Profil.depuisJson(await _api.get('/api/me'));

  Future<Profil> changerMotDePasse(String? ancien, String nouveau) async =>
      Profil.depuisJson(
        await _api.put('/api/me/mot-de-passe', {
          'ancien': ancien,
          'nouveau': nouveau,
        }),
      );
}

final compteApiProvider = Provider<CompteApi>(
  (ref) => CompteApi(ref.watch(apiProvider)),
);
