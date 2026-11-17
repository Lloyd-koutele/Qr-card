import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../commun/api.dart';
import '../../commun/stockage.dart';
import 'donnees/compte_api.dart';
import 'modeles/profil.dart';

class EtatSession {
  const EtatSession({this.chargement = false, this.profil, this.erreur});

  final bool chargement;
  final Profil? profil;

  /// Serveur injoignable au demarrage : la session est gardee, on propose de reessayer.
  final String? erreur;

  bool get connecte => profil != null;
}

class SessionNotifier extends Notifier<EtatSession> {
  static const _cle = 'madeqrcard_jeton';

  @override
  EtatSession build() {
    ref.read(apiProvider).surSessionExpiree = deconnecter;
    Future.microtask(demarrer);
    return const EtatSession(chargement: true);
  }

  Future<void> demarrer() async {
    state = const EtatSession(chargement: true);
    final jeton = await Stockage.lire(_cle);
    if (jeton == null) {
      state = const EtatSession();
      return;
    }
    ref.read(apiProvider).jeton = jeton;
    try {
      await rafraichir();
    } on ErreurApi catch (e) {
      state = e.statut == 401
          ? const EtatSession()
          : EtatSession(erreur: e.message);
    }
  }

  Future<Profil> rafraichir() async {
    final profil = await ref.read(compteApiProvider).profil();
    state = EtatSession(profil: profil);
    return profil;
  }

  Future<void> connecter(String jeton) async {
    ref.read(apiProvider).jeton = jeton;
    await Stockage.ecrire(_cle, jeton);
    await rafraichir();
  }

  Future<void> deconnecter() async {
    ref.read(apiProvider).jeton = null;
    await Stockage.effacer(_cle);
    state = const EtatSession();
  }
}

final sessionProvider = NotifierProvider<SessionNotifier, EtatSession>(
  SessionNotifier.new,
);

/// Fin d'une connexion : code 2FA a saisir, ou session ouverte (le routeur quitte alors les pages de connexion).
Future<void> terminerConnexion(
  BuildContext context,
  WidgetRef ref,
  ResultatConnexion r,
) async {
  if (r.mfaRequis && r.mfaToken != null) {
    context.push('/connexion/code', extra: r.mfaToken);
    return;
  }
  await ref.read(sessionProvider.notifier).connecter(r.token!);
}

/// Mode clair ou sombre, garde sur l'appareil. Clair par defaut, y compris
/// pour un ancien choix « Système » qui n'est plus propose.
class ModeNotifier extends Notifier<ThemeMode> {
  static const _cle = 'madeqrcard_mode';

  @override
  ThemeMode build() {
    Future.microtask(() async {
      final valeur = await Stockage.lire(_cle);
      state = valeur == ThemeMode.dark.name ? ThemeMode.dark : ThemeMode.light;
    });
    return ThemeMode.light;
  }

  Future<void> choisir(ThemeMode mode) async {
    state = mode;
    await Stockage.ecrire(_cle, mode.name);
  }
}

final modeProvider = NotifierProvider<ModeNotifier, ThemeMode>(
  ModeNotifier.new,
);
