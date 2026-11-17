import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../commun/config.dart';
import '../../../commun/widgets.dart';
import '../donnees/compte_api.dart';
import '../google.dart';
import '../session.dart';

final googleActifProvider = FutureProvider<bool>(
  (ref) => ref.watch(compteApiProvider).googleActif(),
);

class EcranConnexion extends ConsumerStatefulWidget {
  const EcranConnexion({super.key, this.message});

  final String? message;

  @override
  ConsumerState<EcranConnexion> createState() => _EcranConnexionState();
}

class _EcranConnexionState extends ConsumerState<EcranConnexion> {
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();
  String _erreur = '';
  bool _envoi = false;

  @override
  void dispose() {
    _email.dispose();
    _motDePasse.dispose();
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

  void _connecter() {
    if (_email.text.trim().isEmpty || _motDePasse.text.isEmpty) {
      setState(() => _erreur = 'Saisissez votre email et votre mot de passe');
      return;
    }
    _agir(() async {
      final r = await ref
          .read(compteApiProvider)
          .connexion(_email.text.trim(), _motDePasse.text);
      if (mounted) await terminerConnexion(context, ref, r);
    });
  }

  void _google() => _agir(() async {
    final r = await connexionGoogle(ref.read(compteApiProvider));
    if (r != null && mounted) await terminerConnexion(context, ref, r);
  });

  @override
  Widget build(BuildContext context) {
    final google = ref.watch(googleActifProvider).value ?? false;
    return Scaffold(
      body: Ecran(
        largeur: 420,
        enfants: [
          const SizedBox(height: 24),
          const _Logo(),
          const SizedBox(height: 24),
          const Titre(
            'Connexion',
            sousTitre: 'Accédez à votre carte et à votre carnet.',
          ),
          if (widget.message != null && _erreur.isEmpty)
            Alerte(widget.message!, type: TypeAlerte.succes),
          if (_erreur.isNotEmpty) Alerte(_erreur),
          if (google) ...[
            OutlinedButton.icon(
              onPressed: _envoi ? null : _google,
              icon: const FaIcon(FontAwesomeIcons.google, size: 16),
              label: const Text('Continuer avec Google'),
            ),
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Text('ou', textAlign: TextAlign.center),
            ),
          ],
          TextField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.email],
            autocorrect: false,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _motDePasse,
            obscureText: true,
            autofillHints: const [AutofillHints.password],
            decoration: const InputDecoration(labelText: 'Mot de passe'),
            onSubmitted: (_) => _connecter(),
          ),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => context.push('/mot-de-passe-oublie'),
              child: const Text('Mot de passe oublié ?'),
            ),
          ),
          FilledButton(
            onPressed: _envoi ? null : _connecter,
            child: Text(_envoi ? 'Connexion…' : 'Se connecter'),
          ),
          const SizedBox(height: 8),
          TextButton(
            onPressed: () => context.push('/inscription'),
            child: const Text('Pas encore de compte ? Créer un compte'),
          ),
          // Le compte de l'administrateur se cree avec l'organisation, sur le site
          TextButton(
            onPressed: () => launchUrl(
              Uri.parse('${Config.siteUrl}/creer-organisation'),
              mode: LaunchMode.externalApplication,
            ),
            child: const Text('Créer une organisation'),
          ),
        ],
      ),
    );
  }
}

class _Logo extends StatelessWidget {
  const _Logo();

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFFC8102E),
            borderRadius: BorderRadius.circular(10),
          ),
          child: const Icon(Icons.qr_code_2, color: Colors.white),
        ),
        const SizedBox(width: 10),
        Text(
          'MadeQrCard',
          style: Theme.of(context).textTheme.titleLarge
              ?.copyWith(fontWeight: FontWeight.w700),
        ),
      ],
    );
  }
}
