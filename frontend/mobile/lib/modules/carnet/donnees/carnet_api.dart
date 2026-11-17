import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/api.dart';
import '../modeles/element_carnet.dart';

class CarnetApi {
  CarnetApi(this._api);

  final Api _api;

  Future<List<ElementCarnet>> lister() async => [
    for (final e in (await _api.get('/api/carnet') as List))
      ElementCarnet.depuisJson(e as Map<String, dynamic>),
  ];

  Future<void> modifierNote(String id, String note) =>
      _api.put('/api/carnet/$id', {'note': note});

  Future<void> retirer(String id) => _api.delete('/api/carnet/$id');
}

final carnetApiProvider = Provider<CarnetApi>(
  (ref) => CarnetApi(ref.watch(apiProvider)),
);

final carnetProvider = FutureProvider.autoDispose<List<ElementCarnet>>(
  (ref) => ref.watch(carnetApiProvider).lister(),
);
