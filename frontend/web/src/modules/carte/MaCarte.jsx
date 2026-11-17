import { useEffect, useMemo, useRef, useState } from 'react'
import { QRCodeCanvas } from 'qrcode.react'
import {
  IconArrowDown, IconArrowUp, IconCopy, IconDownload, IconExternalLink, IconPalette, IconPhoto, IconPlus,
  IconRefresh, IconRotate, IconTrash,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { api, urlCarte } from '../../commun/api'
import CarteIdentite from './CarteIdentite'
import { TYPES_CONTACT, infoContact } from './contacts'
import ChampTelephone from './ChampTelephone'
import { COULEURS_CARTE } from './couleurs'

const estNumero = (type) => type === 'TELEPHONE' || type === 'WHATSAPP'

const TAILLE_MAX_PHOTO = 2 * 1024 * 1024

const versBrouillon = (c) => ({
  prenom: c.prenom ?? '',
  nom: c.nom ?? '',
  profession: c.profession ?? '',
  bio: c.bio ?? '',
  couleur: c.couleur ?? '',
  contacts: c.contacts.map(({ type, valeur, lien }) => ({ type, valeur, lien })),
})

/** Pastilles de couleurs proposees, plus une couleur libre. */
function ChoixCouleur({ valeur, surChoix }) {
  const libre = valeur && !COULEURS_CARTE.some((c) => c.valeur === valeur)
  const choisirLibre = (e) => surChoix(e.target.value)
  return (
    <>
      <div className="choix-couleurs" role="radiogroup" aria-label="Couleur de la carte">
        {COULEURS_CARTE.map((c) => (
          <button key={c.nom} type="button" role="radio" aria-checked={valeur === c.valeur} title={c.nom} aria-label={c.nom}
                  className={`pastille ${valeur === c.valeur ? 'choisie' : ''}`} style={{ background: c.apercu ?? c.valeur }}
                  onClick={() => surChoix(c.valeur)} />
        ))}
        <label className={`pastille pastille-libre ${libre ? 'choisie' : ''}`} title="Autre couleur"
               style={libre ? { background: valeur } : undefined}>
          <span className="sr-only">Autre couleur</span>
          {!libre && <IconPalette size={18} />}
          <input type="color" value={libre ? valeur : '#1f3a93'} onChange={choisirLibre} />
        </label>
      </div>
      <p className="discret" style={{ margin: 0 }}>
        Les personnes qui scannent votre QR code voient votre carte dans cette couleur. Sur une couleur claire,
        le texte du bandeau passe en foncé pour rester lisible. Enregistrez pour la garder.
      </p>
    </>
  )
}

/** Carte personnelle, ou carte professionnelle de l'organisation organisationId. */
export default function MaCarte({ organisationId }) {
  const pro = Boolean(organisationId)
  const base = pro ? `/api/me/organisations/${organisationId}/carte` : '/api/me/carte'
  const [carte, setCarte] = useState(null)
  const [brouillon, setBrouillon] = useState(null)
  const [retournee, setRetournee] = useState(false)
  const [erreur, setErreur] = useState('')
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const qrRef = useRef(null)
  const photoRef = useRef(null)

  const recevoir = (c) => {
    setCarte(c)
    setBrouillon(versBrouillon(c))
  }

  useEffect(() => {
    api(base).then(recevoir).catch((e) => setErreur(e.message))
  }, [base])

  const modifie = useMemo(
    () => carte && brouillon && JSON.stringify(versBrouillon(carte)) !== JSON.stringify(brouillon),
    [carte, brouillon],
  )

  if (!brouillon) {
    return erreur ? <div className="alerte alerte-erreur">{erreur}</div> : <div className="chargement">Chargement…</div>
  }

  const apercu = { ...carte, ...brouillon }
  const lien = urlCarte(carte.jeton)

  const champ = (nom) => ({
    value: brouillon[nom],
    onChange: (e) => setBrouillon({ ...brouillon, [nom]: e.target.value }),
  })

  const majContacts = (contacts) => setBrouillon({ ...brouillon, contacts })
  const majContact = (i, modif) => majContacts(brouillon.contacts.map((c, j) => (j === i ? { ...c, ...modif, lien: undefined } : c)))
  const deplacer = (i, sens) => {
    const liste = [...brouillon.contacts]
    const j = i + sens
    if (j < 0 || j >= liste.length) return
    ;[liste[i], liste[j]] = [liste[j], liste[i]]
    majContacts(liste)
  }

  const annoncer = (texte) => {
    setErreur('')
    setMessage(texte)
    setTimeout(() => setMessage(''), 3000)
  }

  const enregistrer = async (e) => {
    e.preventDefault()
    const vide = brouillon.contacts.findIndex((c) => !c.valeur.trim())
    if (vide >= 0) {
      setErreur(`Le contact n°${vide + 1} (${infoContact(brouillon.contacts[vide].type).libelle}) est vide`)
      return
    }
    setEnvoi(true)
    try {
      const corps = { ...brouillon, contacts: brouillon.contacts.map(({ type, valeur }) => ({ type, valeur })) }
      recevoir(await api(base, { methode: 'PUT', corps }))
      annoncer('Carte enregistrée')
    } catch (err) {
      setMessage('')
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  const envoyerPhoto = async (e) => {
    const fichier = e.target.files?.[0]
    e.target.value = ''
    if (!fichier) return
    if (fichier.size > TAILLE_MAX_PHOTO) return setErreur('Photo trop lourde (2 Mo maximum)')
    const formulaire = new FormData()
    formulaire.append('fichier', fichier)
    try {
      const c = await api(`${base}/photo`, { methode: 'POST', formulaire })
      setCarte({ ...carte, photoVersion: c.photoVersion })
      annoncer('Photo mise à jour')
    } catch (err) {
      setErreur(err.message)
    }
  }

  const retirerPhoto = async () => {
    try {
      const c = await api(`${base}/photo`, { methode: 'DELETE' })
      setCarte({ ...carte, photoVersion: c.photoVersion })
    } catch (err) {
      setErreur(err.message)
    }
  }

  const telechargerQr = () => {
    const canvas = qrRef.current?.querySelector('canvas')
    if (!canvas) return
    const a = document.createElement('a')
    a.href = canvas.toDataURL('image/png')
    a.download = `qr-carte-${carte.numero}.png`
    a.click()
  }

  const copierLien = async () => {
    try {
      await navigator.clipboard.writeText(lien)
      annoncer('Lien copié')
    } catch {
      setErreur(`Copie impossible. Votre lien : ${lien}`)
    }
  }

  const regenerer = async () => {
    const ok = window.confirm(
      "Générer un nouveau lien ?\n\nL'ancien QR code ne fonctionnera plus et votre carte sera retirée des carnets des personnes qui l'ont enregistrée.",
    )
    if (!ok) return
    try {
      const c = await api(`${base}/regenerer-lien`, { methode: 'POST' })
      setCarte({ ...carte, jeton: c.jeton, enregistrements: c.enregistrements })
      annoncer('Nouveau lien généré. Pensez à réimprimer votre QR code.')
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <div>
      <h1>{pro ? "Ma carte d'organisation" : 'Ma carte'}</h1>
      {pro ? (
        <p className="sous-titre">
          Le nom, le logo, la couleur et l'adresse viennent de {carte.organisation?.nom ?? "l'organisation"} ; vous mettez à jour le reste.
          Les administrateurs de l'organisation peuvent la lire. <Link to={`/organisations/${organisationId}`}>Retour à l'organisation</Link>
        </p>
      ) : (
        <p className="sous-titre">
          Seul vous pouvez la modifier. Les personnes qui scannent votre QR code la voient en lecture seule.
        </p>
      )}

      <div className="editeur">
        <form className="pile" onSubmit={enregistrer} noValidate>
          {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
          {message && <div className="alerte alerte-succes">{message}</div>}

          <section className="panneau pile">
            <h2>Identité</h2>
            <div className="deux-colonnes">
              <label>Prénom<input maxLength={80} {...champ('prenom')} /></label>
              <label>Nom<input maxLength={80} {...champ('nom')} /></label>
            </div>
            <label>{pro ? 'Poste' : 'Profession'}<input maxLength={120} placeholder={pro ? 'Responsable commercial' : 'Développeuse, commerçant, étudiante…'} {...champ('profession')} /></label>
            <label>Présentation (facultatif)<textarea maxLength={300} {...champ('bio')} /></label>
            <div className="ligne">
              <button type="button" className="bouton bouton-petit" onClick={() => photoRef.current?.click()}>
                <IconPhoto size={16} /> {carte.photoVersion ? 'Changer la photo' : 'Ajouter une photo'}
              </button>
              {carte.photoVersion && (
                <button type="button" className="bouton bouton-petit bouton-danger" onClick={retirerPhoto}>Retirer la photo</button>
              )}
              <input ref={photoRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={envoyerPhoto} />
            </div>
          </section>

          {!pro && (
            <section className="panneau pile">
              <h2 style={{ margin: 0 }}>Couleur de la carte</h2>
              <ChoixCouleur valeur={brouillon.couleur} surChoix={(couleur) => setBrouillon({ ...brouillon, couleur })} />
            </section>
          )}

          <section className="panneau pile">
            <div className="ligne ecarte">
              <h2 style={{ margin: 0 }}>Contacts et réseaux</h2>
              <span className="discret">{brouillon.contacts.length}/20</span>
            </div>
            <p className="discret" style={{ margin: 0 }}>
              Choisissez le pays puis saisissez le numéro, ou votre nom d'utilisateur : le lien est créé pour vous.
              Les 8 premiers apparaissent au verso de la carte.
            </p>
            {brouillon.contacts.map((c, i) => (
              <div key={i} className={`ligne-contact${estNumero(c.type) ? ' numero' : ''}`}>
                <select value={c.type} aria-label="Type de contact"
                        onChange={(e) => {
                          const type = e.target.value
                          // Telephone <-> WhatsApp : le numero est garde ; sinon la valeur ne convient plus.
                          majContact(i, estNumero(type) === estNumero(c.type) ? { type } : { type, valeur: '' })
                        }}>
                  {TYPES_CONTACT.map((t) => <option key={t.type} value={t.type}>{t.libelle}</option>)}
                </select>
                {estNumero(c.type)
                  ? <ChampTelephone valeur={c.valeur} onChange={(valeur) => majContact(i, { valeur })} />
                  : <input value={c.valeur} placeholder={infoContact(c.type).exemple} maxLength={255}
                           onChange={(e) => majContact(i, { valeur: e.target.value })} aria-label="Valeur" />}
                <div className="ligne" style={{ flexWrap: 'nowrap', gap: 4 }}>
                  <button type="button" className="bouton-icone" onClick={() => deplacer(i, -1)} aria-label="Monter"><IconArrowUp size={16} /></button>
                  <button type="button" className="bouton-icone" onClick={() => deplacer(i, 1)} aria-label="Descendre"><IconArrowDown size={16} /></button>
                  <button type="button" className="bouton-icone" onClick={() => majContacts(brouillon.contacts.filter((_, j) => j !== i))} aria-label="Supprimer"><IconTrash size={16} /></button>
                </div>
              </div>
            ))}
            {brouillon.contacts.length < 20 && (
              <button type="button" className="bouton bouton-petit" style={{ alignSelf: 'flex-start' }}
                      onClick={() => majContacts([...brouillon.contacts, { type: 'TELEPHONE', valeur: '' }])}>
                <IconPlus size={16} /> Ajouter un contact
              </button>
            )}
          </section>

          <div className="ligne">
            <button className="bouton bouton-principal" disabled={envoi || !modifie}>
              {envoi ? 'Enregistrement…' : 'Enregistrer la carte'}
            </button>
            {modifie && <button type="button" className="bouton" onClick={() => setBrouillon(versBrouillon(carte))}>Annuler les modifications</button>}
          </div>
        </form>

        <aside className="apercu">
          <CarteIdentite carte={apercu} retournee={retournee} surRetourner={() => setRetournee(!retournee)} />
          <div className="ligne ecarte">
            <button type="button" className="bouton bouton-petit" onClick={() => setRetournee(!retournee)}>
              <IconRotate size={16} /> {retournee ? 'Voir le recto' : 'Voir le verso'}
            </button>
            <span className="discret">
              {modifie ? 'Aperçu non enregistré' : `Enregistrée par ${carte.enregistrements ?? 0} personne${carte.enregistrements > 1 ? 's' : ''}`}
            </span>
          </div>

          <section className="panneau pile">
            <h2 style={{ margin: 0 }}>Partager</h2>
            <span className="secret">{lien}</span>
            <div className="ligne">
              <button type="button" className="bouton bouton-petit" onClick={telechargerQr}><IconDownload size={16} /> QR code (PNG)</button>
              <button type="button" className="bouton bouton-petit" onClick={copierLien}><IconCopy size={16} /> Copier le lien</button>
              <a className="bouton bouton-petit" href={`/c/${carte.jeton}`} target="_blank" rel="noreferrer"><IconExternalLink size={16} /> Voir</a>
              <button type="button" className="bouton bouton-petit bouton-danger" onClick={regenerer}><IconRefresh size={16} /> Nouveau lien</button>
            </div>
            <div ref={qrRef} hidden>
              <QRCodeCanvas value={lien} size={1024} marginSize={4} level="M" />
            </div>
          </section>
        </aside>
      </div>
    </div>
  )
}
