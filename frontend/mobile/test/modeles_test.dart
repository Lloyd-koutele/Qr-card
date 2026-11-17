import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/carte/modeles/carte.dart';
import 'package:madeqrcard/modules/carte/widgets/carte_identite.dart';

void main() {
  final json = {
    'jeton': 'ssEB3YJqJTkL5X',
    'numero': 'QRC-2026-00012',
    'nom': 'Koné',
    'prenom': 'Aïcha',
    'profession': 'Architecte',
    'bio': null,
    'photoVersion': 1794028405042,
    'contacts': [
      {
        'type': 'WHATSAPP',
        'libelle': 'WhatsApp',
        'valeur': '+2250700000000',
        'lien': 'https://wa.me/2250700000000',
      },
    ],
    'creeLe': '2026-11-07T05:11:44.933Z',
    'proprietaire': true,
    'enregistree': false,
    'connecte': true,
    'enregistrements': 3,
    'organisation': {
      'nom': 'E2E Sarl',
      'couleur': '#0a5c36',
      'adresse': 'Dakar',
      'logo': null,
    },
  };

  test('lit la reponse du serveur', () {
    final c = Carte.depuisJson(json);
    expect(c.nomComplet, 'Aïcha Koné');
    expect(
      c.cheminPhoto,
      '/api/public/cartes/ssEB3YJqJTkL5X/photo?v=1794028405042',
    );
    expect(c.contacts.single.lien, 'https://wa.me/2250700000000');
    expect(c.organisation!.nom, 'E2E Sarl');
    expect(c.enregistrements, 3);
  });

  test('carte personnelle sans organisation ni photo', () {
    final c = Carte.depuisJson({
      ...json,
      'organisation': null,
      'photoVersion': null,
    });
    expect(c.organisation, isNull);
    expect(c.cheminPhoto, isNull);
  });

  test('ligne MRZ identique au site', () {
    final c = Carte.depuisJson(json);
    expect(ligneMrz(c), 'QRC<<KONE<<AICHA<<<<<<<<<<<<<<202600012');
  });

  test('couleurs de l\'organisation', () {
    final c = CouleursCarte.pour(const Marque(nom: 'X', couleur: '#0a5c36'));
    expect(c.principale, couleurHex('#0a5c36'));
    expect(CouleursCarte.pour(null).principale, CouleursCarte.rouge.principale);
  });

  test('couleur choisie pour une carte personnelle', () {
    const perso = Carte(jeton: 'j', numero: 'n', couleur: '#1f3a93');
    expect(CouleursCarte.deCarte(perso).principale, couleurHex('#1f3a93'));
    // Vide : rouge par defaut
    const parDefaut = Carte(jeton: 'j', numero: 'n', couleur: '');
    expect(
      CouleursCarte.deCarte(parDefaut).principale,
      CouleursCarte.rouge.principale,
    );
    // Professionnelle : l'organisation l'emporte
    const pro = Carte(
      jeton: 'j',
      numero: 'n',
      couleur: '#1f3a93',
      organisation: Marque(nom: 'X', couleur: '#0a5c36'),
    );
    expect(CouleursCarte.deCarte(pro).principale, couleurHex('#0a5c36'));
  });
}
