import 'liste_pays.dart';

export 'liste_pays.dart';

/// Pays pour les numeros de telephone : memes donnees que le site (pays.json).
class Pays {
  const Pays(
    this.iso,
    this.nom,
    this.indicatif,
    this.longueurs,
    this.prefixe,
    this.exemple, {
    required this.principal,
  });

  final String iso;
  final String nom;
  final String indicatif;

  /// Nombres de chiffres possibles apres l'indicatif.
  final List<int> longueurs;

  /// Prefixe national (le 0 de 06 12 34 56 78 en France), retire du numero international.
  final String prefixe;
  final String exemple;

  /// Pays principal de son indicatif (+1 : Etats-Unis).
  final bool principal;

  int get longueurMax => longueurs.reduce((a, b) => a > b ? a : b);

  String get drapeau =>
      String.fromCharCodes(iso.codeUnits.map((c) => 0x1F1A5 + c));

  String get longueursTexte => longueurs.length == 1
      ? '${longueurs.first}'
      : '${longueurs.sublist(0, longueurs.length - 1).join(', ')} ou ${longueurs.last}';

  bool estComplet(String national) => longueurs.contains(national.length);

  /// Chiffres saisis -> numero national, sans le prefixe national en trop, limite a la longueur du pays.
  String numeroNational(String saisie) {
    var chiffres = saisie.replaceAll(RegExp(r'\D'), '');
    if (prefixe.isNotEmpty &&
        chiffres.startsWith(prefixe) &&
        longueurs.contains(chiffres.length - prefixe.length) &&
        !longueurs.contains(chiffres.length)) {
      chiffres = chiffres.substring(prefixe.length);
    }
    final max = longueurMax + prefixe.length;
    return chiffres.length > max ? chiffres.substring(0, max) : chiffres;
  }

  String international(String national) =>
      national.isEmpty ? '' : '+$indicatif$national';
}

const paysFrequents = [
  'SN',
  'CI',
  'ML',
  'BF',
  'GN',
  'BJ',
  'TG',
  'NE',
  'CM',
  'GA',
  'CG',
  'CD',
  'MA',
  'FR',
];
const paysDefaut = 'SN';

final Map<String, Pays> _parIso = {for (final p in listePays) p.iso: p};

Pays paysDe(String? iso) => _parIso[iso] ?? _parIso[paysDefaut]!;

String _sansAccents(String s) {
  const avec = 'àâäáãåçéèêëíìîïñóòôöõúùûüýÿœæ’';
  const sans = 'aaaaaaceeeeiiiinooooouuuuyyoe\'';
  final b = StringBuffer();
  for (final c in s.toLowerCase().split('')) {
    final i = avec.indexOf(c);
    b.write(i < 0 ? c : sans[i]);
  }
  return b.toString();
}

/// Recherche par nom ("senegal", "cote"), code ISO ("sn") ou indicatif ("221", "+221").
List<Pays> chercherPays(String texte) {
  final q = _sansAccents(texte.trim());
  if (q.isEmpty) {
    return [
      for (final iso in paysFrequents) _parIso[iso]!,
      for (final p in listePays)
        if (!paysFrequents.contains(p.iso)) p,
    ];
  }
  final chiffres = q.replaceFirst(RegExp(r'^(\+|00)'), '');
  if (RegExp(r'^\d+$').hasMatch(chiffres)) {
    return listePays.where((p) => p.indicatif.startsWith(chiffres)).toList()
      ..sort((a, b) {
        final l = a.indicatif.length.compareTo(b.indicatif.length);
        return l != 0 ? l : (b.principal ? 1 : 0) - (a.principal ? 1 : 0);
      });
  }
  final resultats = listePays
      .where((p) => _sansAccents(p.nom).contains(q) || p.iso.toLowerCase() == q)
      .toList();
  return [
    ...resultats.where((p) => _sansAccents(p.nom).startsWith(q)),
    ...resultats.where((p) => !_sansAccents(p.nom).startsWith(q)),
  ];
}

/// Pays d'un numero international (+221..., 221...) : les indicatifs ne sont jamais prefixes l'un de l'autre.
Pays? paysDuNumero(String numero) {
  final chiffres = numero.replaceAll(RegExp(r'\D'), '');
  for (var n = 3; n >= 1; n--) {
    if (chiffres.length < n) continue;
    final candidats = listePays
        .where((p) => p.indicatif == chiffres.substring(0, n))
        .toList();
    if (candidats.isNotEmpty)
      return candidats.firstWhere(
        (p) => p.principal,
        orElse: () => candidats.first,
      );
  }
  return null;
}
