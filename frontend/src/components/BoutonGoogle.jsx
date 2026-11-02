import { useEffect, useState } from 'react'
import { IconBrandGoogle } from '@tabler/icons-react'
import { api } from '../api'

/** Affiche le bouton seulement si le serveur a ete configure avec un identifiant Google. */
export default function BoutonGoogle() {
  const [actif, setActif] = useState(false)

  useEffect(() => {
    api('/api/auth/config').then((c) => setActif(c.googleActif)).catch(() => setActif(false))
  }, [])

  if (!actif) return null
  return (
    <>
      <a className="bouton bouton-large" href="/oauth2/authorization/google">
        <IconBrandGoogle size={18} /> Continuer avec Google
      </a>
      <div className="separateur">ou</div>
    </>
  )
}
