class Profil {
  const Profil({
    required this.id,
    required this.email,
    required this.nomAffiche,
    required this.role,
    required this.totpActif,
    required this.aMotDePasse,
    required this.googleLie,
    required this.changerMotDePasse,
    required this.securiteRequise,
  });

  factory Profil.depuisJson(Map<String, dynamic> j) => Profil(
    id: j['id'] as String,
    email: j['email'] as String,
    nomAffiche: j['nomAffiche'] as String,
    role: j['role'] as String,
    totpActif: j['totpActif'] == true,
    aMotDePasse: j['aMotDePasse'] == true,
    googleLie: j['googleLie'] == true,
    changerMotDePasse: j['changerMotDePasse'] == true,
    securiteRequise: j['securiteRequise'] == true,
  );

  final String id;
  final String email;
  final String nomAffiche;
  final String role;
  final bool totpActif;
  final bool aMotDePasse;
  final bool googleLie;
  final bool changerMotDePasse;
  final bool securiteRequise;

  bool get superAdmin => role == 'SUPER_ADMIN';
}

/// Soit token (connecte), soit mfaToken (code de double authentification attendu).
class ResultatConnexion {
  const ResultatConnexion({this.token, this.mfaToken, this.mfaRequis = false});

  factory ResultatConnexion.depuisJson(Map<String, dynamic> j) =>
      ResultatConnexion(
        token: j['token'] as String?,
        mfaToken: j['mfaToken'] as String?,
        mfaRequis: j['mfaRequis'] == true,
      );

  final String? token;
  final String? mfaToken;
  final bool mfaRequis;
}

/// Inscription en attente du code a 6 chiffres recu par email.
class InscriptionAttente {
  const InscriptionAttente({
    required this.jeton,
    required this.email,
    required this.renvoiDansSecondes,
  });

  factory InscriptionAttente.depuisJson(Map<String, dynamic> j) =>
      InscriptionAttente(
        jeton: j['jeton'] as String,
        email: j['email'] as String,
        renvoiDansSecondes: (j['renvoiDansSecondes'] as num).toInt(),
      );

  final String jeton;
  final String email;
  final int renvoiDansSecondes;
}
