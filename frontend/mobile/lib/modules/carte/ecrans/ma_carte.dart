import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:share_plus/share_plus.dart';

import '../../../commun/config.dart';
import '../../../commun/widgets.dart';
import '../donnees/carte_api.dart';
import '../modeles/carte.dart';
import '../widgets/carte_identite.dart';

/// Carte personnelle et, pour un membre d'organisations, une carte professionnelle par organisation.
class EcranMaCarte extends ConsumerStatefulWidget {
  const EcranMaCarte({super.key, this.organisationId});

  /// Carte a afficher d'abord : vide pour la carte personnelle.
  final String? organisationId;

  @override
  ConsumerState<EcranMaCarte> createState() => _EtatMaCarte();
}

class _EtatMaCarte extends ConsumerState<EcranMaCarte> {
  late String? _choix = widget.organisationId;

  @override
  void didUpdateWidget(EcranMaCarte ancien) {
    super.didUpdateWidget(ancien);
    if (ancien.organisationId != widget.organisationId) {
      _choix = widget.organisationId;
    }
  }

  @override
  Widget build(BuildContext context) {
    final cartesPro = ref.watch(cartesProProvider).value ?? const <Carte>[];
    // Une organisation quittee entre-temps : retour a la carte personnelle
    final choix = cartesPro.any((c) => c.organisation?.id == _choix)
        ? _choix
        : null;
    final pro = choix != null;
    final carte = ref.watch(maCarteProvider(choix));

    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(cartesProProvider);
        return ref.refresh(maCarteProvider(choix).future);
      },
      child: Ecran(
        enfants: [
          Titre(
            pro ? 'Ma carte professionnelle' : 'Ma carte',
            sousTitre: pro
                ? "Le nom, le logo, la couleur et l'adresse viennent de l'organisation. Ses administrateurs peuvent la lire."
                : 'Seul vous pouvez la modifier. Ceux qui scannent votre QR code la voient en lecture seule.',
          ),
          if (cartesPro.isNotEmpty) ...[
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                spacing: 8,
                children: [
                  ChoiceChip(
                    avatar: const Icon(Icons.person_outline, size: 18),
                    label: const Text('Personnelle'),
                    selected: choix == null,
                    onSelected: (_) => setState(() => _choix = null),
                  ),
                  for (final c in cartesPro)
                    ChoiceChip(
                      avatar: const Icon(Icons.business_outlined, size: 18),
                      label: Text(c.organisation!.nom),
                      selected: choix == c.organisation!.id,
                      onSelected: (_) =>
                          setState(() => _choix = c.organisation!.id),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],
          carte.when(
            loading: () => const Chargement(),
            error: (e, _) => ErreurChargement(
              e,
              reessayer: () => ref.invalidate(maCarteProvider(choix)),
            ),
            data: (c) => _Contenu(carte: c, organisationId: choix),
          ),
        ],
      ),
    );
  }
}

class _Contenu extends StatelessWidget {
  const _Contenu({required this.carte, this.organisationId});

  final Carte carte;
  final String? organisationId;

  @override
  Widget build(BuildContext context) {
    final lien = Config.lienCarte(carte.jeton);
    final n = carte.enregistrements ?? 0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        CarteIdentite(carte: carte),
        const SizedBox(height: 8),
        Text(
          'Touchez la carte pour la retourner · Enregistrée par $n personne${n > 1 ? 's' : ''}',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () => context.push('/carte/qr', extra: carte),
          icon: const Icon(Icons.qr_code_2),
          label: const Text('Montrer mon QR code'),
        ),
        const SizedBox(height: 8),
        Row(
          spacing: 8,
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () =>
                    SharePlus.instance.share(ShareParams(text: lien)),
                icon: const Icon(Icons.ios_share),
                label: const Text('Partager'),
              ),
            ),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => context.push(
                  organisationId == null
                      ? '/carte/modifier'
                      : '/carte/modifier?organisation=$organisationId',
                ),
                icon: const Icon(Icons.edit_outlined),
                label: const Text('Modifier'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        SelectableText(
          lien,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

/// QR code en grand, pour le faire scanner.
class EcranQrPleinEcran extends StatelessWidget {
  const EcranQrPleinEcran({super.key, required this.carte});

  final Carte carte;

  @override
  Widget build(BuildContext context) {
    final couleurs = CouleursCarte.deCarte(carte);
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
      ),
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  carte.nomComplet,
                  style: const TextStyle(
                    color: Colors.black,
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (carte.profession != null)
                  Text(
                    carte.profession!,
                    style: const TextStyle(color: Colors.black54, fontSize: 16),
                  ),
                const SizedBox(height: 24),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 320),
                  child: AspectRatio(
                    aspectRatio: 1,
                    child: _Qr(
                      lien: Config.lienCarte(carte.jeton),
                      couleur: carte.organisation == null
                          ? couleurs.foncee
                          : Colors.black,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Scannez pour ouvrir ma carte',
                  style: TextStyle(color: Colors.black54),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Qr extends StatelessWidget {
  const _Qr({required this.lien, required this.couleur});

  final String lien;
  final Color couleur;

  @override
  Widget build(BuildContext context) => QrPlein(lien: lien, couleur: couleur);
}
