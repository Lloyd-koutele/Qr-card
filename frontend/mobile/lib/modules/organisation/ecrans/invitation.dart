import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/api.dart';
import '../../../commun/widgets.dart';
import '../../carte/donnees/carte_api.dart';
import '../../compte/session.dart';
import '../donnees/organisation_api.dart';
import '../modeles/organisation.dart';

final _invitationProvider = FutureProvider.autoDispose
    .family<Invitation, String>(
      (ref, jeton) => ref.watch(organisationApiProvider).invitation(jeton),
    );

/// Reponse a une invitation, avec le compte deja connecte dans l'application.
class EcranInvitation extends ConsumerStatefulWidget {
  const EcranInvitation({super.key, required this.jeton});

  final String jeton;

  @override
  ConsumerState<EcranInvitation> createState() => _EtatInvitation();
}

class _EtatInvitation extends ConsumerState<EcranInvitation> {
  bool _envoi = false;
  String _erreur = '';

  Future<void> _agir(Future<void> Function() action, String message) async {
    setState(() {
      _envoi = true;
      _erreur = '';
    });
    try {
      await action();
      ref.invalidate(organisationsProvider);
      ref.invalidate(cartesProProvider);
      if (!mounted) return;
      annoncer(context, message);
      context.go('/organisations');
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final invitation = ref.watch(_invitationProvider(widget.jeton));
    final email = ref.watch(sessionProvider).profil?.email;
    final api = ref.read(organisationApiProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Invitation')),
      body: invitation.when(
        loading: () => const Chargement(),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text('$e', textAlign: TextAlign.center),
          ),
        ),
        data: (i) => Ecran(
          enfants: [
            Row(
              children: [
                if (i.logo != null) ...[
                  Image.network(
                    Api.url(i.logo!),
                    width: 52,
                    height: 52,
                    errorBuilder: (_, _, _) => const SizedBox(),
                  ),
                  const SizedBox(width: 12),
                ],
                Expanded(
                  child: Titre(
                    i.organisation,
                    sousTitre:
                        'vous invite à créer votre carte professionnelle.',
                  ),
                ),
              ],
            ),
            if (_erreur.isNotEmpty) Alerte(_erreur),
            Panneau(
              enfants: [
                Text(
                  'Invitation envoyée à ${i.email}${i.poste != null ? ' pour le poste de ${i.poste}' : ''}.',
                ),
                const Text(
                  "• Rien n'est rattaché à l'organisation tant que vous n'avez pas accepté.\n"
                  "• L'organisation voit cette adresse et votre carte professionnelle.\n"
                  "• Elle ne voit ni l'adresse de votre compte, ni votre carte personnelle, ni votre carnet.\n"
                  '• Si vous partez, seule la carte professionnelle disparaît.',
                ),
                if (email != null)
                  Text(
                    'Vous êtes connecté avec $email.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                FilledButton(
                  onPressed: _envoi
                      ? null
                      : () => _agir(
                          () => api.accepter(widget.jeton),
                          'Bienvenue chez ${i.organisation}',
                        ),
                  child: const Text("Accepter l'invitation"),
                ),
                OutlinedButton(
                  onPressed: _envoi
                      ? null
                      : () => _agir(
                          () => api.refuser(widget.jeton),
                          'Invitation refusée',
                        ),
                  child: const Text('Refuser'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
