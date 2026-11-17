import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, session } from '../../commun/api'
import { useAuth } from '../compte'

/**
 * Page du lien recu par email. Elle propose les memes choix a tout le monde :
 * elle ne dit pas si l'adresse invitee possede deja un compte.
 */
export default function Invitation() {
  const { jeton } = useParams()
  const { profil, chargement, connecter } = useAuth()
  const naviguer = useNavigate()
  const [invitation, setInvitation] = useState(null)
  const [erreurPage, setErreurPage] = useState('')
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [refusee, setRefusee] = useState(false)
  const [creation, setCreation] = useState(false)
  const [nomAffiche, setNomAffiche] = useState('')
  const [motDePasse, setMotDePasse] = useState('')

  const chemin = encodeURIComponent(jeton)

  useEffect(() => {
    api(`/api/public/invitations/${chemin}`)
      .then((i) => {
        setInvitation(i)
        setNomAffiche(i.nom ?? '')
      })
      .catch((e) => setErreurPage(e.message))
  }, [chemin])

  if (erreurPage) {
    return (
      <div className="etroit" style={{ textAlign: 'center', paddingTop: 40 }}>
        <h1>Invitation indisponible</h1>
        <p className="sous-titre">{erreurPage}</p>
        <Link to="/" className="bouton">Découvrir MadeQrCard</Link>
      </div>
    )
  }
  if (!invitation || chargement) return <div className="chargement">Chargement…</div>
  if (refusee) {
    return (
      <div className="etroit" style={{ textAlign: 'center', paddingTop: 40 }}>
        <h1>Invitation refusée</h1>
        <p className="sous-titre">Vous n'êtes pas rattaché à {invitation.organisation}.</p>
        <Link to="/" className="bouton">Retour à l'accueil</Link>
      </div>
    )
  }

  const agir = async (action) => {
    setErreur('')
    setEnvoi(true)
    try {
      await action()
    } catch (e) {
      setErreur(e.message)
    } finally {
      setEnvoi(false)
    }
  }

  const accepter = () => agir(async () => {
    await api(`/api/invitations/${chemin}/accepter`, { methode: 'POST' })
    naviguer(`/organisations/${invitation.organisationId}/carte`, { replace: true })
  })

  const refuser = () => agir(async () => {
    await api(`/api/public/invitations/${chemin}/refuser`, { methode: 'POST' })
    setRefusee(true)
  })

  const creerCompte = (e) => {
    e.preventDefault()
    if (!nomAffiche.trim()) return setErreur('Indiquez votre nom')
    if (motDePasse.length < 8) return setErreur('Le mot de passe doit faire au moins 8 caractères')
    return agir(async () => {
      const r = await api(`/api/public/invitations/${chemin}/compte`, { methode: 'POST', corps: { nomAffiche, motDePasse } })
      await connecter(r.token)
      naviguer(`/organisations/${invitation.organisationId}/carte`, { replace: true })
    })
  }

  return (
    <div className="etroit">
      <div className="entete-organisation">
        {invitation.logo && <img className="logo-organisation" src={invitation.logo} alt="" />}
        <h1>{invitation.organisation}</h1>
      </div>
      <p className="sous-titre">vous invite à créer votre carte professionnelle sur MadeQrCard.</p>

      <section className="panneau pile">
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <p style={{ margin: 0 }}>
          Invitation envoyée à <strong>{invitation.email}</strong>
          {invitation.poste && <> pour le poste de <strong>{invitation.poste}</strong></>}.
        </p>
        <ul className="liste-garanties">
          <li>Rien n'est rattaché à l'organisation tant que vous n'avez pas accepté.</li>
          <li>L'organisation voit cette adresse et votre carte professionnelle.</li>
          <li>Elle ne voit ni l'adresse de votre compte, ni votre carte personnelle, ni votre carnet.</li>
          <li>Si vous partez, seule la carte professionnelle disparaît.</li>
        </ul>

        {profil ? (
          <>
            <p className="discret" style={{ margin: 0 }}>Vous êtes connecté avec {profil.email}.</p>
            <div className="ligne">
              <button className="bouton bouton-principal" disabled={envoi} onClick={accepter}>Accepter l'invitation</button>
              <button className="bouton" disabled={envoi} onClick={refuser}>Refuser</button>
            </div>
          </>
        ) : creation ? (
          <form className="pile" onSubmit={creerCompte} noValidate>
            <label>Votre nom
              <input autoComplete="name" maxLength={120} value={nomAffiche} onChange={(e) => setNomAffiche(e.target.value)} />
            </label>
            <label>Choisissez un mot de passe
              <input type="password" autoComplete="new-password" maxLength={100} value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} />
            </label>
            <div className="ligne">
              <button className="bouton bouton-principal" disabled={envoi}>Créer mon compte et accepter</button>
              <button type="button" className="bouton" onClick={() => setCreation(false)}>Retour</button>
            </div>
          </form>
        ) : (
          <div className="pile">
            <Link to="/connexion" className="bouton bouton-principal bouton-large"
                  onClick={() => session.ecrire('madeqrcard_retour', `/invitation/${jeton}`)}>
              J'ai déjà un compte ou j'utilise Google
            </Link>
            <button className="bouton bouton-large" onClick={() => setCreation(true)}>Créer mon compte</button>
            <button className="bouton bouton-large" disabled={envoi} onClick={refuser}>Refuser l'invitation</button>
          </div>
        )}
      </section>
    </div>
  )
}
