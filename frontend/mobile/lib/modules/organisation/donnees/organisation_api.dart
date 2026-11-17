import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/api.dart';
import '../modeles/organisation.dart';

/// Usage personnel seulement : la gestion (membres, invitations, journal) se fait sur le site.
class OrganisationApi {
  OrganisationApi(this._api);

  final Api _api;

  /// Organisations du compte, dans l'ordre ou il les a rejointes.
  Future<List<Organisation>> miennes() async => [
    for (final o in (await _api.get('/api/organisations')) as List)
      Organisation.depuisJson(o as Map<String, dynamic>),
  ];

  Future<void> quitter(String id) =>
      _api.post('/api/organisations/${Uri.encodeComponent(id)}/quitter');

  Future<Invitation> invitation(String jeton) async => Invitation.depuisJson(
    await _api.get('/api/public/invitations/${Uri.encodeComponent(jeton)}'),
  );

  Future<void> accepter(String jeton) =>
      _api.post('/api/invitations/${Uri.encodeComponent(jeton)}/accepter');

  Future<void> refuser(String jeton) => _api.post(
    '/api/public/invitations/${Uri.encodeComponent(jeton)}/refuser',
  );
}

final organisationApiProvider = Provider<OrganisationApi>(
  (ref) => OrganisationApi(ref.watch(apiProvider)),
);

final organisationsProvider = FutureProvider.autoDispose<List<Organisation>>(
  (ref) => ref.watch(organisationApiProvider).miennes(),
);
