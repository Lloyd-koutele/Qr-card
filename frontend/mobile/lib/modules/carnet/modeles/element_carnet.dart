import '../../carte/modeles/carte.dart';

class ElementCarnet {
  const ElementCarnet({
    required this.id,
    this.note,
    this.enregistreeLe,
    this.carte,
  });

  factory ElementCarnet.depuisJson(Map<String, dynamic> j) => ElementCarnet(
    id: j['id'] as String,
    note: j['note'] as String?,
    enregistreeLe: j['enregistreeLe'] == null
        ? null
        : DateTime.parse(j['enregistreeLe'] as String),
    carte: j['carte'] == null
        ? null
        : Carte.depuisJson(j['carte'] as Map<String, dynamic>),
  );

  final String id;
  final String? note;
  final DateTime? enregistreeLe;

  /// Null si la carte n'est plus disponible (compte ou organisation bloque, lien regenere).
  final Carte? carte;
}
