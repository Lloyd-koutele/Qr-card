import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/carte/modeles/pays.dart';
import 'package:madeqrcard/modules/carte/widgets/champ_telephone.dart';

void main() {
  Future<(TextEditingController, Pays Function())> monter(
    WidgetTester tester,
  ) async {
    final numero = TextEditingController();
    var pays = paysDe('SN');
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: StatefulBuilder(
            builder: (context, setState) => ChampTelephone(
              pays: pays,
              numero: numero,
              surPays: (p) => setState(() => pays = p),
            ),
          ),
        ),
      ),
    );
    return (numero, () => pays);
  }

  testWidgets('limite au nombre de chiffres du pays', (tester) async {
    final (numero, _) = await monter(tester);
    await tester.enterText(find.byType(TextField), '77 123 45 67 89');
    await tester.pump();
    expect(numero.text, '771234567');
    expect(find.text('Sénégal : 9 chiffres · 9 saisis'), findsOneWidget);
  });

  testWidgets('un indicatif tape change le pays', (tester) async {
    final (numero, pays) = await monter(tester);
    for (final t in ['+', '+3', '+33', '+3306', '+33 06 12 34 56 78']) {
      await tester.enterText(find.byType(TextField), t);
      await tester.pump();
    }
    expect(pays().iso, 'FR');
    expect(numero.text, '612345678');
  });

  testWidgets('choix du pays par la recherche', (tester) async {
    final (_, pays) = await monter(tester);
    await tester.tap(find.text('+221'));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, 'Pays ou indicatif (Sénégal, 221…)'),
      'cote',
    );
    await tester.pump();
    await tester.tap(find.text('Côte d’Ivoire'));
    await tester.pumpAndSettle();
    expect(pays().iso, 'CI');
    expect(find.text('+225'), findsOneWidget);
  });
}
