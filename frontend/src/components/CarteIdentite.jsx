import { QRCodeSVG } from 'qrcode.react'
import { IconAddressBook, IconEye, IconUser } from '@tabler/icons-react'
import { urlCarte, urlPhoto } from '../api'
import { cibleLien, infoContact } from './contacts'

const CONTACTS_AU_VERSO = 8
const QR_FONCE = { ROUGE: "#5C0A16", BLEU: "#0D2A57", ORANGE: "#4A2104" }

function ligneMrz(carte) {
  const propre = (s) => (s || '').normalize('NFD').replace(/[^A-Za-z]/g, '').toUpperCase()
  const numero = (carte.numero || '').replace(/\D/g, '')
  return `QRC<<${propre(carte.nom)}<<${propre(carte.prenom)}`.padEnd(30, '<') + numero
}

const dateFr = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '')

/**
 * Carte au format carte d'identite (85,6 x 54 mm). Les couleurs viennent du
 * theme de la carte et ne changent pas avec le mode clair ou sombre.
 */
export default function CarteIdentite({ carte, retournee = false, surRetourner, sansQr = false }) {
  const theme = (carte.theme || 'ROUGE').toLowerCase()
  const photo = urlPhoto(carte)
  const contacts = (carte.contacts || []).slice(0, CONTACTS_AU_VERSO)

  return (
    <div
      className={`carte-scene ${surRetourner ? 'cliquable' : ''}`}
      onClick={surRetourner}
      role={surRetourner ? 'button' : undefined}
      aria-label={surRetourner ? 'Retourner la carte' : undefined}
    >
      <div className={`carte-pivot ${retournee ? 'retournee' : ''}`}>
        <div className={`carte face recto theme-${theme}`}>
          <div className="cercle grand" />
          <div className="cercle petit" />
          <div className="bandeau">
            <span className="marque">MadeQrCard</span>
            <span>Carte personnelle</span>
          </div>
          <div className="liseret" />
          <div className="corps">
            <div className="photo">
              {photo ? <img src={photo} alt="" /> : <IconUser className="photo-vide" stroke={1.5} />}
            </div>
            <div className="champs">
              <Champ libelle="Nom" valeur={carte.nom} />
              <Champ libelle="Prénom" valeur={carte.prenom} />
              <Champ libelle="Profession" valeur={carte.profession} />
              <Champ libelle="N° de carte" valeur={carte.numero} mono />
            </div>
            {!sansQr && carte.jeton && (
              <div className="qr">
                <div className="qr-cadre">
                  <QRCodeSVG value={urlCarte(carte.jeton)} size={256} fgColor={QR_FONCE[carte.theme] ?? QR_FONCE.ROUGE} bgColor="#ffffff"
                             level="M" style={{ width: '100%', height: '100%' }} />
                </div>
                <span>Scannez-moi</span>
              </div>
            )}
          </div>
          <div className="pied">Émise le {dateFr(carte.creeLe)}</div>
        </div>

        <div className={`carte face verso theme-${theme}`}>
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
