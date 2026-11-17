import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/carte/modeles/pays.dart';

void main() {
  test('recherche par nom sans accents, code ISO ou indicatif', () {
    expect(chercherPays('senegal').first.iso, 'SN');
    expect(chercherPays('cote').map((p) => p.iso), contains('CI'));
    expect(chercherPays('+225').first.iso, 'CI');
    expect(chercherPays('1').first.iso, 'US');
    expect(chercherPays('').take(2).map((p) => p.iso), ['SN', 'CI']);
  });

  test('pays deduit du numero international', () {
    expect(paysDuNumero('+221771234567')?.iso, 'SN');
    expect(paysDuNumero('+33612345678')?.iso, 'FR');
    expect(paysDuNumero('+15062345678')?.iso, 'US');
    expect(paysDuNumero('+2'), isNull);
  });

  test('numero limite au pays et prefixe national retire', () {
    final sn = paysDe('SN');
    expect(sn.numeroNational('77 123 45 67 89'), '771234567');
    expect(sn.estComplet('771234567'), isTrue);
    expect(sn.international('771234567'), '+221771234567');
    final fr = paysDe('FR');
    expect(fr.numeroNational('06 12 34 56 78'), '612345678');
    expect(paysDe('CI').numeroNational('0707070707'), '0707070707');
  });
}
