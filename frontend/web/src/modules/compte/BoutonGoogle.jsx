import { useEffect, useState } from 'react'
import { IconBrandGoogle } from '@tabler/icons-react'
import { api } from '../../commun/api'

const ADRESSE_GOOGLE = '/oauth2/authorization/google'

/**
 * Affiche le bouton seulement si le serveur a ete configure avec un identifiant Google.
 * avantDepart : appele au clic ; renvoyer false annule le depart (formulaire incomplet par exemple).
 */
export default function BoutonGoogle({ avantDepart }) {
  const [actif, setActif] = useState(false)

  useEffect(() => {
    api('/api/auth/config').then((c) => setActif(c.googleActif)).catch(() => setActif(false))
  }, [])

  const partir = (e) => {
    if (avantDepart && avantDepart() === false) e.preventDefault()
  }

  if (!actif) return null
  return (
    <>
      <a className="bouton bouton-large" href={ADRESSE_GOOGLE} onClick={partir}>
        <IconBrandGoogle size={18} /> Continuer avec Google
      </a>
      <div className="separateur">ou</div>
    </>
  )
}
