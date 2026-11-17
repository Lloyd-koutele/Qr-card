import 'dart:math';

import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../commun/api.dart';
import '../../../commun/config.dart';
import '../../../commun/widgets.dart';
import '../donnees/carte_api.dart';
import '../modeles/carte.dart';
import '../modeles/types_contact.dart';

const _contactsAuVerso = 8;

/// Teintes d'une carte : couleur choisie pour une carte personnelle (rouge par defaut),
/// couleur de l'organisation pour une carte professionnelle.
class CouleursCarte {
  const CouleursCarte(
    this.principale,
    this.claire,
    this.bord,
    this.foncee,
    this.moyenne, {
    this.surPrincipale = Colors.white,
  });

  static const rouge = CouleursCarte(
    Color(0xFFC8102E),
    Color(0xFFFDECEE),
    Color(0xFFF3BCC4),
    Color(0xFF5C0A16),
    Color(0xFFA8303F),
  );

  factory CouleursCarte.pour(Marque? marque) =>
      CouleursCarte.depuisHex(marque?.couleur);

  /// Professionnelle : couleur de l'organisation. Personnelle : couleur choisie, sinon rouge.
  factory CouleursCarte.deCarte(Carte carte) =>
      CouleursCarte.depuisHex(carte.organisation?.couleur ?? carte.couleur);

  factory CouleursCarte.depuisHex(String? hex) {
    if (hex == null || hex.isEmpty) return rouge;
    final c = couleurHex(hex);
    return CouleursCarte(
      c,
      Color.lerp(Colors.white, c, 0.08)!,
      Color.lerp(Colors.white, c, 0.28)!,
      Color.lerp(Colors.black, c, 0.45)!,
      Color.lerp(Colors.black, c, 0.80)!,
      surPrincipale: texteSur(c),
    );
  }

  final Color principale;
  final Color claire;
  final Color bord;
  final Color foncee;
  final Color moyenne;

  /// Texte du bandeau : blanc, ou fonce sur une couleur claire.
  final Color surPrincipale;
}

const _texteFonce = Color(0xFF1D1D1B);

/// Blanc ou fonce, celui qui contraste le plus avec la couleur (calcul WCAG, comme sur le site).
Color texteSur(Color fond) {
  final l = fond.computeLuminance();
  final blanc = 1.05 / (l + 0.05);
  final fonce = (l + 0.05) / (_texteFonce.computeLuminance() + 0.05);
  return blanc >= fonce ? Colors.white : _texteFonce;
}

