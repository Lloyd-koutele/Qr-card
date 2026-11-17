/// Couleurs proposees pour une carte personnelle, les memes que sur le site.
/// Valeur vide : rouge MadeQrCard, la couleur par defaut.
class CouleurProposee {
  const CouleurProposee(this.valeur, this.nom, {this.apercu});

  final String valeur;
  final String nom;
  final String? apercu;

  String get hex => apercu ?? valeur;
}

const couleursCarte = [
  CouleurProposee('', 'Rouge MadeQrCard', apercu: '#c8102e'),
  CouleurProposee('#1f3a93', 'Bleu nuit'),
  CouleurProposee('#0f6e6e', 'Turquoise'),
  CouleurProposee('#1e6b3a', 'Vert forêt'),
  CouleurProposee('#5b2a86', 'Violet'),
  CouleurProposee('#a3214f', 'Framboise'),
  CouleurProposee('#b4461b', 'Orange brûlé'),
  CouleurProposee('#8a6a12', 'Or'),
  CouleurProposee('#3a3a3a', 'Anthracite'),
];
