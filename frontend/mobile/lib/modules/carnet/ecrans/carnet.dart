import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/widgets.dart';
import '../../carte/widgets/carte_identite.dart';
import '../donnees/carnet_api.dart';
import '../modeles/element_carnet.dart';

class EcranCarnet extends ConsumerWidget {
  const EcranCarnet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carnet = ref.watch(carnetProvider);
    return RefreshIndicator(
      onRefresh: () => ref.refresh(carnetProvider.future),
      child: Ecran(
        enfants: [
          const Titre(
            'Carnet',
            sousTitre: 'Les cartes que vous avez gardées, toujours à jour et en lecture seule.',
          ),
          carnet.when(
            loading: () => const Chargement(),
            error: (e, _) => ErreurChargement(
              e,
              reessayer: () => ref.invalidate(carnetProvider),
            ),
            data: (elements) => elements.isEmpty
                ? const Alerte(
                    'Votre carnet est vide. Scannez la carte de quelqu\'un pour l\'y ajouter.',
                    type: TypeAlerte.info,
                  )
                : Column(
                    children: [for (final e in elements) _Element(element: e)],
                  ),
          ),
        ],
      ),
    );
  }
}

class _Element extends ConsumerWidget {
  const _Element({required this.element});

  final ElementCarnet element;

  Future<void> _note(BuildContext context, WidgetRef ref) async {
    final note = await demanderTexte(
      context,
      titre: 'Note personnelle',
      action: 'Enregistrer',
      initial: element.note ?? '',
      longueurMax: 120,
    );
    if (note == null) return;
    try {
      await ref.read(carnetApiProvider).modifierNote(element.id, note);
      ref.invalidate(carnetProvider);
    } catch (e) {
      if (context.mounted) annoncer(context, '$e');
    }
  }

  Future<void> _retirer(BuildContext context, WidgetRef ref) async {
    if (!await confirmer(
      context,
      'Retirer cette carte ?',
      'Elle disparaîtra de votre carnet.',
      action: 'Retirer',
      danger: true,
    ))
      return;
    try {
      await ref.read(carnetApiProvider).retirer(element.id);
      ref.invalidate(carnetProvider);
    } catch (e) {
      if (context.mounted) annoncer(context, '$e');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carte = element.carte;
    return Panneau(
      enfants: [
        if (carte != null)
          GestureDetector(
            onTap: () => context.push('/c/${carte.jeton}'),
            child: AbsorbPointer(
              child: CarteIdentite(
                carte: carte,
                sansQr: true,
                retournable: false,
              ),
            ),
          )
        else
          const Alerte(
            'Carte indisponible : son propriétaire l\'a retirée ou son compte est suspendu.',
            type: TypeAlerte.info,
          ),
        Row(
          children: [
            Expanded(
              child: Text(
                element.note?.isNotEmpty == true
                    ? element.note!
                    : 'Ajoutée le ${dateFr(element.enregistreeLe)}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            IconButton(
              tooltip: 'Modifier la note',
              onPressed: () => _note(context, ref),
              icon: const Icon(Icons.edit_note),
            ),
            IconButton(
              tooltip: 'Retirer du carnet',
              onPressed: () => _retirer(context, ref),
              icon: const Icon(Icons.delete_outline),
            ),
          ],
        ),
      ],
    );
  }
}
