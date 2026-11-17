import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/scan/lien_madeqrcard.dart';

void main() {
  test('reconnait une carte, quel que soit le domaine', () {
    final lien = LienMadeQrCard.lire('https://madeqrcard.sn/c/ssEB3YJqJTkL5X');
    expect(lien, isA<LienCarte>());
    expect((lien! as LienCarte).jeton, 'ssEB3YJqJTkL5X');
    expect(
      LienMadeQrCard.lire('http://localhost:8088/c/ssEB3YJqJTkL5X'),
      isA<LienCarte>(),
    );
  });

  test('reconnait une invitation', () {
    final lien = LienMadeQrCard.lire(
      ' https://madeqrcard.sn/invitation/JvcieqqZzgMeFWxVuJgTy9u4v4TvuIOI0_5f62WoW3w ',
    );
    expect(lien, isA<LienInvitation>());
  });

  test('ignore les autres QR codes', () {
    for (final texte in [
      'bonjour',
      'https://exemple.com',
      'https://madeqrcard.sn/c/',
      'https://madeqrcard.sn/c/a/b',
      'https://madeqrcard.sn/x/abcdefghij',
      'https://madeqrcard.sn/c/<script>',
    ]) {
      expect(LienMadeQrCard.lire(texte), isNull, reason: texte);
    }
  });
}
