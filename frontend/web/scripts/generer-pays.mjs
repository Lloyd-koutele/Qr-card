// Genere la liste des pays (nom francais, indicatif, longueurs, exemple) utilisee par le
// site et l'application mobile. A relancer apres une mise a jour de libphonenumber-js :
//   npm run pays
import { writeFileSync } from 'node:fs'
import { getCountries, getCountryCallingCode, getExampleNumber, Metadata } from 'libphonenumber-js/max'
import exemples from 'libphonenumber-js/mobile/examples'

const noms = new Intl.DisplayNames(['fr'], { type: 'region' })
const meta = new Metadata()

const pays = getCountries().map((iso) => {
  meta.selectNumberingPlan(iso)
  const plan = meta.numberingPlan
  const indicatif = getCountryCallingCode(iso)
  return {
    iso,
    nom: noms.of(iso),
    indicatif,
    // Pays principal de l'indicatif (+1 : Etats-Unis, +7 : Russie...)
    principal: meta.getCountryCodesForCallingCode(indicatif)[0] === iso,
    longueurs: plan.possibleLengths(),
    prefixe: typeof plan.nationalPrefix() === 'string' ? plan.nationalPrefix() : '',
    exemple: getExampleNumber(iso, exemples)?.nationalNumber ?? '',
  }
}).sort((a, b) => a.nom.localeCompare(b.nom, 'fr'))

writeFileSync(new URL('../src/modules/carte/pays.json', import.meta.url), JSON.stringify(pays) + '\n')

const dart = `// Genere par frontend/web/scripts/generer-pays.mjs (npm run pays) : ne pas modifier a la main.
import 'pays.dart';

const listePays = <Pays>[
${pays.map((p) => `  Pays('${p.iso}', ${JSON.stringify(p.nom).replaceAll('$', '\\$')}, '${p.indicatif}', [${p.longueurs.join(', ')}], '${p.prefixe}', '${p.exemple}', principal: ${p.principal}),`).join('\n')}
];
`
writeFileSync(new URL('../../mobile/lib/modules/carte/modeles/liste_pays.dart', import.meta.url), dart)
console.log(`${pays.length} pays`)