Color couleurHex(String hex) =>
    Color(int.parse('FF${hex.replaceFirst('#', '')}', radix: 16));

String ligneMrz(Carte carte) {
  String propre(String? s) => (s ?? '')
      .replaceAll(RegExp('[àâä]'), 'a')
      .replaceAll(RegExp('[éèêë]'), 'e')
      .replaceAll(RegExp('[îï]'), 'i')
      .replaceAll(RegExp('[ôö]'), 'o')
      .replaceAll(RegExp('[ùûü]'), 'u')
      .replaceAll('ç', 'c')
      .replaceAll(RegExp('[^A-Za-z]'), '')
      .toUpperCase();
  final numero = carte.numero.replaceAll(RegExp(r'\D'), '');
  return 'QRC<<${propre(carte.nom)}<<${propre(carte.prenom)}'.padRight(
        30,
        '<',
      ) +
      numero;
}

/// Carte au format carte d'identite (85,6 x 54 mm). Toucher la carte la retourne.
class CarteIdentite extends StatefulWidget {
  const CarteIdentite({
    super.key,
    required this.carte,
    this.sansQr = false,
    this.retournable = true,
  });

  final Carte carte;
  final bool sansQr;
  final bool retournable;

  @override
  State<CarteIdentite> createState() => _EtatCarteIdentite();
}

class _EtatCarteIdentite extends State<CarteIdentite> {
  bool _retournee = false;

  @override
  Widget build(BuildContext context) {
    final couleurs = CouleursCarte.deCarte(widget.carte);
    return GestureDetector(
      onTap: widget.retournable
          ? () => setState(() => _retournee = !_retournee)
          : null,
      child: AspectRatio(
        aspectRatio: 85.6 / 54,
        child: TweenAnimationBuilder<double>(
          tween: Tween(end: _retournee ? pi : 0),
          duration: const Duration(milliseconds: 500),
          curve: Curves.easeInOut,
          builder: (context, angle, _) {
            final verso = angle > pi / 2;
            return Transform(
              alignment: Alignment.center,
              transform: Matrix4.identity()
                ..setEntry(3, 2, 0.0012)
                ..rotateY(angle),
              child: verso
                  ? Transform(
                      alignment: Alignment.center,
                      transform: Matrix4.rotationY(pi),
                      child: _Verso(carte: widget.carte, couleurs: couleurs),
                    )
                  : _Recto(
                      carte: widget.carte,
                      couleurs: couleurs,
                      sansQr: widget.sansQr,
                    ),
            );
          },
        ),
      ),
    );
  }
}

class _Face extends StatelessWidget {
  const _Face({required this.couleurs, required this.enfant});

  final CouleursCarte couleurs;
  final Widget Function(double u) enfant;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, contraintes) {
        final u = contraintes.maxWidth / 100;
        return Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(3 * u),
            border: Border.all(color: couleurs.bord),
            boxShadow: const [
              BoxShadow(
                color: Color(0x14000000),
                blurRadius: 24,
                offset: Offset(0, 8),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(3 * u),
            child: DefaultTextStyle(
              style: TextStyle(color: couleurs.foncee, height: 1.25),
              child: enfant(u),
            ),
          ),
        );
      },
    );
  }
}

Widget _bandeau(double u, CouleursCarte c, Widget gauche, Widget droite) {
  return Column(
    children: [
      Container(
        color: c.principale,
        padding: EdgeInsets.symmetric(horizontal: 3 * u, vertical: 1.7 * u),
        child: DefaultTextStyle(
          style: TextStyle(color: c.surPrincipale, fontSize: 2.4 * u),
          child: Row(
            children: [
              Expanded(child: gauche),
              SizedBox(width: 2 * u),
              droite,
            ],
          ),
        ),
      ),
      Container(height: 0.6 * u, color: c.bord),
    ],
  );
}

class _Recto extends StatelessWidget {
  const _Recto({
    required this.carte,
    required this.couleurs,
    required this.sansQr,
  });

  final Carte carte;
  final CouleursCarte couleurs;
  final bool sansQr;

