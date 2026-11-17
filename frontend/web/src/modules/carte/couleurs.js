/** Couleurs proposees pour une carte personnelle. '' = rouge MadeQrCard (couleur par defaut). */
export const COULEURS_CARTE = [
  { valeur: '', nom: 'Rouge MadeQrCard', apercu: '#c8102e' },
  { valeur: '#1f3a93', nom: 'Bleu nuit' },
  { valeur: '#0f6e6e', nom: 'Turquoise' },
  { valeur: '#1e6b3a', nom: 'Vert forêt' },
  { valeur: '#5b2a86', nom: 'Violet' },
  { valeur: '#a3214f', nom: 'Framboise' },
  { valeur: '#b4461b', nom: 'Orange brûlé' },
  { valeur: '#8a6a12', nom: 'Or' },
  { valeur: '#3a3a3a', nom: 'Anthracite' },
]

const TEXTE_FONCE = '#1d1d1b'

/** Luminance relative (WCAG) d'une couleur #rrggbb. */
const luminance = (hex) => {
  const [r, v, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * v + 0.0722 * b
}

/** Texte pose sur la couleur : blanc ou fonce, celui qui contraste le plus. Toute couleur reste donc lisible. */
export const texteSur = (hex) => {
  const l = luminance(hex)
  return 1.05 / (l + 0.05) >= (l + 0.05) / (luminance(TEXTE_FONCE) + 0.05) ? '#ffffff' : TEXTE_FONCE
}
