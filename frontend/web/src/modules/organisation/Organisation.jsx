import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { IconChevronRight, IconExternalLink, IconPlus, IconPhoto, IconSend, IconTrash, IconUserMinus } from '@tabler/icons-react'
import { api, session } from '../../commun/api'
import { CarteIdentite, texteSur } from '../carte'
import { useAuth } from '../compte'
import { CLE_A_CREER, libelleType, TYPES_ORGANISATION } from './types'

const TAILLE_MAX_LOGO = 2 * 1024 * 1024
const date = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')
const dateHeure = (iso) => new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
const ACTIONS = {
  CREATION: "Création de l'organisation",
  MODIFICATION: "Modification de l'organisation",
  INVITATION: 'Invitation envoyée',
  ANNULATION_INVITATION: 'Invitation annulée',
  ACCEPTATION: 'Invitation acceptée',
  RETRAIT: 'Membre retiré',
  DEPART: "Départ de l'organisation",
  ROLE_ADMIN: 'Nommé administrateur',
  ROLE_MEMBRE: 'Redevenu membre',
}

/** Les organisations du compte : une carte par organisation, et la creation d'une nouvelle. */
export function MesOrganisations() {
  const [liste, setListe] = useState(undefined)
  const [creation, setCreation] = useState(false)
  const [erreur, setErreur] = useState('')
  const naviguer = useNavigate()

  useEffect(() => {
    let actif = true
    const charger = async () => {
      // Organisation saisie sur la page publique avant une connexion Google
      const aCreer = session.lire(CLE_A_CREER)
      if (aCreer) {
        session.effacer(CLE_A_CREER)
        const o = await api('/api/organisations', { methode: 'POST', corps: JSON.parse(aCreer) })
        if (actif) naviguer(`/organisations/${o.id}`, { replace: true })
        return
      }
      const l = await api('/api/organisations')
      if (actif) setListe(l)
    }
    charger().catch((e) => actif && setErreur(e.message))
    return () => { actif = false }
  }, [naviguer])

  if (erreur) return <div className="alerte alerte-erreur">{erreur}</div>
  if (liste === undefined) return <div className="chargement">Chargement…</div>
  const creer = async (corps) => {
    const o = await api('/api/organisations', { methode: 'POST', corps })
    naviguer(`/organisations/${o.id}`)
    return false
  }
  if (liste.length === 0 || creation) {
    return <Creation surCreer={creer} surAnnuler={liste.length > 0 ? () => setCreation(false) : null} />
  }

  return (
    <div>
      <div className="ligne ecarte" style={{ marginBottom: 4 }}>
        <h1 style={{ margin: 0 }}>Mes organisations</h1>
        <button className="bouton bouton-principal" onClick={() => setCreation(true)}>
          <IconPlus size={16} /> Créer une organisation
        </button>
      </div>
      <p className="sous-titre">Vous avez une carte professionnelle distincte dans chacune.</p>
      <div className="liste-organisations">
        {liste.map((o) => (
          <Link key={o.id} to={`/organisations/${o.id}`} className="panneau tuile-organisation">
            <span className="pastille-organisation" style={{ background: o.couleur, color: texteSur(o.couleur) }}>
              {o.logo ? <img src={o.logo} alt="" /> : o.nom.slice(0, 1).toUpperCase()}
            </span>
            <span className="tuile-texte">
              <strong>{o.nom}</strong>
              <span className="discret">
                {libelleType(o.type, o.typePrecision)} · {o.monRole === 'ADMIN' ? 'administrateur' : 'membre'}
                {o.statut !== 'ACTIVE' && ' · bloquée'}
              </span>
            </span>
            <IconChevronRight size={18} className="discret" />
          </Link>
        ))}
      </div>
    </div>
  )
}

