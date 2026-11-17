import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/app/routeur.dart';
import 'package:madeqrcard/modules/compte/modeles/profil.dart';
import 'package:madeqrcard/modules/compte/session.dart';

Profil _profil({String role = 'USER', bool securiteRequise = false}) => Profil(
  id: '1',
  email: 'a@exemple.test',
  nomAffiche: 'A',
  role: role,
  totpActif: false,
  aMotDePasse: true,
  googleLie: false,
  changerMotDePasse: false,
  securiteRequise: securiteRequise,
);

void main() {
  test('sans compte : seules les pages de connexion', () {
    const s = EtatSession();
    expect(redirection(s, '/carte'), '/connexion');
    expect(redirection(s, '/c/ssEB3YJqJTkL5X'), '/connexion');
    expect(redirection(s, '/inscription/code'), isNull);
    expect(redirection(s, '/mot-de-passe-oublie'), isNull);
  });

  test('connecte : les pages de connexion menent a la carte', () {
    final s = EtatSession(profil: _profil());
    expect(redirection(s, '/connexion'), '/carte');
    expect(redirection(s, '/demarrage'), '/carte');
    expect(redirection(s, '/carnet'), isNull);
  });

  test("l'administrateur general est renvoye vers le site", () {
    expect(
      redirection(EtatSession(profil: _profil(role: 'SUPER_ADMIN')), '/carte'),
      '/site',
    );
    expect(
      redirection(
        EtatSession(profil: _profil(securiteRequise: true)),
        '/carnet',
      ),
      '/site',
    );
  });

  test('chargement ou serveur injoignable : ecran de demarrage', () {
    expect(
      redirection(const EtatSession(chargement: true), '/carte'),
      '/demarrage',
    );
    expect(
      redirection(const EtatSession(erreur: 'Serveur injoignable'), '/carte'),
      '/demarrage',
    );
  });
}
