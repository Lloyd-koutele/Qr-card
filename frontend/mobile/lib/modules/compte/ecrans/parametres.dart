import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';
import '../session.dart';

class EcranParametres extends ConsumerWidget {
  const EcranParametres({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profil = ref.watch(sessionProvider).profil;
    final mode = ref.watch(modeProvider);
    if (profil == null) return const Chargement();
    return Ecran(
      enfants: [
        const Titre('Paramètres'),
        Panneau(
          enfants: [
            Text('Compte', style: Theme.of(context).textTheme.titleMedium),
            Text(profil.nomAffiche),
            Text(
              profil.email,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            Text(
              profil.googleLie
                  ? 'Connexion Google liée'
                  : 'Connexion par mot de passe',
            ),
          ],
        ),
        Panneau(
          enfants: [
            Text('Affichage', style: Theme.of(context).textTheme.titleMedium),
            SegmentedButton<ThemeMode>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: ThemeMode.light, label: Text('Clair')),
                ButtonSegment(value: ThemeMode.dark, label: Text('Sombre')),
              ],
              selected: {mode},
              onSelectionChanged: (choix) =>
                  ref.read(modeProvider.notifier).choisir(choix.first),
            ),
          ],
        ),
        Panneau(
          enfants: [
            Text('Sécurité', style: Theme.of(context).textTheme.titleMedium),
            Text(
              profil.totpActif ? 'Double authentification active.' : 'Double authentification inactive : vous pouvez l\'activer depuis le site.',
            ),
            OutlinedButton(
              onPressed: () => showDialog<void>(
                context: context,
                builder: (_) =>
                    _ChangerMotDePasse(aUnMotDePasse: profil.aMotDePasse),
              ),
              child: Text(
                profil.aMotDePasse
                    ? 'Changer le mot de passe'
                    : 'Définir un mot de passe',
              ),
            ),
          ],
        ),
        OutlinedButton.icon(
          onPressed: () => ref.read(sessionProvider.notifier).deconnecter(),
          icon: const Icon(Icons.logout),
          label: const Text('Se déconnecter'),
        ),
      ],
    );
  }
}

class _ChangerMotDePasse extends ConsumerStatefulWidget {
  const _ChangerMotDePasse({required this.aUnMotDePasse});

  final bool aUnMotDePasse;

  @override
  ConsumerState<_ChangerMotDePasse> createState() => _EtatChanger();
}

class _EtatChanger extends ConsumerState<_ChangerMotDePasse> {
  final _ancien = TextEditingController();
  final _nouveau = TextEditingController();
  String _erreur = '';

  @override
  void dispose() {
    _ancien.dispose();
    _nouveau.dispose();
    super.dispose();
  }

  Future<void> _valider() async {
    if (_nouveau.text.length < 8)
      return setState(() => _erreur = 'Au moins 8 caractères');
    try {
      await ref
          .read(compteApiProvider)
          .changerMotDePasse(
            widget.aUnMotDePasse ? _ancien.text : null,
            _nouveau.text,
          );
      await ref.read(sessionProvider.notifier).rafraichir();
      if (!mounted) return;
      Navigator.pop(context);
      annoncer(context, 'Mot de passe enregistré');
    } catch (e) {
      setState(() => _erreur = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        widget.aUnMotDePasse
            ? 'Changer le mot de passe'
            : 'Définir un mot de passe',
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_erreur.isNotEmpty) Alerte(_erreur),
          if (widget.aUnMotDePasse)
            TextField(
              controller: _ancien,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Mot de passe actuel',
              ),
            ),
          TextField(
            controller: _nouveau,
            obscureText: true,
            decoration: const InputDecoration(
              labelText: 'Nouveau mot de passe',
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Annuler'),
        ),
        FilledButton(onPressed: _valider, child: const Text('Enregistrer')),
      ],
    );
  }
}
