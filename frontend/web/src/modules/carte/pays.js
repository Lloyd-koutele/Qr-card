import PAYS from './pays.json'

export { PAYS }

/** Proposes en tete de liste, avant la recherche. */
export const PAYS_FREQUENTS = ['SN', 'CI', 'ML', 'BF', 'GN', 'BJ', 'TG', 'NE', 'CM', 'GA', 'CG', 'CD', 'MA', 'FR']
export const PAYS_DEFAUT = 'SN'

const parIso = new Map(PAYS.map((p) => [p.iso, p]))
export const pays = (iso) => parIso.get(iso) ?? parIso.get(PAYS_DEFAUT)

/** Drapeau emoji a partir du code ISO (SN -> 🇸🇳). */
export const drapeau = (iso) => String.fromCodePoint(...[...iso].map((c) => 0x1f1a5 + c.charCodeAt(0)))

const sansAccents = (s) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

/** Recherche par nom ("senegal", "cote"), code ISO ("sn") ou indicatif ("221", "+221"). */
export function chercherPays(texte) {
  const q = sansAccents(texte.trim())
  if (!q) {
    const frequents = PAYS_FREQUENTS.map((iso) => parIso.get(iso))
    return [...frequents, ...PAYS.filter((p) => !PAYS_FREQUENTS.includes(p.iso))]
  }
  const chiffres = q.replace(/^\+|^00/, '')
  if (/^\d+$/.test(chiffres)) {
    return PAYS.filter((p) => p.indicatif.startsWith(chiffres))
      .sort((a, b) => a.indicatif.length - b.indicatif.length || b.principal - a.principal)
  }
  const nom = (p) => sansAccents(p.nom)
  return PAYS.filter((p) => nom(p).includes(q) || p.iso.toLowerCase() === q)
    .sort((a, b) => nom(b).startsWith(q) - nom(a).startsWith(q))
}

/** Pays d'un numero international : indicatif le plus long, pays principal en priorite. */
export function paysDuNumero(numero) {
  const chiffres = (numero ?? '').replace(/\D/g, '')
  for (let n = 3; n >= 1; n--) {
    const candidats = PAYS.filter((p) => p.indicatif === chiffres.slice(0, n))
    if (candidats.length) return candidats.find((p) => p.principal) ?? candidats[0]
  }
  return null
}

/** Chiffres saisis -> numero national (sans le 0 de prefixe national quand il est en trop). */
export function numeroNational(p, saisie) {
  let chiffres = saisie.replace(/\D/g, '')
  if (p.prefixe && chiffres.startsWith(p.prefixe)
      && p.longueurs.includes(chiffres.length - p.prefixe.length) && !p.longueurs.includes(chiffres.length)) {
    chiffres = chiffres.slice(p.prefixe.length)
  }
  return chiffres.slice(0, Math.max(...p.longueurs) + p.prefixe.length)
}

export const longueursTexte = (p) => {
  const l = p.longueurs
  return l.length === 1 ? `${l[0]}` : `${l.slice(0, -1).join(', ')} ou ${l[l.length - 1]}`
}

export const numeroComplet = (p, national) => p.longueurs.includes(national.length)
