import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, session } from '../../commun/api'
import { BoutonGoogle } from '../compte'
import { Formulaire } from './Organisation'
import { CLE_A_CREER } from './types'

const RETOUR = '/organisations'

/**
 * Creer une organisation sans compte : le compte de l'administrateur est cree en meme temps.
 * Par email, le serveur cree les deux une fois le code saisi ; avec Google, l'organisation
 * est gardee dans l'onglet et creee au retour (voir la page Organisation).
 */
export default function CreationPublique() {
  const [compte, setCompte] = useState({ nomAffiche: '', email: '', motDePasse: '', confirmation: '' })
  const naviguer = useNavigate()

  const changer = (champ) => (e) => setCompte({ ...compte, [champ]: e.target.value })

  const enregistrer = async (organisation) => {
    if (!compte.nomAffiche.trim() || !compte.email.trim()) throw new Error('Saisissez votre nom et votre email')
    if (compte.motDePasse.length < 8) throw new Error('Le mot de passe doit faire au moins 8 caractères')
    if (compte.motDePasse !== compte.confirmation) throw new Error('Les deux mots de passe ne correspondent pas')
    const r = await api('/api/auth/inscription/organisation', {
      methode: 'POST',
      corps: {
        organisation,
        compte: { nomAffiche: compte.nomAffiche, email: compte.email, motDePasse: compte.motDePasse },
      },
    })
    session.ecrire('madeqrcard_inscription', JSON.stringify({ jeton: r.jeton, email: r.email, origine: '/creer-organisation' }))
    session.ecrire('madeqrcard_retour', RETOUR)
    naviguer('/inscription/code')
    return false
  }

  const avecGoogle = (organisation, setErreur) => () => {
    if (!organisation.nom.trim()) {
      setErreur("Indiquez d'abord le nom de l'organisation")
      return false
    }
    session.ecrire(CLE_A_CREER, JSON.stringify(organisation))
    session.ecrire('madeqrcard_retour', RETOUR)
    return true
  }

  return (
    <div>
      <h1>Créer une organisation</h1>
      <p className="sous-titre">
        Entreprise, association, école ou administration : donnez à vos membres une carte professionnelle à vos couleurs.
      </p>
      <section className="panneau">
        <Formulaire
          libelle="Créer l'organisation"
          note="Vous recevrez un code par email pour confirmer votre adresse. Les autres administrateurs la verront."
          enregistrer={enregistrer}
        >
          {(organisation, setErreur) => (
            <>
              <h2 className="titre-compte">Votre compte administrateur</h2>
              <BoutonGoogle avantDepart={avecGoogle(organisation, setErreur)} />
              <label>Prénom et nom
                <input autoComplete="name" value={compte.nomAffiche} onChange={changer('nomAffiche')} placeholder="Aïcha Koné" />
              </label>
              <label>Email
                <input type="email" autoComplete="email" value={compte.email} onChange={changer('email')} placeholder="nom@exemple.com" />
              </label>
              <label>Mot de passe (8 caractères minimum)
                <input type="password" autoComplete="new-password" value={compte.motDePasse} onChange={changer('motDePasse')} />
              </label>
              <label>Confirmer le mot de passe
                <input type="password" autoComplete="new-password" value={compte.confirmation} onChange={changer('confirmation')} />
              </label>
            </>
          )}
        </Formulaire>
        <p className="discret" style={{ margin: '16px 0 0', textAlign: 'center' }}>
          Déjà un compte ? <Link to="/connexion" onClick={() => session.ecrire('madeqrcard_retour', RETOUR)}>Connectez-vous</Link> pour créer l'organisation.
        </p>
      </section>
    </div>
  )
}
