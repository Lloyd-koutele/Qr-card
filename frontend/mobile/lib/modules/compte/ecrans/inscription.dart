import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';

class EcranInscription extends ConsumerStatefulWidget {
  const EcranInscription({super.key});

  @override
  ConsumerState<EcranInscription> createState() => _EtatInscription();
}

class _EtatInscription extends ConsumerState<EcranInscription> {
  final _nom = TextEditingController();
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();
  String _erreur = '';
  bool _envoi = false;

  @override
  void dispose() {
    _nom.dispose();
    _email.dispose();
    _motDePasse.dispose();
    super.dispose();
  }

  Future<void> _creer() async {
    if (_nom.text.trim().isEmpty || _email.text.trim().isEmpty)
      return setState(() => _erreur = 'Indiquez votre nom et votre email');
    if (_motDePasse.text.length < 8)
      return setState(
        () => _erreur = 'Le mot de passe doit faire au moins 8 caractères',
      );
    setState(() {
      _erreur = '';
      _envoi = true;
    });
    try {
      final attente = await ref
          .read(compteApiProvider)
          .inscription(_email.text.trim(), _motDePasse.text, _nom.text.trim());
      if (mounted) context.push('/inscription/code', extra: attente);
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Ecran(
        largeur: 420,
        enfants: [
          const Titre(
            'Créer un compte',
            sousTitre: 'Un code de vérification vous sera envoyé par email.',
          ),
          if (_erreur.isNotEmpty) Alerte(_erreur),
          TextField(
            controller: _nom,
            autofillHints: const [AutofillHints.name],
            decoration: const InputDecoration(labelText: 'Nom'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            autocorrect: false,
            autofillHints: const [AutofillHints.email],
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _motDePasse,
            obscureText: true,
            autofillHints: const [AutofillHints.newPassword],
            decoration: const InputDecoration(
              labelText: 'Mot de passe',
              helperText: '8 caractères au minimum',
            ),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _envoi ? null : _creer,
            child: Text(_envoi ? 'Envoi du code…' : 'Créer mon compte'),
          ),
        ],
      ),
    );
  }
}
