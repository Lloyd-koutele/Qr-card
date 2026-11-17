import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../modules/carnet/ecrans/carnet.dart';
import '../modules/carte/ecrans/carte_publique.dart';
import '../modules/carte/ecrans/edition_carte.dart';
import '../modules/carte/ecrans/ma_carte.dart';
import '../modules/carte/modeles/carte.dart';
import '../modules/compte/ecrans/code_deux_facteurs.dart';
import '../modules/compte/ecrans/connexion.dart';
import '../modules/compte/ecrans/inscription.dart';
import '../modules/compte/ecrans/mot_de_passe_oublie.dart';
import '../modules/compte/ecrans/parametres.dart';
import '../modules/compte/ecrans/verification_email.dart';
import '../modules/compte/modeles/profil.dart';
import '../modules/compte/session.dart';
import '../modules/organisation/ecrans/organisation.dart';
import '../modules/organisation/ecrans/invitation.dart';
import '../modules/scan/ecrans/scanner.dart';
import 'coquille.dart';
import 'demarrage.dart';

const _pagesSansCompte = ['/connexion', '/inscription', '/mot-de-passe-oublie'];

/// Redirection selon la session : sans compte, seules les pages de connexion sont accessibles.
String? redirection(EtatSession session, String lieu) {
  final sansCompte = _pagesSansCompte.any(lieu.startsWith);
  if (session.chargement || session.erreur != null)
    return lieu == '/demarrage' ? null : '/demarrage';
  final profil = session.profil;
  if (profil == null) return sansCompte ? null : '/connexion';
  if (profil.superAdmin || profil.securiteRequise)
    return lieu == '/site' ? null : '/site';
  if (sansCompte || lieu == '/demarrage' || lieu == '/site') return '/carte';
  return null;
}

final routeurProvider = Provider<GoRouter>((ref) {
  final changement = ValueNotifier(0);
  ref.listen(sessionProvider, (_, _) => changement.value++);
  ref.onDispose(changement.dispose);

  return GoRouter(
    initialLocation: '/demarrage',
    refreshListenable: changement,
    redirect: (context, etat) =>
        redirection(ref.read(sessionProvider), etat.matchedLocation),
    routes: [
      GoRoute(path: '/demarrage', builder: (_, _) => const EcranDemarrage()),
      GoRoute(path: '/site', builder: (_, _) => const EcranSite()),
      GoRoute(
        path: '/connexion',
        builder: (_, etat) => EcranConnexion(message: etat.extra as String?),
        routes: [
          GoRoute(
            path: 'code',
            builder: (_, etat) =>
                EcranCodeDeuxFacteurs(mfaToken: etat.extra as String? ?? ''),
          ),
        ],
      ),
      GoRoute(
        path: '/inscription',
        builder: (_, _) => const EcranInscription(),
        routes: [
          GoRoute(
            path: 'code',
            redirect: (_, etat) =>
                etat.extra is InscriptionAttente ? null : '/inscription',
            builder: (_, etat) => EcranVerificationEmail(
              attente: etat.extra! as InscriptionAttente,
            ),
          ),
        ],
      ),
      GoRoute(
        path: '/mot-de-passe-oublie',
        builder: (_, _) => const EcranMotDePasseOublie(),
      ),
      GoRoute(
        path: '/c/:jeton',
        builder: (_, etat) =>
            EcranCartePublique(jeton: etat.pathParameters['jeton']!),
      ),
      GoRoute(
        path: '/invitation/:jeton',
        builder: (_, etat) =>
            EcranInvitation(jeton: etat.pathParameters['jeton']!),
      ),
      GoRoute(
        path: '/carte/modifier',
        builder: (_, etat) => EcranEditionCarte(
          organisationId: etat.uri.queryParameters['organisation'],
        ),
      ),
      GoRoute(
        path: '/carte/qr',
        redirect: (_, etat) => etat.extra is Carte ? null : '/carte',
        builder: (_, etat) => EcranQrPleinEcran(carte: etat.extra! as Carte),
      ),
      StatefulShellRoute.indexedStack(
        builder: (_, _, navigation) => Coquille(navigation: navigation),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/carte',
                builder: (_, etat) => EcranMaCarte(
                  organisationId: etat.uri.queryParameters['organisation'],
                ),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/scanner',
                builder: (_, _) => const EcranScanner(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(path: '/carnet', builder: (_, _) => const EcranCarnet()),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/organisations',
                builder: (_, _) => const EcranOrganisation(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/parametres',
                builder: (_, _) => const EcranParametres(),
              ),
            ],
          ),
        ],
      ),
    ],
  );
});
