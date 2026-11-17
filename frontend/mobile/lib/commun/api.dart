import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'config.dart';

class ErreurApi implements Exception {
  ErreurApi(this.message, [this.statut = 0]);

  final String message;
  final int statut;

  @override
  String toString() => message;
}

/// Client HTTP du serveur : ajoute le jeton de session et traduit les erreurs en messages lisibles.
class Api {
  Api()
    : _dio = Dio(
        BaseOptions(
          baseUrl: Config.apiUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 20),
        ),
      ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, suite) {
          final j = jeton;
          if (j != null) options.headers['Authorization'] = 'Bearer $j';
          suite.next(options);
        },
      ),
    );
  }

  final Dio _dio;

  /// Jeton de la session en cours, tenu par le module compte.
  String? jeton;

  /// Appele quand le serveur refuse le jeton (expire, compte bloque, mot de passe reinitialise).
  void Function()? surSessionExpiree;

  Future<dynamic> get(String chemin) => _envoyer(() => _dio.get(chemin));

  Future<dynamic> post(String chemin, [Object? corps]) =>
      _envoyer(() => _dio.post(chemin, data: corps));

  Future<dynamic> put(String chemin, [Object? corps]) =>
      _envoyer(() => _dio.put(chemin, data: corps));

  Future<dynamic> delete(String chemin) => _envoyer(() => _dio.delete(chemin));

  Future<dynamic> envoyerFichier(String chemin, String fichier, String type) =>
      _envoyer(
        () async => _dio.post(
          chemin,
          data: FormData.fromMap({
            'fichier': await MultipartFile.fromFile(
              fichier,
              contentType: DioMediaType.parse(type),
            ),
          }),
        ),
      );

  Future<List<int>> octets(String chemin) async => List<int>.from(
    await _envoyer(
      () => _dio.get<List<int>>(
        chemin,
        options: Options(responseType: ResponseType.bytes),
      ),
    ),
  );

  /// Adresse complete d'une ressource publique (photo, logo).
  static String url(String chemin) => '${Config.apiUrl}$chemin';

  Future<dynamic> _envoyer(Future<Response<dynamic>> Function() requete) async {
    try {
      final r = await requete();
      return r.data == '' ? null : r.data;
    } on DioException catch (e) {
      final statut = e.response?.statusCode ?? 0;
      if (statut == 401 && jeton != null) surSessionExpiree?.call();
      final donnees = e.response?.data;
      final message = donnees is Map && donnees['message'] is String
          ? donnees['message'] as String
          : statut == 0
          ? 'Serveur injoignable. Vérifiez votre connexion.'
          : 'Erreur $statut';
      throw ErreurApi(message, statut);
    }
  }
}

final apiProvider = Provider<Api>((ref) => Api());
