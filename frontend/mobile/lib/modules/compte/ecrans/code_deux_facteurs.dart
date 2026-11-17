import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';
import '../session.dart';

class EcranCodeDeuxFacteurs extends ConsumerStatefulWidget {
  const EcranCodeDeuxFacteurs({super.key, required this.mfaToken});

  final String mfaToken;

  @override
  ConsumerState<EcranCodeDeuxFacteurs> createState() => _EtatCode();
}

class _EtatCode extends ConsumerState<EcranCodeDeuxFacteurs> {
  final _code = TextEditingController();
  String _erreur = '';
  bool _envoi = false;

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _valider() async {
    if (_code.text.trim().isEmpty)
      return setState(() => _erreur = 'Saisissez le code');
    setState(() {
      _erreur = '';
      _envoi = true;
    });
    try {
      final r = await ref
          .read(compteApiProvider)
          .deuxFacteurs(widget.mfaToken, _code.text.trim());
      if (mounted) await terminerConnexion(context, ref, r);
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
            'Double authentification',
            sousTitre: "Saisissez le code de votre application d'authentification, ou un code de secours.",
          ),
          if (_erreur.isNotEmpty) Alerte(_erreur),
          TextField(
            controller: _code,
            autofocus: true,
            keyboardType: TextInputType.number,
            autofillHints: const [AutofillHints.oneTimeCode],
            decoration: const InputDecoration(labelText: 'Code'),
            onSubmitted: (_) => _valider(),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _envoi ? null : _valider,
            child: const Text('Valider'),
          ),
        ],
      ),
    );
  }
}
