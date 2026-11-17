/// Libelles des types d'organisation, dans l'ordre du formulaire du site.
const typesOrganisation = {
  'ENTREPRISE': 'Entreprise',
  'ASSOCIATION': 'Association ou ONG',
  'ECOLE': 'École ou université',
  'ADMINISTRATION': 'Administration',
  'AUTRE': 'Autre',
};

class Organisation {
  const Organisation({
    required this.id,
    required this.nom,
    required this.type,
    required this.couleur,
    required this.statut,
    required this.monRole,
    required this.monEmail,
    this.typePrecision,
    this.adresse,
    this.logo,
    this.motifBlocage,
  });

  factory Organisation.depuisJson(Map<String, dynamic> j) => Organisation(
    id: j['id'] as String,
    nom: j['nom'] as String,
    type: j['type'] as String? ?? 'ENTREPRISE',
    couleur: j['couleur'] as String,
    statut: j['statut'] as String,
    monRole: j['monRole'] as String,
    monEmail: j['monEmail'] as String,
    typePrecision: j['typePrecision'] as String?,
    adresse: j['adresse'] as String?,
    logo: j['logo'] as String?,
    motifBlocage: j['motifBlocage'] as String?,
  );

  final String id;
  final String nom;

  /// ENTREPRISE, ASSOCIATION, ECOLE, ADMINISTRATION ou AUTRE.
  final String type;

  /// Saisie libre pour le type AUTRE, facultative.
  final String? typePrecision;
  final String couleur;
  final String statut;
  final String monRole;

  /// L'adresse invitee : la seule que l'organisation connait.
  final String monEmail;
  final String? adresse;
  final String? logo;
  final String? motifBlocage;

  bool get admin => monRole == 'ADMIN';
  bool get bloquee => statut != 'ACTIVE';

  String get libelleType => type == 'AUTRE' && (typePrecision ?? '').isNotEmpty
      ? typePrecision!
      : typesOrganisation[type] ?? type;
}

class Invitation {
  const Invitation({
    required this.organisation,
    required this.couleur,
    required this.email,
    this.logo,
    this.nom,
    this.poste,
  });

  factory Invitation.depuisJson(Map<String, dynamic> j) => Invitation(
    organisation: j['organisation'] as String,
    couleur: j['couleur'] as String,
    email: j['email'] as String,
    logo: j['logo'] as String?,
    nom: j['nom'] as String?,
    poste: j['poste'] as String?,
  );

  final String organisation;
  final String couleur;
  final String email;
  final String? logo;
  final String? nom;
  final String? poste;
}
