import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';
import '../modeles/profil.dart';
import '../session.dart';

class EcranVerificationEmail extends ConsumerStatefulWidget {
  const EcranVerificationEmail({super.key, required this.attente});

  final InscriptionAttente attente;

  @override
  ConsumerState<EcranVerificationEmail> createState() => _EtatVerification();
}

class _EtatVerification extends ConsumerState<EcranVerificationEmail> {
  final _code = TextEditingController();
  late InscriptionAttente _attente = widget.attente;
  late int _restant = widget.attente.renvoiDansSecondes;
  Timer? _minuteur;
  String _erreur = '';
  String _message = '';
  bool _envoi = false;

  @override
  void initState() {
    super.initState();
    _demarrerCompteARebours();
  }

  @override
  void dispose() {
    _minuteur?.cancel();
    _code.dispose();
    super.dispose();
  }

  void _demarrerCompteARebours() {
    _minuteur?.cancel();
    _minuteur = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_restant <= 0) return t.cancel();
      setState(() => _restant--);
    });
  }

  Future<void> _verifier() async {
    if (_code.text.trim().length != 6)
      return setState(() => _erreur = 'Le code contient 6 chiffres');
    setState(() {
      _erreur = '';
      _envoi = true;
    });
    try {
      final r = await ref
          .read(compteApiProvider)
          .verifierEmail(_attente.jeton, _code.text.trim());
      if (mounted) await terminerConnexion(context, ref, r);
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _renvoyer() async {
    try {
      final a = await ref.read(compteApiProvider).renvoyerCode(_attente.jeton);
      setState(() {
        _attente = a;
        _restant = a.renvoiDansSecondes;
        _erreur = '';
        _message = 'Nouveau code envoyé';
      });
      _demarrerCompteARebours();
    } catch (e) {
      setState(() => _erreur = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Ecran(
        largeur: 420,
        enfants: [
          Titre(
            'Vérifiez votre email',
            sousTitre:
                'Saisissez le code à 6 chiffres envoyé à ${_attente.email}.',
          ),
          if (_erreur.isNotEmpty) Alerte(_erreur),
          if (_message.isNotEmpty && _erreur.isEmpty)
            Alerte(_message, type: TypeAlerte.succes),
          TextField(
            controller: _code,
            autofocus: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            autofillHints: const [AutofillHints.oneTimeCode],
            style: const TextStyle(fontSize: 22, letterSpacing: 6),
            decoration: const InputDecoration(
              labelText: 'Code',
              counterText: '',
            ),
            onSubmitted: (_) => _verifier(),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _envoi ? null : _verifier,
            child: const Text('Valider'),
          ),
          TextButton(
            onPressed: _restant > 0 ? null : _renvoyer,
            child: Text(
              _restant > 0
                  ? 'Renvoyer le code dans $_restant s'
                  : 'Renvoyer le code',
            ),
          ),
        ],
      ),
    );
  }
}
