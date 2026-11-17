import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../commun/config.dart';
import '../commun/widgets.dart';
import '../modules/carte/donnees/carte_api.dart';
import '../modules/compte/session.dart';

/// Pendant la lecture de la session, ou si le serveur est injoignable.
class EcranDemarrage extends ConsumerWidget {
  const EcranDemarrage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final erreur = ref.watch(sessionProvider).erreur;
    return Scaffold(
      body: erreur == null
          ? const Chargement()
          : ErreurChargement(
              erreur,
              reessayer: () => ref.read(sessionProvider.notifier).demarrer(),
            ),
    );
  }
}

/// Les administrateurs generaux, et les comptes qui doivent d'abord securiser leur acces, passent par le site.
class EcranSite extends ConsumerWidget {
  const EcranSite({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profil = ref.watch(sessionProvider).profil;
    final texte = profil?.superAdmin == true
        ? "L'administration se fait sur le site. L'application mobile est réservée à l'usage personnel."
        : 'Changez votre mot de passe et activez la double authentification sur le site avant de continuer.';
    return Scaffold(
      body: Ecran(
        largeur: 420,
        enfants: [
          const SizedBox(height: 48),
          const Titre('Continuez sur le site'),
          Alerte(texte, type: TypeAlerte.info),
          FilledButton.icon(
            onPressed: () => ouvrirLien(Config.siteUrl),
            icon: const Icon(Icons.open_in_new),
            label: const Text('Ouvrir le site'),
          ),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => ref.read(sessionProvider.notifier).deconnecter(),
            child: const Text('Se déconnecter'),
          ),
        ],
      ),
    );
  }
}
