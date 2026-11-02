import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { session } from '../api'
import { destination, useAuth } from '../auth'

/** Le serveur renvoie ici apres Google, avec le jeton dans le fragment (#token=... ou #mfa=...). */
export default function RetourGoogle() {
  const { connecter } = useAuth()
  const naviguer = useNavigate()
  const traite = useRef(false)

  useEffect(() => {
    // StrictMode execute l'effet deux fois en developpement : le fragment n'est lu qu'une fois
    if (traite.current) return
    traite.current = true
    const params = new URLSearchParams(window.location.hash.slice(1))
    window.history.replaceState(null, '', window.location.pathname)
    const token = params.get('token')
    const mfa = params.get('mfa')
    if (mfa) {
      session.ecrire('qrcard_mfa', mfa)
      naviguer('/connexion/code', { replace: true })
    } else if (token) {
      connecter(token).then((profil) => naviguer(destination(profil), { replace: true }))
    } else {
      naviguer('/connexion?erreur=google', { replace: true })
    }
  }, [connecter, naviguer])

  return <div className="chargement">Connexion…</div>
}