/** Espace d'une organisation : la carte du membre et, pour un admin, la gestion. */
export default function Organisation() {
  const { id } = useParams()
  const [organisation, setOrganisation] = useState(undefined)
  const [erreur, setErreur] = useState('')
  const naviguer = useNavigate()

  useEffect(() => {
    let actif = true
    setOrganisation(undefined)
    setErreur('')
    api(`/api/organisations/${id}`).then((o) => actif && setOrganisation(o)).catch((e) => actif && setErreur(e.message))
    return () => { actif = false }
  }, [id])

  if (erreur) return <div className="alerte alerte-erreur">{erreur} <Link to="/organisations">Mes organisations</Link></div>
  if (organisation === undefined) return <div className="chargement">Chargement…</div>
  // Apres un depart ou une suppression : retour a la liste
  const surChange = (o) => (o ? setOrganisation(o) : naviguer('/organisations', { replace: true }))
  return <Espace organisation={organisation} surChange={surChange} />
}

export function Formulaire({ initial, libelle, enregistrer, note, children }) {
  const [nom, setNom] = useState(initial?.nom ?? '')
  const [type, setType] = useState(initial?.type ?? 'ENTREPRISE')
  const [typePrecision, setTypePrecision] = useState(initial?.typePrecision ?? '')
  const [adresse, setAdresse] = useState(initial?.adresse ?? '')
  const [couleur, setCouleur] = useState(initial?.couleur ?? '#c8102e')
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)

  const soumettre = async (e) => {
    e.preventDefault()
    if (!nom.trim()) return setErreur("Indiquez le nom de l'organisation")
    setErreur('')
    setMessage('')
    setEnvoi(true)
    try {
      const fini = await enregistrer({ nom, type, typePrecision: type === 'AUTRE' ? typePrecision : '', adresse, couleur })
      if (fini !== false) setMessage('Enregistré')
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  const exemple = {
    prenom: 'Aïcha', nom: 'Koné', profession: 'Votre poste', numero: 'QRC-2026-00001',
    creeLe: new Date().toISOString(), contacts: [],
    organisation: { nom: nom || 'Votre organisation', adresse, couleur, logo: initial?.logo },
  }

  return (
    <div className="formulaire-organisation">
      <form className="pile" onSubmit={soumettre} noValidate>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        {message && <div className="alerte alerte-succes">{message}</div>}
        <label>Nom de l'organisation
          <input maxLength={150} value={nom} onChange={(e) => setNom(e.target.value)} />
        </label>
        <label>Type
          <select value={type} onChange={(e) => setType(e.target.value)}>
            {TYPES_ORGANISATION.map((t) => <option key={t.valeur} value={t.valeur}>{t.libelle}</option>)}
          </select>
        </label>
        {type === 'AUTRE' && (
          <label>Précisez le type (facultatif)
            <input maxLength={60} value={typePrecision} onChange={(e) => setTypePrecision(e.target.value)}
                   placeholder="Club de sport, cabinet, collectif…" />
          </label>
        )}
        <label>Adresse (facultatif)
          <input maxLength={255} value={adresse} onChange={(e) => setAdresse(e.target.value)} placeholder="Dakar, Sénégal" />
        </label>
        <label>Couleur des cartes
          <span className="choix-couleur">
            <input type="color" value={couleur} onChange={(e) => setCouleur(e.target.value)} />
            <span className="discret">{couleur}</span>
          </span>
        </label>
        {typeof children === 'function'
          ? children({ nom, type, typePrecision: type === 'AUTRE' ? typePrecision : '', adresse, couleur }, setErreur)
          : children}
        {note && <p className="discret" style={{ margin: 0 }}>{note}</p>}
        <div className="ligne">
          <button className="bouton bouton-principal" disabled={envoi}>{envoi ? 'Enregistrement…' : libelle}</button>
        </div>
      </form>
      <CarteIdentite carte={exemple} sansQr />
    </div>
  )
}

function Creation({ surCreer, surAnnuler }) {
  const { profil } = useAuth()
  return (
    <div>
      {surAnnuler && <button className="bouton bouton-petit" onClick={surAnnuler} style={{ marginBottom: 12 }}>Retour à mes organisations</button>}
      <h1>Créer une organisation</h1>
      <p className="sous-titre">
        Donnez à vos membres une carte professionnelle à vos couleurs.
      </p>
      <section className="panneau">
        <Formulaire
          libelle="Créer l'organisation"
          note={`Les autres administrateurs verront votre adresse : ${profil.email}.`}
          enregistrer={surCreer}
        />
      </section>
    </div>
  )
}

function Espace({ organisation, surChange }) {
  const admin = organisation.monRole === 'ADMIN'
  const bloquee = organisation.statut !== 'ACTIVE'
  const [erreur, setErreur] = useState('')

  const quitter = async () => {
    const ok = window.confirm(
      `Quitter ${organisation.nom} ?\n\nVotre carte professionnelle et son QR code ne fonctionneront plus. Votre compte, votre carte personnelle et votre carnet ne changent pas.`,
    )
    if (!ok) return
    try {
      await api(`/api/organisations/${organisation.id}/quitter`, { methode: 'POST' })
      surChange(null)
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <div>
      <Link to="/organisations" className="discret lien-retour">← Mes organisations</Link>
      <div className="entete-organisation">
        {organisation.logo && <img className="logo-organisation" src={organisation.logo} alt="" />}
        <h1>{organisation.nom}</h1>
      </div>
      <p className="sous-titre">
        {libelleType(organisation.type, organisation.typePrecision)} · {admin ? 'vous administrez cette organisation.' : 'vous êtes membre de cette organisation.'}
      </p>

      {bloquee && (
        <div className="alerte alerte-erreur" style={{ marginBottom: 16 }}>
          Cette organisation est bloquée : ses cartes sont indisponibles et elle ne peut plus être modifiée.
          {organisation.motifBlocage && ` Motif : ${organisation.motifBlocage}`}
        </div>
      )}
      {erreur && <div className="alerte alerte-erreur" style={{ marginBottom: 16 }}>{erreur}</div>}

      <section className="panneau pile">
        <h2 style={{ margin: 0 }}>Ma carte d'organisation</h2>
        <p className="discret" style={{ margin: 0 }}>
          Distincte de votre carte personnelle, avec son propre lien et son propre QR code. Adresse visible par l'organisation : {organisation.monEmail}.
        </p>
        <div className="ligne">
          <Link to={`/organisations/${organisation.id}/carte`} className="bouton bouton-principal">Mettre à jour ma carte</Link>
          <button className="bouton bouton-danger" onClick={quitter}>Quitter l'organisation</button>
        </div>
      </section>

      {admin && <Gestion organisation={organisation} surChange={surChange} bloquee={bloquee} />}
    </div>
  )
}

function Gestion({ organisation, surChange, bloquee }) {
  const base = `/api/organisations/${organisation.id}`
  const [membres, setMembres] = useState([])
  const [invitations, setInvitations] = useState([])
  const [journal, setJournal] = useState(null)
  const [erreur, setErreur] = useState('')
  const logoRef = useRef(null)

  const charger = useCallback(async () => {
    try {
      const [m, i, j] = await Promise.all([
        api(`${base}/membres`), api(`${base}/invitations`), api(`${base}/journal`),
      ])
      setMembres(m)
      setInvitations(i)
      setJournal(j)
    } catch (e) {
      setErreur(e.message)
    }
  }, [base])

  useEffect(() => { charger() }, [charger])

  const agir = async (action) => {
    setErreur('')
    try {
      await action()
      await charger()
    } catch (e) {
      setErreur(e.message)
    }
  }

  const envoyerLogo = (e) => {
    const fichier = e.target.files?.[0]
    e.target.value = ''
    if (!fichier) return
    if (fichier.size > TAILLE_MAX_LOGO) return setErreur('Logo trop lourd (2 Mo maximum)')
    const formulaire = new FormData()
    formulaire.append('fichier', fichier)
    agir(async () => surChange(await api(`${base}/logo`, { methode: 'POST', formulaire })))
  }

  const retirer = (m) => {
    if (!window.confirm(`Retirer ${m.email} de l'organisation ?\n\nSa carte professionnelle ne fonctionnera plus. Son compte et sa carte personnelle ne changent pas.`)) return
    agir(() => api(`${base}/membres/${m.id}`, { methode: 'DELETE' }))
  }

  const changerRole = (m, role) => agir(() => api(`${base}/membres/${m.id}/role`, { methode: 'PUT', corps: { role } }))

  const supprimer = async () => {
    const saisi = window.prompt(
      `Supprimer définitivement ${organisation.nom} ?\n\nToutes ses cartes professionnelles disparaîtront. Les comptes et les cartes personnelles restent.\n\nSaisissez le nom de l'organisation pour confirmer.`,
    )
    if (saisi === null) return
    if (saisi.trim() !== organisation.nom) return setErreur("Le nom saisi ne correspond pas : l'organisation n'a pas été supprimée")
    try {
      await api(base, { methode: 'DELETE' })
      surChange(null)
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <>
      {erreur && <div className="alerte alerte-erreur" style={{ margin: '16px 0' }}>{erreur}</div>}

      {!bloquee && <FormulaireInvitation base={base} apres={charger} />}

      <section className="panneau pile">
        <h2 style={{ margin: 0 }}>Membres ({membres.length})</h2>
        <div className="tableau-defilant">
          <table>
            <thead><tr><th>Adresse</th><th>Nom</th><th>Poste</th><th>Rôle</th><th>Depuis</th><th /></tr></thead>
            <tbody>
              {membres.map((m) => (
                <tr key={m.id}>
                  <td>{m.email}</td>
                  <td>{m.nom || '—'}</td>
                  <td className="discret">{m.poste || '—'}</td>
                  <td>
                    <select value={m.role} disabled={bloquee} aria-label={`Rôle de ${m.email}`} onChange={(e) => changerRole(m, e.target.value)}>
                      <option value="MEMBRE">Membre</option>
                      <option value="ADMIN">Administrateur</option>
                    </select>
                  </td>
                  <td className="discret">{date(m.depuis)}</td>
                  <td>
                    <div className="ligne" style={{ flexWrap: 'nowrap' }}>
                      {m.carteJeton && (
                        <a className="bouton bouton-petit" href={`/c/${m.carteJeton}`} target="_blank" rel="noreferrer"><IconExternalLink size={14} /> Carte</a>
                      )}
                      {m.email !== organisation.monEmail && !bloquee && (
                        <button className="bouton bouton-petit bouton-danger" onClick={() => retirer(m)}><IconUserMinus size={14} /> Retirer</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panneau pile">
        <h2 style={{ margin: 0 }}>Invitations</h2>
        <div className="tableau-defilant">
          <table>
            <thead><tr><th>Adresse</th><th>Poste</th><th>Statut</th><th>Envoyée le</th><th /></tr></thead>
            <tbody>
              {invitations.map((i) => (
                <tr key={i.id}>
                  <td>{i.email}</td>
                  <td className="discret">{i.poste || '—'}</td>
                  <td>
                    <span className={`etiquette ${i.statut === 'EN_ATTENTE' ? 'etiquette-actif' : 'etiquette-neutre'}`}>
                      {i.statut === 'EN_ATTENTE' ? 'En attente' : 'Expirée ou refusée'}
                    </span>
                  </td>
                  <td className="discret">{date(i.envoyeeLe)}</td>
                  <td>
                    {i.statut === 'EN_ATTENTE' && !bloquee && (
                      <button className="bouton bouton-petit" onClick={() => agir(() => api(`${base}/invitations/${i.id}`, { methode: 'DELETE' }))}>Annuler</button>
                    )}
                  </td>
                </tr>
              ))}
              {invitations.length === 0 && <tr><td colSpan={5} className="discret">Aucune invitation en cours</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      {!bloquee && (
        <section className="panneau pile">
          <h2 style={{ margin: 0 }}>Identité de l'organisation</h2>
          <p className="discret" style={{ margin: 0 }}>Ces informations apparaissent sur toutes les cartes et les membres ne peuvent pas les changer.</p>
          <div className="ligne">
            <button type="button" className="bouton bouton-petit" onClick={() => logoRef.current?.click()}>
              <IconPhoto size={16} /> {organisation.logo ? 'Changer le logo' : 'Ajouter un logo'}
            </button>
            {organisation.logo && (
              <button type="button" className="bouton bouton-petit bouton-danger"
                      onClick={() => agir(async () => surChange(await api(`${base}/logo`, { methode: 'DELETE' })))}>Retirer le logo</button>
            )}
            <input ref={logoRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={envoyerLogo} />
          </div>
          <Formulaire
            initial={organisation}
            libelle="Enregistrer"
            enregistrer={async (corps) => {
              surChange(await api(base, { methode: 'PUT', corps }))
              await charger()
            }}
          />
        </section>
      )}

      <section className="panneau pile">
        <h2 style={{ margin: 0 }}>Journal</h2>
        <div className="tableau-defilant">
          <table>
            <thead><tr><th>Date</th><th>Auteur</th><th>Action</th><th>Concerne</th></tr></thead>
            <tbody>
              {journal?.contenu.map((j, i) => (
                <tr key={i}>
                  <td className="discret">{dateHeure(j.date)}</td>
                  <td>{j.auteur}</td>
                  <td>{ACTIONS[j.action] ?? j.action}</td>
                  <td className="discret">{j.cible ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {!bloquee && (
        <section className="panneau pile">
          <h2 style={{ margin: 0 }}>Supprimer l'organisation</h2>
          <p className="discret" style={{ margin: 0 }}>Action définitive : toutes les cartes professionnelles disparaissent. Les comptes et les cartes personnelles restent.</p>
          <div className="ligne">
            <button className="bouton bouton-danger" onClick={supprimer}><IconTrash size={16} /> Supprimer l'organisation</button>
          </div>
        </section>
      )}
    </>
  )
}

function FormulaireInvitation({ base, apres }) {
  const [email, setEmail] = useState('')
  const [nom, setNom] = useState('')
  const [poste, setPoste] = useState('')
  const [liste, setListe] = useState('')
  const [erreur, setErreur] = useState('')
  const [resultat, setResultat] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  const soumettre = async (e) => {
    e.preventDefault()
    const autres = liste.split(/[\s,;]+/).filter(Boolean).map((adresse) => ({ email: adresse }))
    const invitations = [...(email.trim() ? [{ email, nom, poste }] : []), ...autres]
    setResultat(null)
    if (invitations.length === 0) return setErreur('Indiquez au moins une adresse')
    setErreur('')
    setEnvoi(true)
    try {
      setResultat(await api(`${base}/invitations`, { methode: 'POST', corps: { invitations } }))
      setEmail('')
      setNom('')
      setPoste('')
      setListe('')
      await apres()
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section className="panneau">
      <form className="pile" onSubmit={soumettre} noValidate>
        <h2 style={{ margin: 0 }}>Inviter un membre</h2>
        <p className="discret" style={{ margin: 0 }}>
          Chaque personne reçoit un lien et décide d'accepter ou non. Vous ne saurez pas si elle a déjà un compte, et ne choisirez jamais son mot de passe.
        </p>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        {resultat && (
          <div className={`alerte ${resultat.envoyees > 0 ? 'alerte-succes' : 'alerte-info'}`}>
            {resultat.envoyees} invitation{resultat.envoyees > 1 ? 's' : ''} envoyée{resultat.envoyees > 1 ? 's' : ''}.
            {resultat.ignorees.map((i) => <div key={i.email}>{i.email} : {i.motif}</div>)}
          </div>
        )}
        <label>Adresse email
          <input type="email" maxLength={255} value={email} onChange={(e) => setEmail(e.target.value)} placeholder="prenom.nom@organisation.sn" />
        </label>
        <div className="deux-colonnes">
          <label>Nom (facultatif)<input maxLength={120} value={nom} onChange={(e) => setNom(e.target.value)} /></label>
          <label>Poste (facultatif)<input maxLength={120} value={poste} onChange={(e) => setPoste(e.target.value)} /></label>
        </div>
        <details className="adresses-multiples">
          <summary>Inviter plusieurs adresses</summary>
          <textarea aria-label="Liste d'adresses" value={liste} onChange={(e) => setListe(e.target.value)}
                    placeholder="Une adresse par ligne, 50 au maximum" />
        </details>
        <div className="ligne">
          <button className="bouton bouton-principal" disabled={envoi}><IconSend size={16} /> {envoi ? 'Envoi…' : "Envoyer l'invitation"}</button>
        </div>
      </form>
    </section>
  )
}
