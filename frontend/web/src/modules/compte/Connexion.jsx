import { useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { api, session } from '../../commun/api'
import { destination, useAuth } from './auth'
import BoutonGoogle from './BoutonGoogle'

const ERREURS_GOOGLE = {
  google: 'La connexion avec Google a échoué. Réessayez.',
  'compte-existant': 'Cet email est déjà lié à un autre compte Google.',
  'email-non-verifie': "Votre adresse Google n'est pas vérifiée.",
  bloque: "Ce compte est bloqué. Contactez l'administrateur.",
}

export default function Connexion() {
  const [params] = useSearchParams()
  const message = useLocation().state?.message
  const [email, setEmail] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [erreur, setErreur] = useState(ERREURS_GOOGLE[params.get('erreur')] ?? '')
  const [envoi, setEnvoi] = useState(false)
  const { connecter } = useAuth()
  const naviguer = useNavigate()

  const soumettre = async (e) => {
    e.preventDefault()
    if (!email.trim() || !motDePasse) {
      setErreur('Saisissez votre email et votre mot de passe')
      return
    }
    setErreur('')
    setEnvoi(true)
    try {
      const r = await api('/api/auth/connexion', { methode: 'POST', corps: { email, motDePasse } })
      if (r.mfaRequis) {
        session.ecrire('madeqrcard_mfa', r.mfaToken)
        naviguer('/connexion/code')
      } else {
        const profil = await connecter(r.token)
        naviguer(destination(profil), { replace: true })
      }
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="etroit">
      <h1>Connexion</h1>
      <p className="sous-titre">Accédez à votre carte et à votre carnet.</p>
      <form className="panneau pile" onSubmit={soumettre} noValidate>
        {message && !erreur && <div className="alerte alerte-succes">{message}</div>}
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <BoutonGoogle />
        <label>Email
          <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="nom@exemple.com" />
        </label>
        <label>Mot de passe
          <input type="password" autoComplete="current-password" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} />
        </label>
        <Link to="/mot-de-passe-oublie" className="discret" style={{ alignSelf: 'flex-end' }}>Mot de passe oublié ?</Link>
        <button className="bouton bouton-principal bouton-large" disabled={envoi}>
          {envoi ? 'Connexion…' : 'Se connecter'}
        </button>
        <p className="discret" style={{ margin: 0, textAlign: 'center' }}>
          Pas encore de compte ? <Link to="/inscription">Créer un compte</Link>
        </p>
      </form>
    </div>
  )
}
