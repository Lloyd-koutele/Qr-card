import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../commun/widgets.dart';
import '../donnees/carte_api.dart';
import '../modeles/carte.dart';
import '../modeles/couleurs_carte.dart';
import '../modeles/pays.dart';
import '../modeles/types_contact.dart';
import '../widgets/champ_telephone.dart';
import '../widgets/carte_identite.dart';

const _tailleMaxPhoto = 2 * 1024 * 1024;

class EcranEditionCarte extends ConsumerWidget {
  const EcranEditionCarte({super.key, this.organisationId});

  /// Vide : carte personnelle.
  final String? organisationId;

  bool get pro => organisationId != null;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carte = ref.watch(maCarteProvider(organisationId));
    return Scaffold(
      appBar: AppBar(
        title: Text(pro ? "Carte d'organisation" : 'Modifier ma carte'),
      ),
      body: carte.when(
        loading: () => const Chargement(),
        error: (e, _) => ErreurChargement(
          e,
          reessayer: () => ref.invalidate(maCarteProvider(organisationId)),
        ),
        data: (c) => _Formulaire(carte: c, organisationId: organisationId),
      ),
    );
  }
}

bool _estNumero(String type) => type == 'TELEPHONE' || type == 'WHATSAPP';

class _ContactSaisi {
  /// Pour un numero, [valeur] ne garde que le numero national ; l'indicatif vient de [pays].
  factory _ContactSaisi(String type, String valeur) {
    if (!_estNumero(type)) return _ContactSaisi._(type, paysDe(null), valeur);
    final p = paysDuNumero(valeur) ?? paysDe(null);
    final chiffres = valeur.replaceAll(RegExp(r'\D'), '');
    return _ContactSaisi._(
      type,
      p,
      chiffres.startsWith(p.indicatif)
          ? chiffres.substring(p.indicatif.length)
          : chiffres,
    );
  }

  _ContactSaisi._(this.type, this.pays, String valeur)
    : valeur = TextEditingController(text: valeur);

  String type;
  Pays pays;
  final TextEditingController valeur;

  String get aEnvoyer => _estNumero(type)
      ? pays.international(valeur.text.trim())
      : valeur.text.trim();
}

class _Formulaire extends ConsumerStatefulWidget {
  const _Formulaire({required this.carte, this.organisationId});

  final Carte carte;

  /// Vide : carte personnelle.
  final String? organisationId;

  bool get pro => organisationId != null;

  @override
  ConsumerState<_Formulaire> createState() => _EtatFormulaire();
}

class _EtatFormulaire extends ConsumerState<_Formulaire> {
  late Carte _carte = widget.carte;
  late final _prenom = TextEditingController(text: widget.carte.prenom ?? '');
  late final _nom = TextEditingController(text: widget.carte.nom ?? '');
  late final _profession = TextEditingController(
    text: widget.carte.profession ?? '',
  );
  late final _bio = TextEditingController(text: widget.carte.bio ?? '');
  late String _couleur = widget.carte.couleur ?? '';
  late final List<_ContactSaisi> _contacts = [
    for (final c in widget.carte.contacts) _ContactSaisi(c.type, c.valeur),
  ];
  String _erreur = '';
  bool _envoi = false;

