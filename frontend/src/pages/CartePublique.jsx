import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { IconAddressBook, IconBookmark, IconBookmarkFilled, IconRotate } from '@tabler/icons-react'
import { api, session } from '../api'
import CarteIdentite from '../components/CarteIdentite'
import { cibleLien, infoContact } from '../components/contacts'

/** Lecture seule d'une carte par son lien (QR code). */
export default function CartePublique() {
  const { jeton } = useParams()
  const [carte, setCarte] = useState(null)
  const [erreur, setErreur] = useState('')
  const [retournee, setRetournee] = useState(false)
  const [note, setNote] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    setCarte(null)
    setErreur('')
    api(`/api/public/cartes/${encodeURIComponent(jeton)}`)
      .then(setCarte)
      .catch((e) => setErreur(e.statut === 404 ? 'indisponible' : e.message))
  }, [jeton])

  if (erreur === 'indisponible') {
    return (
      <div className="etroit" style={{ textAlign: 'center', paddingTop: 40 }}>
        <h1>Carte indisponible</h1>
        <p className="sous-titre">Ce lien n'existe pas ou n'est plus actif.</p>
        <Link to="/" className="bouton">Découvrir QR Card</Link>
      </div>
    )
  }
  if (erreur) return <div className="alerte alerte-erreur">{erreur}</div>
  if (!carte) return <div className="chargement">Chargement…</div>

  const enregistrer = async () => {
    try {
      await api('/api/carnet', { methode: 'POST', corps: { jeton, note } })
      setCarte({ ...carte, enregistree: true })
      setMessage('Carte ajoutée à votre carnet')
    } catch (e) {
      setMessage(e.message)
    }
  }

  const memoriserRetour = () => session.ecrire('qrcard_retour', `/c/${jeton}`)
  const nomComplet = [carte.prenom, carte.nom].filter(Boolean).join(' ')

  return (
    <div className="page-carte">
      <CarteIdentite carte={carte} retournee={retournee} surRetourner={() => setRetournee(!retournee)} />
      <div className="ligne ecarte">
        <button className="bouton bouton-petit" onClick={() => setRetournee(!retournee)}>
          <IconRotate size={16} /> Retourner la carte
        </button>
        <span className="discret">Lecture seule</span>
      </div>

      <div>
        <h1>{nomComplet || 'Carte personnelle'}</h1>
        {carte.profession && <p className="sous-titre" style={{ marginBottom: 8 }}>{carte.profession}</p>}
        {carte.bio && <p style={{ marginTop: 0 }}>{carte.bio}</p>}
      </div>

      {carte.contacts.length > 0 && (
        <div className="liste-contacts">
          {carte.contacts.map((c, i) => {
            const info = infoContact(c.type)
            const Icone = info.icone
            return (
              <a key={i} className="gros-bouton" href={c.lien} {...cibleLien(c.lien)}>
                <Icone size={22} /> {info.action ?? info.libelle}
              </a>
            )
          })}
        </div>
      )}

      <a className="bouton bouton-principal" href={`/api/public/cartes/${jeton}/vcard`}>
        <IconAddressBook size={18} /> Ajouter aux contacts du téléphone
      </a>

      <section className="panneau pile">
        {carte.proprietaire ? (
          <>
            <p style={{ margin: 0 }}>C'est votre carte. Voici ce que voient les personnes qui scannent votre QR code.</p>
            <Link to="/ma-carte" className="bouton">Modifier ma carte</Link>
          </>
        ) : carte.enregistree ? (
          <>
            <p style={{ margin: 0 }}><IconBookmarkFilled size={16} style={{ verticalAlign: -3, color: 'var(--accent)' }} /> Cette carte est dans votre carnet.</p>
            <Link to="/carnet" className="bouton">Ouvrir mon carnet</Link>
          </>
        ) : carte.connecte ? (
          <>
            <label>Note personnelle (facultatif)
              <input value={note} maxLength={120} onChange={(e) => setNote(e.target.value)} placeholder="Rencontré au salon de l'habitat" />
            </label>
            <button className="bouton" onClick={enregistrer}><IconBookmark size={18} /> Enregistrer dans mon carnet</button>
          </>
        ) : (
          <>
            <p style={{ margin: 0 }}>Créez un compte gratuit pour garder cette carte et la retrouver plus tard.</p>
            <div className="ligne">
              <Link to="/inscription" className="bouton" onClick={memoriserRetour}>Créer un compte</Link>
              <Link to="/connexion" className="bouton" onClick={memoriserRetour}>Se connecter</Link>
            </div>
          </>
        )}
        {message && <div className="alerte alerte-info">{message}</div>}
      </section>
    </div>
  )
}
