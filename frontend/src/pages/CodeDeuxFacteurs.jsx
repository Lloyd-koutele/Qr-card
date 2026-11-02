import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { IconShieldLock } from '@tabler/icons-react'
import { api, session } from '../api'
import { destination, useAuth } from '../auth'

export default function CodeDeuxFacteurs() {
  const mfaToken = session.lire('madeqrcard_mfa')
  const [code, setCode] = useState('')
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const { connecter } = useAuth()
  const naviguer = useNavigate()

  if (!mfaToken) return <Navigate to="/connexion" replace />

  const soumettre = async (e) => {
    e.preventDefault()
    if (!code.trim()) return setErreur('Saisissez le code affiché dans votre application')
    setErreur('')
    setEnvoi(true)
    try {
      const r = await api('/api/auth/2fa', { methode: 'POST', corps: { mfaToken, code } })
      session.effacer('madeqrcard_mfa')
      const profil = await connecter(r.token)
      naviguer(destination(profil), { replace: true })
    } catch (err) {
      if (err.statut === 401 && err.message.startsWith('Session')) {
        session.effacer('madeqrcard_mfa')
        naviguer('/connexion', { replace: true })
        return
      }
      setErreur(err.message)
      setCode('')
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="etroit">
      <h1>Double authentification</h1>
      <p className="sous-titre">Saisissez le code à 6 chiffres de votre application d'authentification.</p>
      <form className="panneau pile" onSubmit={soumettre} noValidate>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <label>Code
          <input inputMode="numeric" autoComplete="one-time-code" autoFocus value={code}
                 onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={11} />
        </label>
        <button className="bouton bouton-principal bouton-large" disabled={envoi}>
          <IconShieldLock size={18} /> {envoi ? 'Vérification…' : 'Valider'}
        </button>
        <p className="discret" style={{ margin: 0 }}>
          Téléphone perdu ? Saisissez l'un de vos codes de secours (format ABCDE-FGHIJ).
        </p>
      </form>
    </div>
  )
}
