import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../commun/api.dart';
import '../modeles/carte.dart';

class CarteApi {
  CarteApi(this._api);

  final Api _api;

  /// organisationId vide : carte personnelle ; sinon la carte professionnelle de cette organisation.
  static String _base(String? organisationId) => organisationId == null
      ? '/api/me/carte'
      : '/api/me/organisations/$organisationId/carte';

  Future<Carte> maCarte({String? organisationId}) async =>
      Carte.depuisJson(await _api.get(_base(organisationId)));

  /// Une carte professionnelle par organisation active du compte.
  Future<List<Carte>> cartesPro() async => [
    for (final c in (await _api.get('/api/me/cartes-pro')) as List)
      Carte.depuisJson(c as Map<String, dynamic>),
  ];

  Future<Carte> modifier(
    String? organisationId,
    Map<String, dynamic> saisie,
  ) async => Carte.depuisJson(await _api.put(_base(organisationId), saisie));

  Future<Carte> envoyerPhoto(
    String? organisationId,
    String fichier,
    String type,
  ) async => Carte.depuisJson(
    await _api.envoyerFichier('${_base(organisationId)}/photo', fichier, type),
  );

  Future<Carte> supprimerPhoto(String? organisationId) async =>
      Carte.depuisJson(await _api.delete('${_base(organisationId)}/photo'));

  Future<Carte> regenererLien(String? organisationId) async => Carte.depuisJson(
    await _api.post('${_base(organisationId)}/regenerer-lien'),
  );

  Future<Carte> publique(String jeton) async => Carte.depuisJson(
    await _api.get('/api/public/cartes/${Uri.encodeComponent(jeton)}'),
  );

  /// La vCard est proposee au partage : sur iPhone, « Contacts » l'ajoute au carnet du telephone.
  Future<void> partagerVcard(Carte carte) async {
    final octets = await _api.octets('/api/public/cartes/${carte.jeton}/vcard');
    final dossier = await getTemporaryDirectory();
    final fichier = File(
      '${dossier.path}/${nomFichierVcard(carte.nomComplet)}',
    );
    await fichier.writeAsBytes(octets);
    await SharePlus.instance.share(
      ShareParams(files: [XFile(fichier.path, mimeType: 'text/vcard')]),
    );
  }
}

final carteApiProvider = Provider<CarteApi>(
  (ref) => CarteApi(ref.watch(apiProvider)),
);

/// Carte personnelle (null) ou professionnelle d'une organisation du compte connecte.
final maCarteProvider = FutureProvider.autoDispose.family<Carte, String?>(
  (ref, organisationId) =>
      ref.watch(carteApiProvider).maCarte(organisationId: organisationId),
);

final cartesProProvider = FutureProvider.autoDispose<List<Carte>>(
  (ref) => ref.watch(carteApiProvider).cartesPro(),
);

final cartePubliqueProvider = FutureProvider.autoDispose.family<Carte, String>(
  (ref, jeton) => ref.watch(carteApiProvider).publique(jeton),
);

/// Ouvre l'application correspondante (telephone, WhatsApp, navigateur...).
Future<bool> ouvrirLien(String lien) async {
  try {
    return await launchUrl(
      Uri.parse(lien),
      mode: LaunchMode.externalApplication,
    );
  } catch (_) {
    return false;
  }
}

/// « Aicha Kone.vcf », comme le nom donne par le serveur : lisible une fois partage.
String nomFichierVcard(String nomComplet) {
  const avec = 'àâäáãåçéèêëíìîïñóòôöõúùûüýÿÀÂÄÁÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝ';
  const sans = 'aaaaaaceeeeiiiinooooouuuuyyAAAAAACEEEEIIIINOOOOOUUUUY';
  final b = StringBuffer();
  for (final c in nomComplet.split('')) {
    final i = avec.indexOf(c);
    b.write(i < 0 ? c : sans[i]);
  }
  final nom = b
      .toString()
      .replaceAll(RegExp(r'[^A-Za-z0-9 ._-]'), '')
      .replaceAll(RegExp(r'\s+'), ' ')
      .trim();
  return '${nom.isEmpty ? 'contact' : nom}.vcf';
}
