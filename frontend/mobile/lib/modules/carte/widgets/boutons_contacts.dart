import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

import '../../../commun/widgets.dart';
import '../donnees/carte_api.dart';
import '../modeles/carte.dart';
import '../modeles/types_contact.dart';

/// Un gros bouton par contact : il ouvre directement l'application concernee.
class BoutonsContacts extends StatelessWidget {
  const BoutonsContacts({super.key, required this.contacts});

  final List<ContactCarte> contacts;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        for (final c in contacts)
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 20),
              alignment: Alignment.centerLeft,
            ),
            onPressed: c.lien == null
                ? null
                : () async {
                    if (!await ouvrirLien(c.lien!) && context.mounted) {
                      annoncer(
                        context,
                        "Impossible d'ouvrir ${infoContact(c.type).libelle} sur cet appareil",
                      );
                    }
                  },
            icon: FaIcon(infoContact(c.type).icone, size: 18),
            label: Text(infoContact(c.type).texteBouton),
          ),
      ],
    );
  }
}
