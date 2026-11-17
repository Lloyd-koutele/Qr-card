import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../modeles/pays.dart';

/// Numero de telephone ou WhatsApp : bouton du pays (recherche par nom ou indicatif)
/// et numero national limite au nombre de chiffres du pays.
class ChampTelephone extends StatefulWidget {
  const ChampTelephone({
    super.key,
    required this.pays,
    required this.numero,
    required this.surPays,
  });

  final Pays pays;

  /// Numero national (sans l'indicatif).
  final TextEditingController numero;
  final ValueChanged<Pays> surPays;

  @override
  State<ChampTelephone> createState() => _EtatChampTelephone();
}

class _EtatChampTelephone extends State<ChampTelephone> {
  Future<void> _choisirPays() async {
    final choisi = await showModalBottomSheet<Pays>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      showDragHandle: true,
      builder: (_) => _ChoixPays(actuel: widget.pays),
    );
    if (choisi != null) {
      widget.surPays(choisi);
      widget.numero.text = choisi.numeroNational(widget.numero.text);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.pays;
    final national = widget.numero.text;
    final incomplet =
        national.isNotEmpty &&
        !p.estComplet(national) &&
        !national.startsWith(RegExp(r'\+|00'));
    final aide =
        '${p.nom} : ${p.longueursTexte} chiffres${national.isEmpty ? '' : ' · ${national.length} saisi${national.length > 1 ? 's' : ''}'}';
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(right: 8),
          child: OutlinedButton(
            onPressed: _choisirPays,
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(0, 56),
              padding: const EdgeInsets.symmetric(horizontal: 10),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            child: Semantics(
              label: 'Pays : ${p.nom}, +${p.indicatif}',
              excludeSemantics: true,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(p.drapeau, style: const TextStyle(fontSize: 20)),
                  const SizedBox(width: 6),
                  Text('+${p.indicatif}'),
                  const Icon(Icons.arrow_drop_down),
                ],
              ),
            ),
          ),
        ),
        Expanded(
          child: TextField(
            controller: widget.numero,
            keyboardType: TextInputType.phone,
            autofillHints: const [AutofillHints.telephoneNumberNational],
            inputFormatters: [_FormatNumero(() => widget.pays, widget.surPays)],
            decoration: InputDecoration(
              hintText: p.exemple,
              helperText: incomplet ? null : aide,
              errorText: incomplet ? aide : null,
            ),
            onChanged: (_) => setState(() {}),
          ),
        ),
      ],
    );
  }
}

/// Garde les chiffres du pays ; un numero saisi avec son indicatif (+225..., 00225...) change le pays.
class _FormatNumero extends TextInputFormatter {
  _FormatNumero(this.pays, this.surPays);

  final Pays Function() pays;
  final ValueChanged<Pays> surPays;

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue ancien,
    TextEditingValue nouveau,
  ) {
    final texte = nouveau.text.trim();
    var p = pays();
    var reste = texte;
    final international = RegExp(r'^(\+|00)').firstMatch(texte);
    if (international != null) {
      final apres = texte.substring(international.end);
      final trouve = paysDuNumero(apres);
      // Indicatif encore incomplet (+2, +22...) : garde tel quel
      if (trouve == null)
        return nouveau.copyWith(
          text: '${international[0]}${apres.replaceAll(RegExp(r'\D'), '')}',
        );
      p = trouve;
      reste = apres
          .replaceAll(RegExp(r'\D'), '')
          .substring(trouve.indicatif.length);
      WidgetsBinding.instance.addPostFrameCallback((_) => surPays(trouve));
    }
    final national = p.numeroNational(reste);
    return TextEditingValue(
      text: national,
      selection: TextSelection.collapsed(offset: national.length),
    );
  }
}

class _ChoixPays extends StatefulWidget {
  const _ChoixPays({required this.actuel});

  final Pays actuel;

  @override
  State<_ChoixPays> createState() => _EtatChoixPays();
}

class _EtatChoixPays extends State<_ChoixPays> {
  String _recherche = '';

  @override
  Widget build(BuildContext context) {
    final resultats = chercherPays(_recherche);
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SizedBox(
        height: MediaQuery.sizeOf(context).height * 0.75,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              child: TextField(
                autofocus: true,
                decoration: const InputDecoration(
                  prefixIcon: Icon(Icons.search),
                  hintText: 'Pays ou indicatif (Sénégal, 221…)',
                ),
                onChanged: (v) => setState(() => _recherche = v),
                onSubmitted: (_) {
                  if (resultats.isNotEmpty)
                    Navigator.pop(context, resultats.first);
                },
              ),
            ),
            Expanded(
              child: resultats.isEmpty
                  ? const Center(child: Text('Aucun pays trouvé'))
                  : ListView.builder(
                      itemCount: resultats.length,
                      itemBuilder: (_, i) {
                        final p = resultats[i];
                        final tuile = ListTile(
                          leading: Text(
                            p.drapeau,
                            style: const TextStyle(fontSize: 24),
                          ),
                          title: Text(p.nom),
                          trailing: Text(
                            '+${p.indicatif}',
                            style: theme.textTheme.bodyMedium,
                          ),
                          selected: p.iso == widget.actuel.iso,
                          onTap: () => Navigator.pop(context, p),
                        );
                        return _recherche.isEmpty && i == paysFrequents.length
                            ? Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [const Divider(height: 1), tuile],
                              )
                            : tuile;
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
