import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/carte/modeles/carte.dart';
import 'package:madeqrcard/modules/carte/widgets/carte_identite.dart';

Widget _dans(Widget enfant) => MaterialApp(
  home: Scaffold(
    body: Center(child: SizedBox(width: 400, child: enfant)),
  ),
);

void main() {
  const pro = Carte(
    jeton: 'ssEB3YJqJTkL5X',
    numero: 'QRC-2026-00012',
    nom: 'Koné',
    prenom: 'Aïcha',
    profession: 'Directrice',
    contacts: [
      ContactCarte(
        type: 'TELEPHONE',
        valeur: '+2250700000000',
        lien: 'tel:+2250700000000',
      ),
    ],
    organisation: Marque(nom: 'E2E Sarl', couleur: '#0a5c36', adresse: 'Dakar'),
  );

  testWidgets('carte professionnelle : marque, poste et adresse', (
    tester,
  ) async {
    await tester.pumpWidget(_dans(const CarteIdentite(carte: pro)));
    expect(find.text('E2E Sarl'), findsOneWidget);
    expect(find.text('Carte professionnelle'), findsOneWidget);
    expect(find.text('Poste'), findsOneWidget);
    expect(find.text('Directrice'), findsOneWidget);
    expect(find.textContaining('Dakar'), findsOneWidget);
  });

  testWidgets('carte personnelle par defaut', (tester) async {
    await tester.pumpWidget(
      _dans(
        const CarteIdentite(
          carte: Carte(jeton: 'ssEB3YJqJTkL5X', numero: 'QRC-2026-00001'),
          sansQr: true,
        ),
      ),
    );
    expect(find.text('MadeQrCard'), findsOneWidget);
    expect(find.text('Carte personnelle'), findsOneWidget);
    expect(find.text('Profession'), findsOneWidget);
  });

  testWidgets('toucher la carte la retourne', (tester) async {
    await tester.pumpWidget(_dans(const CarteIdentite(carte: pro)));
    expect(find.text('Mes contacts'), findsNothing);
    await tester.tap(find.byType(CarteIdentite));
    await tester.pumpAndSettle();
    expect(find.text('Mes contacts'), findsOneWidget);
    expect(find.text('Appeler'), findsOneWidget);
  });
}