  @override
  void dispose() {
    for (final c in [
      _prenom,
      _nom,
      _profession,
      _bio,
      ..._contacts.map((c) => c.valeur),
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  CarteApi get _api => ref.read(carteApiProvider);

  void _recevoir(Carte c) {
    setState(() => _carte = c);
    ref.invalidate(maCarteProvider(widget.organisationId));
  }

  Future<void> _enregistrer() async {
    final vide = _contacts.indexWhere((c) => c.valeur.text.trim().isEmpty);
    if (vide >= 0) {
      return setState(
        () => _erreur =
            'Le contact n°${vide + 1} (${infoContact(_contacts[vide].type).libelle}) est vide',
      );
    }
    setState(() {
      _erreur = '';
      _envoi = true;
    });
    try {
      final c = await _api.modifier(widget.organisationId, {
        'prenom': _prenom.text,
        'nom': _nom.text,
        'profession': _profession.text,
        'bio': _bio.text,
        'couleur': _couleur,
        'contacts': [
          for (final c in _contacts) {'type': c.type, 'valeur': c.aEnvoyer},
        ],
      });
      _recevoir(c);
      if (mounted) {
        annoncer(context, 'Carte enregistrée');
        Navigator.of(context).maybePop();
      }
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _choisirPhoto() async {
    final image = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      maxWidth: 1200,
      maxHeight: 1200,
      imageQuality: 85,
    );
    if (image == null) return;
    if (await image.length() > _tailleMaxPhoto)
      return setState(() => _erreur = 'Photo trop lourde (2 Mo maximum)');
    final type = image.path.toLowerCase().endsWith('.png')
        ? 'image/png'
        : 'image/jpeg';
    try {
      final c = await _api.envoyerPhoto(
        widget.organisationId,
        image.path,
        type,
      );
      _recevoir(c);
    } catch (e) {
      setState(() => _erreur = '$e');
    }
  }

  Future<void> _retirerPhoto() async {
    try {
      _recevoir(await _api.supprimerPhoto(widget.organisationId));
    } catch (e) {
      setState(() => _erreur = '$e');
    }
  }

  Future<void> _nouveauLien() async {
    final ok = await confirmer(
      context,
      'Générer un nouveau lien ?',
      "L'ancien QR code ne fonctionnera plus et votre carte sera retirée des carnets des personnes qui l'ont enregistrée.",
      action: 'Nouveau lien',
      danger: true,
    );
    if (!ok) return;
    try {
      _recevoir(await _api.regenererLien(widget.organisationId));
      if (mounted)
        annoncer(
          context,
          'Nouveau lien généré. Pensez à réimprimer votre QR code.',
        );
    } catch (e) {
      setState(() => _erreur = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final apercu = _carte.apercu(
      prenom: _prenom.text,
      nom: _nom.text,
      profession: _profession.text,
      bio: _bio.text,
      couleur: _couleur,
      contacts: [
        for (final c in _contacts)
          ContactCarte(type: c.type, valeur: c.aEnvoyer),
      ],
    );
    return Ecran(
      enfants: [
        CarteIdentite(carte: apercu),
        const SizedBox(height: 16),
        if (_erreur.isNotEmpty) Alerte(_erreur),
        Panneau(
          enfants: [
            Text('Identité', style: Theme.of(context).textTheme.titleMedium),
            Row(
              spacing: 12,
              children: [
                Expanded(
                  child: TextField(
                    controller: _prenom,
                    maxLength: 80,
                    decoration: const InputDecoration(
                      labelText: 'Prénom',
                      counterText: '',
                    ),
                    onChanged: (_) => setState(() {}),
                  ),
                ),
                Expanded(
                  child: TextField(
                    controller: _nom,
                    maxLength: 80,
                    decoration: const InputDecoration(
                      labelText: 'Nom',
                      counterText: '',
                    ),
                    onChanged: (_) => setState(() {}),
                  ),
                ),
              ],
            ),
            TextField(
              controller: _profession,
              maxLength: 120,
              decoration: InputDecoration(
                labelText: widget.pro ? 'Poste' : 'Profession',
                counterText: '',
              ),
              onChanged: (_) => setState(() {}),
            ),
            TextField(
              controller: _bio,
              maxLength: 300,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: 'Présentation (facultatif)',
              ),
            ),
            Wrap(
              spacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: _choisirPhoto,
                  icon: const Icon(Icons.photo_outlined),
                  label: Text(
                    _carte.photoVersion == null
                        ? 'Ajouter une photo'
                        : 'Changer la photo',
                  ),
                ),
                if (_carte.photoVersion != null)
                  TextButton(
                    onPressed: _retirerPhoto,
                    child: const Text('Retirer la photo'),
                  ),
              ],
            ),
          ],
        ),
        if (!widget.pro)
          Panneau(
            enfants: [
              Text(
                'Couleur de la carte',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  for (final c in couleursCarte)
                    _Pastille(
                      couleur: c,
                      choisie: _couleur == c.valeur,
                      surChoix: () => setState(() => _couleur = c.valeur),
                    ),
                ],
              ),
              Text(
                'Les personnes qui scannent votre QR code voient votre carte dans cette couleur. Enregistrez pour la garder.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        Panneau(
          enfants: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Contacts et réseaux',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text('${_contacts.length}/20'),
              ],
            ),
            const Text(
              "Choisissez le pays puis saisissez le numéro, ou votre nom d'utilisateur : le lien est créé pour vous. Les 8 premiers apparaissent au verso.",
            ),
            for (var i = 0; i < _contacts.length; i++) _ligneContact(i),
            if (_contacts.length < 20)
              OutlinedButton.icon(
                onPressed: () => setState(
                  () => _contacts.add(_ContactSaisi('TELEPHONE', '')),
                ),
                icon: const Icon(Icons.add),
                label: const Text('Ajouter un contact'),
              ),
          ],
        ),
        FilledButton(
          onPressed: _envoi ? null : _enregistrer,
          child: Text(_envoi ? 'Enregistrement…' : 'Enregistrer la carte'),
        ),
        const SizedBox(height: 24),
        TextButton.icon(
          onPressed: _nouveauLien,
          icon: const Icon(Icons.refresh),
          label: const Text('Générer un nouveau lien'),
          style: TextButton.styleFrom(
            foregroundColor: Theme.of(context).colorScheme.error,
          ),
        ),
      ],
    );
  }

  Widget _ligneContact(int i) {
    final c = _contacts[i];
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: c.type,
                decoration: const InputDecoration(labelText: 'Type'),
                items: [
                  for (final t in typesContact)
                    DropdownMenuItem(value: t.type, child: Text(t.libelle)),
                ],
                onChanged: (v) => setState(() {
                  if (v == null) return;
                  // Telephone <-> WhatsApp : le numero est garde ; sinon la valeur ne convient plus.
                  if (_estNumero(v) != _estNumero(c.type)) c.valeur.clear();
                  c.type = v;
                }),
              ),
            ),
            IconButton(
              tooltip: 'Monter',
              onPressed: i == 0
                  ? null
                  : () => setState(
                      () => _contacts.insert(i - 1, _contacts.removeAt(i)),
                    ),
              icon: const Icon(Icons.arrow_upward),
            ),
            IconButton(
              tooltip: 'Supprimer',
              onPressed: () =>
                  setState(() => _contacts.removeAt(i).valeur.dispose()),
              icon: const Icon(Icons.delete_outline),
            ),
          ],
        ),
        if (_estNumero(c.type))
          ChampTelephone(
            key: ObjectKey(c),
            pays: c.pays,
            numero: c.valeur,
            surPays: (p) => setState(() => c.pays = p),
          )
        else
          TextField(
            controller: c.valeur,
            maxLength: 255,
            decoration: InputDecoration(
              hintText: infoContact(c.type).exemple,
              counterText: '',
            ),
            onChanged: (_) => setState(() {}),
          ),
        const Divider(),
      ],
    );
  }
}

class _Pastille extends StatelessWidget {
  const _Pastille({
    required this.couleur,
    required this.choisie,
    required this.surChoix,
  });

  final CouleurProposee couleur;
  final bool choisie;
  final VoidCallback surChoix;

  @override
  Widget build(BuildContext context) {
    final bord = Theme.of(context).colorScheme.onSurface;
    return Semantics(
      label: couleur.nom,
      selected: choisie,
      button: true,
      child: Tooltip(
        message: couleur.nom,
        child: InkWell(
          onTap: surChoix,
          customBorder: const CircleBorder(),
          child: Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: couleurHex(couleur.hex),
              shape: BoxShape.circle,
              border: Border.all(
                color: choisie ? bord : Colors.transparent,
                width: 2.5,
              ),
            ),
            child: choisie
                ? const Icon(Icons.check, color: Colors.white, size: 20)
                : null,
          ),
        ),
      ),
    );
  }
}