  @override
  Widget build(BuildContext context) {
    final organisation = carte.organisation;
    final photo = carte.cheminPhoto;
    return _Face(
      couleurs: couleurs,
      enfant: (u) => Stack(
        children: [
          Positioned(
            right: -10 * u,
            bottom: -16 * u,
            child: _Cercle(
              taille: 36 * u,
              epaisseur: 2.4 * u,
              couleur: couleurs.claire,
            ),
          ),
          Positioned(
            right: -3 * u,
            bottom: -9 * u,
            child: _Cercle(
              taille: 20 * u,
              epaisseur: 1.6 * u,
              couleur: couleurs.claire,
            ),
          ),
          Column(
            children: [
              _bandeau(
                u,
                couleurs,
                Row(
                  children: [
                    if (organisation?.logo != null) ...[
                      Container(
                        color: Colors.white,
                        height: 3.6 * u,
                        constraints: BoxConstraints(maxWidth: 9 * u),
                        child: Image.network(
                          Api.url(organisation!.logo!),
                          fit: BoxFit.contain,
                          errorBuilder: (_, _, _) => const SizedBox(),
                        ),
                      ),
                      SizedBox(width: 1.2 * u),
                    ],
                    Flexible(
                      child: Text(
                        organisation?.nom ?? 'MadeQrCard',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.6,
                        ),
                      ),
                    ),
                  ],
                ),
                Text(
                  organisation == null
                      ? 'Carte personnelle'
                      : 'Carte professionnelle',
                ),
              ),
              Expanded(
                child: Padding(
                  padding: EdgeInsets.fromLTRB(3 * u, 3 * u, 3 * u, 7 * u),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 22 * u,
                        height: 22 * u * 4 / 3,
                        child: Container(
                          decoration: BoxDecoration(
                            color: couleurs.claire,
                            border: Border.all(color: couleurs.bord),
                            borderRadius: BorderRadius.circular(1.5 * u),
                          ),
                          clipBehavior: Clip.antiAlias,
                          child: photo == null
                              ? Icon(
                                  Icons.person_outline,
                                  size: 10 * u,
                                  color: couleurs.moyenne,
                                )
                              : Image.network(
                                  Api.url(photo),
                                  fit: BoxFit.cover,
                                  errorBuilder: (_, _, _) => Icon(
                                    Icons.person_outline,
                                    size: 10 * u,
                                    color: couleurs.moyenne,
                                  ),
                                ),
                        ),
                      ),
                      SizedBox(width: 3 * u),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _Champ(
                              u: u,
                              couleurs: couleurs,
                              libelle: 'Nom',
                              valeur: carte.nom,
                            ),
                            _Champ(
                              u: u,
                              couleurs: couleurs,
                              libelle: 'Prénom',
                              valeur: carte.prenom,
                            ),
                            _Champ(
                              u: u,
                              couleurs: couleurs,
                              libelle: organisation == null
                                  ? 'Profession'
                                  : 'Poste',
                              valeur: carte.profession,
                            ),
                            _Champ(
                              u: u,
                              couleurs: couleurs,
                              libelle: 'N° de carte',
                              valeur: carte.numero,
                              mono: true,
                            ),
                          ],
                        ),
                      ),
                      if (!sansQr) ...[
                        SizedBox(width: 2 * u),
                        SizedBox(
                          width: 19 * u,
                          child: Column(
                            children: [
                              Container(
                                padding: EdgeInsets.all(0.8 * u),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  border: Border.all(color: couleurs.bord),
                                  borderRadius: BorderRadius.circular(1 * u),
                                ),
                                child: QrImageView(
                                  data: Config.lienCarte(carte.jeton),
                                  padding: EdgeInsets.zero,
                                  backgroundColor: Colors.white,
                                  eyeStyle: QrEyeStyle(
                                    eyeShape: QrEyeShape.square,
                                    color: organisation == null
                                        ? couleurs.foncee
                                        : const Color(0xFF111111),
                                  ),
                                  dataModuleStyle: QrDataModuleStyle(
                                    dataModuleShape: QrDataModuleShape.square,
                                    color: organisation == null
                                        ? couleurs.foncee
                                        : const Color(0xFF111111),
                                  ),
                                ),
                              ),
                              SizedBox(height: 0.8 * u),
                              Text(
                                'Scannez-moi',
                                style: TextStyle(
                                  fontSize: 1.8 * u,
                                  color: couleurs.moyenne,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          ),
          Positioned(
            left: 3 * u,
            right: 24 * u,
            bottom: 2.2 * u,
            child: Text(
              'Émise le ${dateFr(carte.creeLe)}${organisation?.adresse != null ? ' · ${organisation!.adresse}' : ''}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 2.1 * u, color: couleurs.moyenne),
            ),
          ),
        ],
      ),
    );
  }
}

class _Verso extends StatelessWidget {
  const _Verso({required this.carte, required this.couleurs});

  final Carte carte;
  final CouleursCarte couleurs;

  @override
  Widget build(BuildContext context) {
    final contacts = carte.contacts.take(_contactsAuVerso).toList();
    return _Face(
      couleurs: couleurs,
      enfant: (u) => Column(
        children: [
          _bandeau(
            u,
            couleurs,
            const Text(
              'Mes contacts',
              style: TextStyle(fontWeight: FontWeight.w700, letterSpacing: 0.6),
            ),
            Row(
              children: [
                Icon(
                  Icons.visibility_outlined,
                  size: 2.6 * u,
                  color: couleurs.surPrincipale,
                ),
                SizedBox(width: 0.8 * u),
                const Text('Lecture seule'),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: EdgeInsets.all(3 * u),
              child: contacts.isEmpty
                  ? Center(
                      child: Text(
                        "Aucun contact pour l'instant",
                        style: TextStyle(
                          fontSize: 2.6 * u,
                          color: couleurs.moyenne,
                        ),
                      ),
                    )
                  : GridView.count(
                      crossAxisCount: 4,
                      mainAxisSpacing: 1.5 * u,
                      crossAxisSpacing: 1.5 * u,
                      childAspectRatio: 2.3,
                      physics: const NeverScrollableScrollPhysics(),
                      padding: EdgeInsets.zero,
                      children: [
                        for (final c in contacts)
                          _MiniBouton(u: u, couleurs: couleurs, contact: c),
                      ],
                    ),
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(3 * u, 0, 3 * u, 1.6 * u),
            child: Text(
              ligneMrz(carte),
              maxLines: 1,
              overflow: TextOverflow.clip,
              style: TextStyle(
                fontFamily: 'Courier',
                fontSize: 2.6 * u,
                letterSpacing: 0.3 * u,
                color: couleurs.moyenne,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MiniBouton extends StatelessWidget {
  const _MiniBouton({
    required this.u,
    required this.couleurs,
    required this.contact,
  });

  final double u;
  final CouleursCarte couleurs;
  final ContactCarte contact;

  @override
  Widget build(BuildContext context) {
    final info = infoContact(contact.type);
    return Material(
      color: couleurs.claire,
      borderRadius: BorderRadius.circular(1.5 * u),
      child: InkWell(
        borderRadius: BorderRadius.circular(1.5 * u),
        onTap: contact.lien == null ? null : () => ouvrirLien(contact.lien!),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            FaIcon(info.icone, size: 2.8 * u, color: couleurs.principale),
            SizedBox(width: 1 * u),
            Flexible(
              child: Text(
                info.texteBouton,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 2 * u, fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Champ extends StatelessWidget {
  const _Champ({
    required this.u,
    required this.couleurs,
    required this.libelle,
    this.valeur,
    this.mono = false,
  });

  final double u;
  final CouleursCarte couleurs;
  final String libelle;
  final String? valeur;
  final bool mono;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: 0.8 * u),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            libelle,
            style: TextStyle(fontSize: 1.9 * u, color: couleurs.moyenne),
          ),
          Text(
            valeur == null || valeur!.isEmpty ? '—' : valeur!,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 3 * u,
              fontWeight: FontWeight.w700,
              fontFamily: mono ? 'Courier' : null,
            ),
          ),
        ],
      ),
    );
  }
}

class _Cercle extends StatelessWidget {
  const _Cercle({
    required this.taille,
    required this.epaisseur,
    required this.couleur,
  });

  final double taille;
  final double epaisseur;
  final Color couleur;

  @override
  Widget build(BuildContext context) => Container(
    width: taille,
    height: taille,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      border: Border.all(color: couleur, width: epaisseur),
    ),
  );
}

/// QR code seul, pour l'affichage en grand.
class QrPlein extends StatelessWidget {
  const QrPlein({super.key, required this.lien, required this.couleur});

  final String lien;
  final Color couleur;

  @override
  Widget build(BuildContext context) => QrImageView(
    data: lien,
    backgroundColor: Colors.white,
    eyeStyle: QrEyeStyle(eyeShape: QrEyeShape.square, color: couleur),
    dataModuleStyle: QrDataModuleStyle(
      dataModuleShape: QrDataModuleShape.square,
      color: couleur,
    ),
  );
}
