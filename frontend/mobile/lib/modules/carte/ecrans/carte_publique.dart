import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/api.dart';
import '../../../commun/widgets.dart';
import '../donnees/carte_api.dart';
import '../modeles/carte.dart';
import '../widgets/boutons_contacts.dart';
import '../widgets/carte_identite.dart';

/// Carte d'une autre personne, ouverte par un scan ou depuis le carnet : lecture seule.
class EcranCartePublique extends ConsumerWidget {
  const EcranCartePublique({super.key, required this.jeton});

  final String jeton;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carte = ref.watch(cartePubliqueProvider(jeton));
    return Scaffold(
      appBar: AppBar(),
      body: carte.when(
        loading: () => const Chargement(),
        error: (e, _) => e is ErreurApi && e.statut == 404
            ? const Center(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: Text(
                    "Carte indisponible : ce lien n'existe pas ou n'est plus actif.",
                    textAlign: TextAlign.center,
                  ),
                ),
              )
            : ErreurChargement(
                e,
                reessayer: () => ref.invalidate(cartePubliqueProvider(jeton)),
              ),
        data: (c) => _Contenu(carte: c),
      ),
    );
  }
}

class _Contenu extends ConsumerStatefulWidget {
  const _Contenu({required this.carte});

  final Carte carte;

  @override
  ConsumerState<_Contenu> createState() => _EtatContenu();
}

class _EtatContenu extends ConsumerState<_Contenu> {
  final _note = TextEditingController();
  bool _envoi = false;

  @override
  void dispose() {
    _note.dispose();
    super.dispose();
  }

  Future<void> _enregistrer() async {
    setState(() => _envoi = true);
    try {
      await ref.read(apiProvider).post('/api/carnet', {
        'jeton': widget.carte.jeton,
        'note': _note.text,
      });
      ref.invalidate(cartePubliqueProvider(widget.carte.jeton));
      if (mounted) annoncer(context, 'Carte ajoutée à votre carnet');
    } catch (e) {
      if (mounted) annoncer(context, '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _vcard() async {
    try {
      await ref.read(carteApiProvider).partagerVcard(widget.carte);
    } catch (e) {
      if (mounted) annoncer(context, '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.carte;
    final theme = Theme.of(context);
    final sousTitre = [
      c.profession,
      c.organisation?.nom,
    ].where((s) => s != null && s.isNotEmpty).join(' · ');
    return Ecran(
      enfants: [
        CarteIdentite(carte: c),
        const SizedBox(height: 16),
        Text(
          c.nomComplet.isEmpty
              ? (c.organisation == null
                    ? 'Carte personnelle'
                    : 'Carte professionnelle')
              : c.nomComplet,
          style: theme.textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        if (sousTitre.isNotEmpty)
          Text(
            sousTitre,
            style: theme.textTheme.titleSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        if (c.organisation?.adresse != null)
          Text(c.organisation!.adresse!, style: theme.textTheme.bodySmall),
        if (c.bio != null)
          Padding(padding: const EdgeInsets.only(top: 8), child: Text(c.bio!)),
        const SizedBox(height: 16),
        BoutonsContacts(contacts: c.contacts),
        const SizedBox(height: 8),
        FilledButton.icon(
          onPressed: _vcard,
          icon: const Icon(Icons.contacts_outlined),
          label: const Text('Ajouter aux contacts du téléphone'),
        ),
        const SizedBox(height: 16),
        Panneau(
          enfants: [
            if (c.proprietaire) ...[
              const Text(
                "C'est votre carte. Voici ce que voient les personnes qui scannent votre QR code.",
              ),
              OutlinedButton(
                onPressed: () => context.go(
                  c.organisation == null
                      ? '/carte'
                      : '/carte?organisation=${c.organisation!.id}',
                ),
                child: const Text('Voir ma carte'),
              ),
            ] else if (c.enregistree) ...[
              const Row(
                children: [
                  Icon(Icons.bookmark),
                  SizedBox(width: 8),
                  Expanded(child: Text('Cette carte est dans votre carnet.')),
                ],
              ),
              OutlinedButton(
                onPressed: () => context.go('/carnet'),
                child: const Text('Ouvrir mon carnet'),
              ),
            ] else ...[
              TextField(
                controller: _note,
                maxLength: 120,
                decoration: const InputDecoration(
                  labelText: 'Note personnelle (facultatif)',
                  hintText: "Rencontré au salon de l'habitat",
                ),
              ),
              OutlinedButton.icon(
                onPressed: _envoi ? null : _enregistrer,
                icon: const Icon(Icons.bookmark_add_outlined),
                label: const Text('Enregistrer dans mon carnet'),
              ),
            ],
          ],
        ),
      ],
    );
  }
}
