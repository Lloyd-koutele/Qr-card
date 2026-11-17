import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/api.dart';
import '../../../commun/config.dart';
import '../../../commun/widgets.dart';
import '../../carte/donnees/carte_api.dart';
import '../../scan/lien_madeqrcard.dart';
import '../donnees/organisation_api.dart';
import '../modeles/organisation.dart';

class EcranOrganisation extends ConsumerWidget {
  const EcranOrganisation({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final organisations = ref.watch(organisationsProvider);
    return RefreshIndicator(
      onRefresh: () => ref.refresh(organisationsProvider.future),
      child: Ecran(
        enfants: [
          organisations.when(
            loading: () => const Chargement(),
            error: (e, _) => ErreurChargement(
              e,
              reessayer: () => ref.invalidate(organisationsProvider),
            ),
            data: (liste) => Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Titre(
                  'Organisations',
                  sousTitre: liste.isEmpty
                      ? "Vous n'appartenez à aucune organisation."
                      : 'Une carte professionnelle distincte dans chacune.',
                ),
                for (final o in liste) _Membre(organisation: o),
                const _Rejoindre(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Rejoindre extends StatelessWidget {
  const _Rejoindre();

  Future<void> _collerInvitation(BuildContext context) async {
    final texte = await demanderTexte(
      context,
      titre: "Lien d'invitation",
      action: 'Ouvrir',
      indice: 'Collez le lien reçu par email',
      clavier: TextInputType.url,
    );
    if (texte == null || !context.mounted) return;
    final lien = LienMadeQrCard.lire(texte);
    if (lien is LienInvitation) {
      context.push('/invitation/${lien.jeton}');
    } else {
      annoncer(context, "Ce lien n'est pas une invitation MadeQrCard");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Panneau(
          enfants: [
            Text(
              'Vous avez reçu une invitation ?',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const Text(
              "Collez le lien reçu par email. Votre compte et votre carte personnelle resteront privés.",
            ),
            FilledButton.icon(
              onPressed: () => _collerInvitation(context),
              icon: const Icon(Icons.mail_outline),
              label: const Text('Ouvrir une invitation'),
            ),
          ],
        ),
        Panneau(
          enfants: [
            Text(
              'Créer une organisation',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const Text(
              'La création et la gestion d\'une organisation (membres, invitations, logo) se font sur le site.',
            ),
            OutlinedButton.icon(
              onPressed: () => ouvrirLien('${Config.siteUrl}/organisations'),
              icon: const Icon(Icons.open_in_new),
              label: const Text('Ouvrir le site'),
            ),
          ],
        ),
      ],
    );
  }
}

class _Membre extends ConsumerWidget {
  const _Membre({required this.organisation});

  final Organisation organisation;

  Future<void> _quitter(BuildContext context, WidgetRef ref) async {
    final ok = await confirmer(
      context,
      'Quitter ${organisation.nom} ?',
      'Votre carte professionnelle et son QR code ne fonctionneront plus. Votre compte, votre carte personnelle et votre carnet ne changent pas.',
      action: 'Quitter',
      danger: true,
    );
    if (!ok) return;
    try {
      await ref.read(organisationApiProvider).quitter(organisation.id);
      ref.invalidate(organisationsProvider);
      ref.invalidate(cartesProProvider);
    } catch (e) {
      if (context.mounted) annoncer(context, '$e');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            if (organisation.logo != null) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: Container(
                  color: Colors.white,
                  width: 52,
                  height: 52,
                  child: Image.network(
                    Api.url(organisation.logo!),
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) => const SizedBox(),
                  ),
                ),
              ),
              const SizedBox(width: 12),
            ],
            Expanded(
              child: Titre(
                organisation.nom,
                sousTitre:
                    '${organisation.libelleType} · ${organisation.admin ? 'vous administrez cette organisation.' : 'vous êtes membre de cette organisation.'}',
              ),
            ),
          ],
        ),
        if (organisation.bloquee)
          Alerte(
            'Cette organisation est bloquée : ses cartes sont indisponibles.${organisation.motifBlocage != null ? ' Motif : ${organisation.motifBlocage}' : ''}',
          ),
        Panneau(
          enfants: [
            Text(
              "Ma carte d'organisation",
              style: Theme.of(context).textTheme.titleMedium,
            ),
            Text(
              "Distincte de votre carte personnelle. Adresse visible par l'organisation : ${organisation.monEmail}.",
            ),
            FilledButton(
              onPressed: () =>
                  context.go('/carte?organisation=${organisation.id}'),
              child: const Text('Voir ma carte'),
            ),
            OutlinedButton(
              onPressed: () => context.push(
                '/carte/modifier?organisation=${organisation.id}',
              ),
              child: const Text('Mettre à jour ma carte'),
            ),
          ],
        ),
        if (organisation.admin)
          Panneau(
            enfants: [
              Text(
                'Administration',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const Text(
                'Les membres, les invitations, le logo et le journal se gèrent sur le site.',
              ),
              OutlinedButton.icon(
                onPressed: () => ouvrirLien(
                  '${Config.siteUrl}/organisations/${organisation.id}',
                ),
                icon: const Icon(Icons.open_in_new),
                label: const Text('Ouvrir le site'),
              ),
            ],
          ),
        TextButton(
          onPressed: () => _quitter(context, ref),
          style: TextButton.styleFrom(
            foregroundColor: Theme.of(context).colorScheme.error,
          ),
          child: const Text("Quitter l'organisation"),
        ),
      ],
    );
  }
}
