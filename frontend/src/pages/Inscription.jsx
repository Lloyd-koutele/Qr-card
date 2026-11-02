import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { destination, useAuth } from '../auth'
import BoutonGoogle from '../components/BoutonGoogle'

export default function Inscription() {
  const [form, setForm] = useState({ nomAffiche: '', email: '', motDePasse: '', confirmation: '' })
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const { connecter } = useAuth()
  const naviguer = useNavigate()

  const changer = (champ) => (e) => setForm({ ...form, [champ]: e.target.value })

  const soumettre = async (e) => {
    e.preventDefault()
    if (!form.nomAffiche.trim() || !form.email.trim()) return setErreur('Saisissez votre nom et votre email')
    if (form.motDePasse.length < 8) return setErreur('Le mot de passe doit faire au moins 8 caractères')
    if (form.motDePasse !== form.confirmation) return setErreur('Les deux mots de passe ne correspondent pas')
    setErreur('')
    setEnvoi(true)
    try {
      const r = await api('/api/auth/inscription', {
        methode: 'POST',
        corps: { nomAffiche: form.nomAffiche, email: form.email, motDePasse: form.motDePasse },
      })
      const profil = await connecter(r.token)
      naviguer(destination(profil), { replace: true })
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="etroit">
      <h1>Créer un compte</h1>
      <p className="sous-titre">Votre carte est créée tout de suite, vous la complétez ensuite.</p>
      <form className="panneau pile" onSubmit={soumettre} noValidate>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <BoutonGoogle />
        <label>Prénom et nom
          <input autoComplete="name" value={form.nomAffiche} onChange={changer('nomAffiche')} placeholder="Aïcha Koné" />
        </label>
        <label>Email
          <input type="email" autoComplete="email" value={form.email} onChange={changer('email')} placeholder="nom@exemple.com" />
        </label>
        <label>Mot de passe (8 caractères minimum)
          <input type="password" autoComplete="new-password" value={form.motDePasse} onChange={changer('motDePasse')} />
        </label>
        <label>Confirmer le mot de passe
          <input type="password" autoComplete="new-password" value={form.confirmation} onChange={changer('confirmation')} />
        </label>
        <button className="bouton bouton-principal bouton-large" disabled={envoi}>
          {envoi ? 'Création…' : 'Créer mon compte'}
        </button>
        <p className="discret" style={{ margin: 0, textAlign: 'center' }}>
          Déjà inscrit ? <Link to="/connexion">Se connecter</Link>
        </p>
      </form>
    </div>
  )
}
