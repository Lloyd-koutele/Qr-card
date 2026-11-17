import 'package:flutter/material.dart';

/// Page a largeur limitee, defilante, avec les marges de l'application.
class Ecran extends StatelessWidget {
  const Ecran({super.key, required this.enfants, this.largeur = 560});

  final List<Widget> enfants;
  final double largeur;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        children: [
          Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: largeur),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: enfants,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

enum TypeAlerte { erreur, succes, info }

class Alerte extends StatelessWidget {
  const Alerte(this.message, {super.key, this.type = TypeAlerte.erreur});

  final String message;
  final TypeAlerte type;

  @override
  Widget build(BuildContext context) {
    final couleurs = Theme.of(context).colorScheme;
    final (fond, texte) = switch (type) {
      TypeAlerte.erreur => (couleurs.errorContainer, couleurs.onErrorContainer),
      TypeAlerte.succes => (const Color(0xFFDDF3E4), const Color(0xFF1B6B3A)),
      TypeAlerte.info => (
        couleurs.secondaryContainer,
        couleurs.onSecondaryContainer,
      ),
    };
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: fond,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message, style: TextStyle(color: texte)),
    );
  }
}

class Chargement extends StatelessWidget {
  const Chargement({super.key});

  @override
  Widget build(BuildContext context) => const Center(
    child: Padding(
      padding: EdgeInsets.all(32),
      child: CircularProgressIndicator(),
    ),
  );
}

/// Erreur de chargement d'une page, avec un bouton pour recommencer.
class ErreurChargement extends StatelessWidget {
  const ErreurChargement(this.erreur, {super.key, required this.reessayer});

  final Object erreur;
  final VoidCallback reessayer;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('$erreur', textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: reessayer,
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }
}

class Titre extends StatelessWidget {
  const Titre(this.texte, {super.key, this.sousTitre});

  final String texte;
  final String? sousTitre;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            texte,
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          if (sousTitre != null) ...[
            const SizedBox(height: 4),
            Text(
              sousTitre!,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class Panneau extends StatelessWidget {
  const Panneau({super.key, required this.enfants});

  final List<Widget> enfants;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 12,
          children: enfants,
        ),
      ),
    );
  }
}

void annoncer(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}

Future<bool> confirmer(
  BuildContext context,
  String titre,
  String texte, {
  String action = 'Confirmer',
  bool danger = false,
}) async {
  final reponse = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(titre),
      content: Text(texte),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('Annuler'),
        ),
        FilledButton(
          style: danger
              ? FilledButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.error,
                )
              : null,
          onPressed: () => Navigator.pop(context, true),
          child: Text(action),
        ),
      ],
    ),
  );
  return reponse ?? false;
}

String dateFr(DateTime? d) {
  if (d == null) return '';
  final l = d.toLocal();
  String deux(int n) => n.toString().padLeft(2, '0');
  return '${deux(l.day)}/${deux(l.month)}/${l.year}';
}

/// Demande un texte dans une boite de dialogue. Null si l'utilisateur annule.
/// Le champ appartient a la boite : il reste valable pendant l'animation de fermeture.
Future<String?> demanderTexte(
  BuildContext context, {
  required String titre,
  String action = 'Valider',
  String? indice,
  String initial = '',
  int? longueurMax,
  TextInputType clavier = TextInputType.text,
}) => showDialog<String>(
  context: context,
  builder: (_) => _BoiteTexte(
    titre: titre,
    action: action,
    indice: indice,
    initial: initial,
    longueurMax: longueurMax,
    clavier: clavier,
  ),
);

class _BoiteTexte extends StatefulWidget {
  const _BoiteTexte({
    required this.titre,
    required this.action,
    required this.initial,
    required this.clavier,
    this.indice,
    this.longueurMax,
  });

  final String titre;
  final String action;
  final String? indice;
  final String initial;
  final int? longueurMax;
  final TextInputType clavier;

  @override
  State<_BoiteTexte> createState() => _EtatBoiteTexte();
}

class _EtatBoiteTexte extends State<_BoiteTexte> {
  late final _champ = TextEditingController(text: widget.initial);

  @override
  void dispose() {
    _champ.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.titre),
      content: TextField(
        controller: _champ,
        autofocus: true,
        keyboardType: widget.clavier,
        maxLength: widget.longueurMax,
        decoration: InputDecoration(hintText: widget.indice),
        onSubmitted: (texte) => Navigator.pop(context, texte),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Annuler'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, _champ.text),
          child: Text(widget.action),
        ),
      ],
    );
  }
}
