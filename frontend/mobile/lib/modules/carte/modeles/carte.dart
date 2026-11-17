/// Ce que l'organisation impose sur une carte professionnelle.
class Marque {
  const Marque({
    this.id,
    required this.nom,
    required this.couleur,
    this.adresse,
    this.logo,
  });

  factory Marque.depuisJson(Map<String, dynamic> j) => Marque(
    id: j['id'] as String?,
    nom: j['nom'] as String,
    couleur: j['couleur'] as String,
    adresse: j['adresse'] as String?,
    logo: j['logo'] as String?,
  );

  /// Identifiant de l'organisation, pour ouvrir la bonne carte professionnelle.
  final String? id;
  final String nom;
  final String couleur;
  final String? adresse;

  /// Chemin du logo sur le serveur (/api/public/...), vide s'il n'y en a pas.
  final String? logo;
}

class ContactCarte {
  const ContactCarte({
    required this.type,
    required this.valeur,
    this.libelle,
    this.lien,
  });

  factory ContactCarte.depuisJson(Map<String, dynamic> j) => ContactCarte(
    type: j['type'] as String,
    valeur: j['valeur'] as String,
    libelle: j['libelle'] as String?,
    lien: j['lien'] as String?,
  );

  final String type;
  final String valeur;
  final String? libelle;

  /// Lien construit par le serveur (tel:, https://wa.me/...), jamais saisi librement.
  final String? lien;
}

class Carte {
  const Carte({
    required this.jeton,
    required this.numero,
    this.nom,
    this.prenom,
    this.profession,
    this.bio,
    this.couleur,
    this.photoVersion,
    this.contacts = const [],
    this.creeLe,
    this.proprietaire = false,
    this.enregistree = false,
    this.connecte = false,
    this.enregistrements,
    this.organisation,
  });

  factory Carte.depuisJson(Map<String, dynamic> j) => Carte(
    jeton: j['jeton'] as String,
    numero: j['numero'] as String,
    nom: j['nom'] as String?,
    prenom: j['prenom'] as String?,
    profession: j['profession'] as String?,
    bio: j['bio'] as String?,
    couleur: j['couleur'] as String?,
    photoVersion: (j['photoVersion'] as num?)?.toInt(),
    contacts: [
      for (final c in (j['contacts'] as List? ?? const []))
        ContactCarte.depuisJson(c as Map<String, dynamic>),
    ],
    creeLe: j['creeLe'] == null ? null : DateTime.parse(j['creeLe'] as String),
    proprietaire: j['proprietaire'] == true,
    enregistree: j['enregistree'] == true,
    connecte: j['connecte'] == true,
    enregistrements: (j['enregistrements'] as num?)?.toInt(),
    organisation: j['organisation'] == null
        ? null
        : Marque.depuisJson(j['organisation'] as Map<String, dynamic>),
  );

  final String jeton;
  final String numero;
  final String? nom;
  final String? prenom;
  final String? profession;
  final String? bio;

  /// Carte personnelle : couleur choisie (#rrggbb), vide pour le rouge MadeQrCard.
  final String? couleur;
  final int? photoVersion;
  final List<ContactCarte> contacts;
  final DateTime? creeLe;
  final bool proprietaire;
  final bool enregistree;
  final bool connecte;
  final int? enregistrements;
  final Marque? organisation;

  String get nomComplet =>
      [prenom, nom].where((s) => s != null && s.isNotEmpty).join(' ');

  String? get cheminPhoto => photoVersion == null
      ? null
      : '/api/public/cartes/$jeton/photo?v=$photoVersion';

  /// Apercu pendant la modification, avant enregistrement.
  Carte apercu({
    String? nom,
    String? prenom,
    String? profession,
    String? bio,
    String? couleur,
    List<ContactCarte>? contacts,
  }) => Carte(
    jeton: jeton,
    numero: numero,
    nom: nom,
    prenom: prenom,
    profession: profession,
    bio: bio,
    couleur: couleur ?? this.couleur,
    photoVersion: photoVersion,
    contacts: contacts ?? this.contacts,
    creeLe: creeLe,
    proprietaire: proprietaire,
    enregistrements: enregistrements,
    organisation: organisation,
  );
}
