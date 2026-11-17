import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class TypeContact {
  const TypeContact(
    this.type,
    this.libelle,
    this.icone,
    this.exemple, [
    this.action,
  ]);

  final String type;
  final String libelle;
  final FaIconData icone;
  final String exemple;

  /// Texte du bouton s'il differe du libelle.
  final String? action;

  String get texteBouton => action ?? libelle;
}

/// Meme liste que le site et le serveur (TypeContact).
const typesContact = [
  TypeContact(
    'TELEPHONE',
    'Téléphone',
    FontAwesomeIcons.phone,
    '771234567',
    'Appeler',
  ),
  TypeContact('WHATSAPP', 'WhatsApp', FontAwesomeIcons.whatsapp, '771234567'),
  TypeContact('TELEGRAM', 'Telegram', FontAwesomeIcons.telegram, 'pseudo'),
  TypeContact('INSTAGRAM', 'Instagram', FontAwesomeIcons.instagram, 'pseudo'),
  TypeContact('FACEBOOK', 'Facebook', FontAwesomeIcons.facebook, 'pseudo'),
  TypeContact('TIKTOK', 'TikTok', FontAwesomeIcons.tiktok, 'pseudo'),
  TypeContact('LINKEDIN', 'LinkedIn', FontAwesomeIcons.linkedin, 'pseudo'),
  TypeContact('SNAPCHAT', 'Snapchat', FontAwesomeIcons.snapchat, 'pseudo'),
  TypeContact('X', 'X', FontAwesomeIcons.xTwitter, 'pseudo'),
  TypeContact('EMAIL', 'Email', FontAwesomeIcons.envelope, 'nom@exemple.com'),
  TypeContact('SITE_WEB', 'Site web', FontAwesomeIcons.globe, 'monsite.com'),
];

TypeContact infoContact(String type) => typesContact.firstWhere(
  (t) => t.type == type,
  orElse: () => typesContact.first,
);
