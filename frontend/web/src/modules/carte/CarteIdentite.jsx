import { QRCodeSVG } from 'qrcode.react'
import { IconAddressBook, IconEye, IconUser } from '@tabler/icons-react'
import { urlCarte, urlPhoto } from '../../commun/api'
import { texteSur } from './couleurs'
import { cibleLien, infoContact } from './contacts'

const CONTACTS_AU_VERSO = 8
const QR_FONCE = "#5C0A16"

function ligneMrz(carte) {
  const propre = (s) => (s || '').normalize('NFD').replace(/[^A-Za-z]/g, '').toUpperCase()
  const numero = (carte.numero || '').replace(/\D/g, '')
  return `QRC<<${propre(carte.nom)}<<${propre(carte.prenom)}`.padEnd(30, '<') + numero
}

const dateFr = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '')

/** Teintes de la carte derivees de sa couleur principale (organisation, ou choix du titulaire). */
const teintes = (c) => ({
  '--c-main': c,
  '--c-sur-main': texteSur(c),
  '--c-clair': `color-mix(in srgb, ${c} 8%, white)`,
  '--c-bord': `color-mix(in srgb, ${c} 28%, white)`,
  '--c-fonce': `color-mix(in srgb, ${c} 45%, black)`,
  '--c-moyen': `color-mix(in srgb, ${c} 80%, black)`,
})

/**
 * Carte au format carte d'identite (85,6 x 54 mm). Carte personnelle : couleur choisie
 * (rouge par defaut) ; carte professionnelle : couleur de l'organisation. Les couleurs
 * ne changent pas avec le mode clair ou sombre.
 */
export default function CarteIdentite({ carte, retournee = false, surRetourner, sansQr = false }) {
  const photo = urlPhoto(carte)
  const contacts = (carte.contacts || []).slice(0, CONTACTS_AU_VERSO)
  const organisation = carte.organisation
  // Professionnelle : couleur de l'organisation. Personnelle : couleur choisie, sinon rouge.
  const couleur = organisation ? organisation.couleur : carte.couleur
  const style = couleur ? teintes(couleur) : undefined

  return (
    <div
      className={`carte-scene ${surRetourner ? 'cliquable' : ''}`}
      onClick={surRetourner}
      role={surRetourner ? 'button' : undefined}
      aria-label={surRetourner ? 'Retourner la carte' : undefined}
    >
      <div className={`carte-pivot ${retournee ? 'retournee' : ''}`}>
        <div className="carte face recto theme-rouge" style={style}>
          <div className="cercle grand" />
          <div className="cercle petit" />
          <div className="bandeau">
            <span className="marque">
              {organisation?.logo && <img className="marque-logo" src={organisation.logo} alt="" />}
              <span className="marque-nom">{organisation?.nom ?? 'MadeQrCard'}</span>
            </span>
            <span className="marque-type">{organisation ? 'Carte professionnelle' : 'Carte personnelle'}</span>
          </div>
          <div className="liseret" />
          <div className="corps">
            <div className="photo">
              {photo ? <img src={photo} alt="" /> : <IconUser className="photo-vide" stroke={1.5} />}
            </div>
            <div className="champs">
              <Champ libelle="Nom" valeur={carte.nom} />
              <Champ libelle="Prénom" valeur={carte.prenom} />
              <Champ libelle={organisation ? 'Poste' : 'Profession'} valeur={carte.profession} />
              <Champ libelle="N° de carte" valeur={carte.numero} mono />
            </div>
            {!sansQr && carte.jeton && (
              <div className="qr">
                <div className="qr-cadre">
                  <QRCodeSVG value={urlCarte(carte.jeton)} size={256} fgColor={organisation ? '#111111' : QR_FONCE} bgColor="#ffffff"
                             level="M" style={{ width: '100%', height: '100%' }} />
                </div>
                <span>Scannez-moi</span>
              </div>
            )}
          </div>
          <div className="pied">Émise le {dateFr(carte.creeLe)}{organisation?.adresse && ` · ${organisation.adresse}`}</div>
        </div>

        <div className="carte face verso theme-rouge" style={style}>
          <div className="bandeau">
            <span className="marque">Mes contacts</span>
            <span className="lecture"><IconEye size="1em" /> Lecture seule</span>
          </div>
          <div className="liseret" />
          <div className="grille-contacts">
            {contacts.length === 0 && <p className="vide">Aucun contact pour l'instant</p>}
            {contacts.map((c, i) => {
              const info = infoContact(c.type)
              const Icone = info.icone
              const contenu = <><Icone className="ic" stroke={1.8} /><span>{info.action ?? info.libelle}</span></>
              return c.lien ? (
                <a key={i} className="mini-bouton" href={c.lien} {...cibleLien(c.lien)} onClick={(e) => e.stopPropagation()}>
                  {contenu}
                </a>
              ) : (
                <span key={i} className="mini-bouton">{contenu}</span>
              )
            })}
          </div>
          {carte.jeton && (
            <a className="bouton-carte" href={`/api/public/cartes/${carte.jeton}/vcard`} onClick={(e) => e.stopPropagation()}>
              <IconAddressBook size="1.3em" /> Ajouter aux contacts
            </a>
          )}
          <div className="mrz">{ligneMrz(carte)}</div>
        </div>
      </div>
    </div>
  )
}

function Champ({ libelle, valeur, mono }) {
  return (
    <div className="champ">
      <p className="lib">{libelle}</p>
      <p className={`val ${mono ? 'mono' : ''}`}>{valeur || '—'}</p>
    </div>
  )
}
