import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../commun/api'
import { useCompteARebours } from './useCompteARebours'

const DELAI_RENVOI = 60

export default function MotDePasseOublie() {
  const [email, setEmail] = useState('')
  const [codeEnvoye, setCodeEnvoye] = useState(false)
  const [form, setForm] = useState({ code: '', nouveau: '', confirmation: '' })
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [restant, setRestant] = useCompteARebours()
  const naviguer = useNavigate()

  const changer = (champ) => (e) => setForm({ ...form, [champ]: e.target.value })

  const demander = async (e) => {
    e?.preventDefault()
    if (!email.trim()) return setErreur('Saisissez votre email')
    setErreur('')
    setEnvoi(true)
    try {
      await api('/api/auth/mot-de-passe/oublie', { methode: 'POST', corps: { email } })
      setCodeEnvoye(true)
      setRestant(DELAI_RENVOI)
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  const reinitialiser = async (e) => {
    e.preventDefault()
    if (!form.code.trim()) return setErreur('Saisissez le code reçu par email')
    if (form.nouveau.length < 8) return setErreur('Le mot de passe doit faire au moins 8 caractères')
    if (form.nouveau !== form.confirmation) return setErreur('Les deux mots de passe ne correspondent pas')
    setErreur('')
    setEnvoi(true)
    try {
      await api('/api/auth/mot-de-passe/reinitialiser', {
        methode: 'POST',
        corps: { email, code: form.code, nouveau: form.nouveau },
      })
      naviguer('/connexion', { replace: true, state: { message: 'Mot de passe modifié. Vous pouvez vous connecter.' } })
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  if (!codeEnvoye) {
    return (
      <div className="etroit">
        <h1>Mot de passe oublié</h1>
        <p className="sous-titre">Saisissez votre email : nous vous enverrons un code à 6 chiffres.</p>
        <form className="panneau pile" onSubmit={demander} noValidate>
          {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
          <label>Email
            <input type="email" autoComplete="email" autoFocus value={email}
                   onChange={(e) => setEmail(e.target.value)} placeholder="nom@exemple.com" />
          </label>
          <button className="bouton bouton-principal bouton-large" disabled={envoi}>
            {envoi ? 'Envoi…' : 'Recevoir un code'}
          </button>
          <p className="discret" style={{ margin: 0, textAlign: 'center' }}>
            <Link to="/connexion">Retour à la connexion</Link>
          </p>
        </form>
      </div>
    )
  }

  return (
    <div className="etroit">
      <h1>Nouveau mot de passe</h1>
      <p className="sous-titre">Si un compte existe pour {email}, un code vient d'y être envoyé. Pensez à regarder dans les spams.</p>
      <form className="panneau pile" onSubmit={reinitialiser} noValidate>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <label>Code
          <input inputMode="numeric" autoComplete="one-time-code" autoFocus value={form.code}
                 onChange={changer('code')} placeholder="123456" maxLength={6} />
        </label>
        <label>Nouveau mot de passe (8 caractères minimum)
          <input type="password" autoComplete="new-password" value={form.nouveau} onChange={changer('nouveau')} />
        </label>
        <label>Confirmer le mot de passe
          <input type="password" autoComplete="new-password" value={form.confirmation} onChange={changer('confirmation')} />
        </label>
        <button className="bouton bouton-principal bouton-large" disabled={envoi}>
          {envoi ? 'Enregistrement…' : 'Changer le mot de passe'}
        </button>
        <button type="button" className="bouton bouton-large" onClick={demander} disabled={restant > 0 || envoi}>
          {restant > 0 ? `Renvoyer le code (${restant} s)` : 'Renvoyer le code'}
        </button>
      </form>
    </div>
  )
}
