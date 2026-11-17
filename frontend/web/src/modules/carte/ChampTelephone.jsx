import { useEffect, useRef, useState } from 'react'
import { IconChevronDown, IconSearch } from '@tabler/icons-react'
import {
  chercherPays, drapeau, longueursTexte, numeroComplet, numeroNational, pays, paysDuNumero, PAYS_DEFAUT, PAYS_FREQUENTS,
} from './pays'

/**
 * Numero de telephone ou WhatsApp : choix du pays (recherche par nom ou indicatif) puis
 * numero national, limite au nombre de chiffres du pays. La valeur est au format +221771234567.
 */
export default function ChampTelephone({ valeur, onChange }) {
  const [choisi, setChoisi] = useState(null)
  const [ouvert, setOuvert] = useState(false)
  const [recherche, setRecherche] = useState('')
  // Indicatif en cours de frappe (+2, +22...) : affiche tel quel jusqu'a reconnaitre le pays.
  const [indicatifTape, setIndicatifTape] = useState(null)
  const boite = useRef(null)
  const champRecherche = useRef(null)
  const champNumero = useRef(null)

  // Pays choisi tant qu'il correspond a la valeur (+1 : Canada ou Etats-Unis), sinon deduit du numero.
  const p = valeur
    ? (choisi && valeur.startsWith(`+${pays(choisi).indicatif}`) ? pays(choisi) : (paysDuNumero(valeur) ?? pays(choisi)))
    : pays(choisi ?? PAYS_DEFAUT)
  const national = valeur ? valeur.replace(/\D/g, '').slice(p.indicatif.length) : ''

  useEffect(() => {
    if (!ouvert) return undefined
    champRecherche.current?.focus()
    const fermer = (e) => { if (!boite.current?.contains(e.target)) setOuvert(false) }
    document.addEventListener('mousedown', fermer)
    return () => document.removeEventListener('mousedown', fermer)
  }, [ouvert])

  const emettre = (q, chiffres) => onChange(chiffres ? `+${q.indicatif}${chiffres}` : '')

  const choisir = (q) => {
    setChoisi(q.iso)
    setOuvert(false)
    setRecherche('')
    emettre(q, national ? numeroNational(q, national) : '')
    champNumero.current?.focus()
  }

  const saisir = (texte) => {
    // Numero colle avec son indicatif (+225..., 00225...) : le pays suit.
    const international = texte.trim().match(/^(\+|00)/)
    if (international) {
      const q = paysDuNumero(texte.trim().slice(international[0].length))
      if (!q) return setIndicatifTape(texte.trim())
      setIndicatifTape(null)
      setChoisi(q.iso)
      const reste = texte.replace(/\D/g, '').slice(international[0] === '00' ? 2 : 0).slice(q.indicatif.length)
      return emettre(q, numeroNational(q, reste))
    }
    setIndicatifTape(null)
    emettre(p, numeroNational(p, texte))
  }

  const resultats = ouvert ? chercherPays(recherche) : []
  const complet = numeroComplet(p, national)

  return (
    <div className="champ-telephone" ref={boite}>
      <div className="champ-telephone-saisie">
        <button type="button" className="champ-telephone-pays" onClick={() => setOuvert(!ouvert)}
                aria-haspopup="listbox" aria-expanded={ouvert} aria-label={`Pays : ${p.nom}, +${p.indicatif}`}>
          <span className="drapeau" aria-hidden="true">{drapeau(p.iso)}</span>
          +{p.indicatif}
          <IconChevronDown size={14} aria-hidden="true" />
        </button>
        <input ref={champNumero} type="tel" inputMode="tel" autoComplete="tel-national" value={indicatifTape ?? national}
               placeholder={p.exemple}
               onChange={(e) => saisir(e.target.value)} aria-label={`Numéro (${longueursTexte(p)} chiffres)`}
               aria-invalid={national !== '' && !complet} />
      </div>
      <span className={`champ-telephone-aide${national && !complet ? ' incomplet' : ''}`}>
        {p.nom} : {longueursTexte(p)} chiffres{national ? ` · ${national.length} saisi${national.length > 1 ? 's' : ''}` : ''}
      </span>

      {ouvert && (
        <div className="champ-telephone-liste">
          <div className="champ-telephone-recherche">
            <IconSearch size={16} aria-hidden="true" />
            <input ref={champRecherche} value={recherche} placeholder="Pays ou indicatif (Sénégal, 221…)"
                   aria-label="Rechercher un pays"
                   onChange={(e) => setRecherche(e.target.value)}
                   onKeyDown={(e) => {
                     if (e.key === 'Escape') { e.preventDefault(); setOuvert(false) }
                     if (e.key === 'Enter') { e.preventDefault(); if (resultats[0]) choisir(resultats[0]) }
                   }} />
          </div>
          <ul role="listbox" aria-label="Pays">
            {resultats.map((q, i) => (
              <li key={q.iso} role="option" aria-selected={q.iso === p.iso}
                  className={!recherche && i === PAYS_FREQUENTS.length ? 'separe' : undefined}>
                <button type="button" onClick={() => choisir(q)}>
                  <span className="drapeau" aria-hidden="true">{drapeau(q.iso)}</span>
                  <span className="nom">{q.nom}</span>
                  <span className="indicatif">+{q.indicatif}</span>
                </button>
              </li>
            ))}
            {resultats.length === 0 && <li className="vide">Aucun pays trouvé</li>}
          </ul>
        </div>
      )}
    </div>
  )
}
