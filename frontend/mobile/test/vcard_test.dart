import 'package:flutter_test/flutter_test.dart';
import 'package:madeqrcard/modules/carte/donnees/carte_api.dart';

void main() {
  test('nom de fichier vCard lisible, sans accents', () {
    expect(nomFichierVcard("Aïcha Koné / N'Diaye"), 'Aicha Kone NDiaye.vcf');
    expect(nomFichierVcard('Élodie Ébé'), 'Elodie Ebe.vcf');
    expect(nomFichierVcard(''), 'contact.vcf');
  });
}
