import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';

class EcranMotDePasseOublie extends ConsumerStatefulWidget {
  const EcranMotDePasseOublie({super.key});

  @override
  ConsumerState<EcranMotDePasseOublie> createState() => _EtatOubli();
}

class _EtatOubli extends ConsumerState<EcranMotDePasseOublie> {
  final _email = TextEditingController();
  final _code = TextEditingController();
  final _nouveau = TextEditingController();
  bool _codeEnvoye = false;
  bool _envoi = false;
  String _erreur = '';

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _nouveau.dispose();
    super.dispose();
  }

  Future<void> _agir(Future<void> Function() action) async {
    setState(() {
      _erreur = '';
      _envoi = true;
    });
    try {
      await action();
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  void _demander() {
    if (_email.text.trim().isEmpty)
      return setState(() => _erreur = 'Saisissez votre email');
    _agir(() async {
      await ref.read(compteApiProvider).motDePasseOublie(_email.text.trim());
      setState(() => _codeEnvoye = true);
    });
  }

  void _reinitialiser() {
    if (_nouveau.text.length < 8)
      return setState(
        () => _erreur = 'Le mot de passe doit faire au moins 8 caractères',
      );
    _agir(() async {
      await ref
          .read(compteApiProvider)
          .reinitialiser(_email.text.trim(), _code.text.trim(), _nouveau.text);
      if (mounted)
        context.go(
          '/connexion',
          extra: 'Mot de passe modifié. Connectez-vous.',
        );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Ecran(
        largeur: 420,
        enfants: [
          Titre(
            'Mot de passe oublié',
            sousTitre: _codeEnvoye
                ? "Si un compte existe pour ${_email.text.trim()}, un code vient d'y être envoyé."
                : 'Indiquez votre email : vous recevrez un code pour choisir un nouveau mot de passe.',
          ),
          if (_erreur.isNotEmpty) Alerte(_erreur),
          TextField(
            controller: _email,
            enabled: !_codeEnvoye,
            keyboardType: TextInputType.emailAddress,
            autocorrect: false,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          const SizedBox(height: 12),
          if (_codeEnvoye) ...[
            TextField(
              controller: _code,
              keyboardType: TextInputType.number,
              maxLength: 6,
              autofillHints: const [AutofillHints.oneTimeCode],
              decoration: const InputDecoration(
                labelText: 'Code reçu',
                counterText: '',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _nouveau,
              obscureText: true,
              autofillHints: const [AutofillHints.newPassword],
              decoration: const InputDecoration(
                labelText: 'Nouveau mot de passe',
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _envoi ? null : _reinitialiser,
              child: const Text('Changer le mot de passe'),
            ),
          ] else
            FilledButton(
              onPressed: _envoi ? null : _demander,
              child: const Text('Recevoir un code'),
            ),
        ],
      ),
    );
  }
}
