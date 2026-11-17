import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { IconMailCheck } from '@tabler/icons-react'
import { api, session } from '../../commun/api'
import { destination, useAuth } from './auth'
import { useCompteARebours } from './useCompteARebours'

const lireDemande = () => {
  try {
    return JSON.parse(session.lire('madeqrcard_inscription'))
  } catch {
    return null
  }
}

export default function VerificationEmail() {
  const [demande] = useState(lireDemande)
  const [code, setCode] = useState('')
  const [erreur, setErreur] = useState('')
  const [info, setInfo] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [restant, setRestant] = useCompteARebours()
  const { connecter } = useAuth()
  const naviguer = useNavigate()

  if (!demande) return <Navigate to="/inscription" replace />

  const expiree = () => {
    session.effacer('madeqrcard_inscription')
    naviguer(demande.origine ?? '/inscription', { replace: true })
  }

  const soumettre = async (e) => {
    e.preventDefault()
    if (!code.trim()) return setErreur('Saisissez le code reçu par email')
    setErreur('')
    setInfo('')
    setEnvoi(true)
    try {
      const r = await api('/api/auth/inscription/verifier', { methode: 'POST', corps: { jeton: demande.jeton, code } })
      session.effacer('madeqrcard_inscription')
      const profil = await connecter(r.token)
      naviguer(destination(profil), { replace: true })
    } catch (err) {
      if (err.statut === 410) return expiree()
      setErreur(err.message)
      setCode('')
    } finally {
      setEnvoi(false)
    }
  }

  const renvoyer = async () => {
    setErreur('')
    setInfo('')
    try {
      const r = await api('/api/auth/inscription/renvoyer', { methode: 'POST', corps: { jeton: demande.jeton } })
      setRestant(r.renvoiDansSecondes)
      setInfo('Un nouveau code vient de vous être envoyé.')
    } catch (err) {
      if (err.statut === 410) return expiree()
      setErreur(err.message)
    }
  }

  return (
    <div className="etroit">
      <h1>Vérifiez votre email</h1>
      <p className="sous-titre">Saisissez le code à 6 chiffres envoyé à {demande.email}. Pensez à regarder dans les spams.</p>
      <form className="panneau pile" onSubmit={soumettre} noValidate>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        {info && <div className="alerte alerte-succes">{info}</div>}
        <label>Code
          <input inputMode="numeric" autoComplete="one-time-code" autoFocus value={code}
                 onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6} />
        </label>
        <button className="bouton bouton-principal bouton-large" disabled={envoi}>
          <IconMailCheck size={18} /> {envoi ? 'Vérification…' : 'Créer mon compte'}
        </button>
        <button type="button" className="bouton bouton-large" onClick={renvoyer} disabled={restant > 0}>
          {restant > 0 ? `Renvoyer le code (${restant} s)` : 'Renvoyer le code'}
        </button>
      </form>
    </div>
  )
}
