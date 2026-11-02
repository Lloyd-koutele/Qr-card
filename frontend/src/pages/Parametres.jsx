import { useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { IconDeviceDesktop, IconMoon, IconShieldCheck, IconSun } from '@tabler/icons-react'
import { api } from '../api'
import { useAuth } from '../auth'

const MODES = [
  { valeur: 'CLAIR', libelle: 'Clair', icone: IconSun },
  { valeur: 'SOMBRE', libelle: 'Sombre', icone: IconMoon },
  { valeur: 'SYSTEME', libelle: 'Comme le téléphone', icone: IconDeviceDesktop },
]

export default function Parametres() {
  const { profil, setProfil } = useAuth()
  const admin = profil.role !== 'USER'

  return (
    <div style={{ maxWidth: 640 }}>
      <h1>Paramètres</h1>
      <p className="sous-titre">{profil.email}</p>
      {profil.securiteRequise && (
        <div className="alerte alerte-info" style={{ marginBottom: 16 }}>
          Avant d'accéder à l'administration : {profil.changerMotDePasse && 'changez le mot de passe initial'}
          {profil.changerMotDePasse && !profil.totpActif && ' puis '}
          {!profil.totpActif && 'activez la double authentification'}.
        </div>
      )}
      <Affichage profil={profil} setProfil={setProfil} />
      <MotDePasse profil={profil} setProfil={setProfil} />
      <DeuxFacteurs profil={profil} setProfil={setProfil} admin={admin} />
    </div>
  )
}

function Affichage({ profil, setProfil }) {
  const [erreur, setErreur] = useState('')
  const choisir = async (modeAffichage) => {
    try {
      setProfil(await api('/api/me/preferences', { methode: 'PUT', corps: { modeAffichage } }))
    } catch (e) {
      setErreur(e.message)
    }
  }
  return (
    <section className="panneau pile">
      <h2 style={{ margin: 0 }}>Affichage</h2>
      <div className="choix">
        {MODES.map(({ valeur, libelle, icone: Icone }) => (
          <button key={valeur} type="button" className={profil.modeAffichage === valeur ? 'actif' : ''} onClick={() => choisir(valeur)}>
            <Icone size={16} /> {libelle}
          </button>
        ))}
      </div>
      <p className="discret" style={{ margin: 0 }}>Les couleurs de votre carte ne changent pas : elles dépendent du thème choisi.</p>
      {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
    </section>
  )
}

function MotDePasse({ profil, setProfil }) {
  const [form, setForm] = useState({ ancien: '', nouveau: '', confirmation: '' })
  const [etat, setEtat] = useState({ erreur: '', ok: '' })

  const soumettre = async (e) => {
    e.preventDefault()
    if (form.nouveau.length < 8) return setEtat({ erreur: 'Le nouveau mot de passe doit faire au moins 8 caractères', ok: '' })
    if (form.nouveau !== form.confirmation) return setEtat({ erreur: 'Les deux mots de passe ne correspondent pas', ok: '' })
    try {
      setProfil(await api('/api/me/mot-de-passe', { methode: 'PUT', corps: { ancien: form.ancien, nouveau: form.nouveau } }))
      setForm({ ancien: '', nouveau: '', confirmation: '' })
      setEtat({ erreur: '', ok: 'Mot de passe modifié' })
    } catch (err) {
      setEtat({ erreur: err.message, ok: '' })
    }
  }

  return (
    <form className="panneau pile" onSubmit={soumettre} noValidate>
      <h2 style={{ margin: 0 }}>{profil.aMotDePasse ? 'Mot de passe' : 'Définir un mot de passe'}</h2>
      {!profil.aMotDePasse && <p className="discret" style={{ margin: 0 }}>Votre compte utilise Google. Un mot de passe vous permet aussi de vous connecter sans Google.</p>}
      {etat.erreur && <div className="alerte alerte-erreur">{etat.erreur}</div>}
      {etat.ok && <div className="alerte alerte-succes">{etat.ok}</div>}
      {profil.aMotDePasse && (
        <label>Mot de passe actuel
          <input type="password" autoComplete="current-password" value={form.ancien} onChange={(e) => setForm({ ...form, ancien: e.target.value })} />
        </label>
      )}
      <div className="deux-colonnes">
        <label>Nouveau mot de passe
          <input type="password" autoComplete="new-password" value={form.nouveau} onChange={(e) => setForm({ ...form, nouveau: e.target.value })} />
        </label>
        <label>Confirmation
          <input type="password" autoComplete="new-password" value={form.confirmation} onChange={(e) => setForm({ ...form, confirmation: e.target.value })} />
        </label>
      </div>
      <button className="bouton" style={{ alignSelf: 'flex-start' }}>Enregistrer</button>
    </form>
  )
}

function DeuxFacteurs({ profil, setProfil, admin }) {
  const [config, setConfig] = useState(null)
  const [code, setCode] = useState('')
  const [codesSecours, setCodesSecours] = useState(null)
  const [erreur, setErreur] = useState('')

  const demarrer = async () => {
    setErreur('')
    try {
      setConfig(await api('/api/me/2fa/initialiser', { methode: 'POST' }))
    } catch (e) {
      setErreur(e.message)
    }
  }

  const activer = async (e) => {
    e.preventDefault()
    if (!code.trim()) return setErreur("Saisissez le code affiché dans l'application")
    try {
      const r = await api('/api/me/2fa/activer', { methode: 'POST', corps: { code } })
      setCodesSecours(r.codes)
      setConfig(null)
      setCode('')
      setErreur('')
    } catch (err) {
      setErreur(err.message)
    }
  }

  const desactiver = async (e) => {
    e.preventDefault()
    if (!code.trim()) return setErreur('Saisissez un code pour confirmer')
    try {
      setProfil(await api('/api/me/2fa/desactiver', { methode: 'POST', corps: { code } }))
      setCode('')
      setErreur('')
    } catch (err) {
      setErreur(err.message)
    }
  }

  const terminer = async () => {
    setCodesSecours(null)
    setProfil(await api('/api/me'))
  }

  const telechargerCodes = () => {
    const blob = new Blob([`Codes de secours MadeQrCard (${profil.email})\n\n${codesSecours.join('\n')}\n`], { type: 'text/plain' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'codes-secours-madeqrcard.txt'
    a.click()
    URL.revokeObjectURL(a.href)
  }

  return (
    <section className="panneau pile">
      <div className="ligne ecarte">
        <h2 style={{ margin: 0 }}>Double authentification</h2>
        <span className={`etiquette ${profil.totpActif ? 'etiquette-actif' : 'etiquette-neutre'}`}>{profil.totpActif ? 'Active' : 'Inactive'}</span>
      </div>
      {erreur && <div className="alerte alerte-erreur">{erreur}</div>}

      {codesSecours ? (
        <>
          <div className="alerte alerte-succes"><IconShieldCheck size={16} style={{ verticalAlign: -3 }} /> Double authentification activée.</div>
          <p style={{ margin: 0 }}>
            Gardez ces codes de secours en lieu sûr. Chacun ne sert qu'une fois, si vous perdez votre téléphone. Ils ne seront plus affichés.
          </p>
          <div className="codes">{codesSecours.map((c) => <span key={c}>{c}</span>)}</div>
          <div className="ligne">
            <button className="bouton" onClick={telechargerCodes}>Télécharger les codes</button>
            <button className="bouton bouton-principal" onClick={terminer}>J'ai noté mes codes</button>
          </div>
        </>
      ) : config ? (
        <form className="pile" onSubmit={activer} noValidate>
          <p style={{ margin: 0 }}>1. Scannez ce QR code avec Google Authenticator, Microsoft Authenticator ou Authy.</p>
          <div className="qr-2fa"><QRCodeSVG value={config.uri} size={180} /></div>
          <p className="discret" style={{ margin: 0 }}>Ou saisissez cette clé à la main :</p>
          <span className="secret">{config.secret}</span>
          <label>2. Saisissez le code à 6 chiffres affiché
            <input inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6} />
          </label>
          <div className="ligne">
            <button className="bouton bouton-principal">Activer</button>
            <button type="button" className="bouton" onClick={() => setConfig(null)}>Annuler</button>
          </div>
        </form>
      ) : profil.totpActif ? (
        admin ? (
          <p className="discret" style={{ margin: 0 }}>Obligatoire pour les administrateurs : elle ne peut pas être désactivée.</p>
        ) : (
          <form className="pile" onSubmit={desactiver} noValidate>
            <p className="discret" style={{ margin: 0 }}>Un code vous est demandé à chaque connexion, en plus du mot de passe ou de Google.</p>
            <label>Code actuel pour désactiver
              <input inputMode="numeric" value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6} />
            </label>
            <button className="bouton bouton-danger" style={{ alignSelf: 'flex-start' }}>Désactiver</button>
          </form>
        )
      ) : (
        <>
          <p className="discret" style={{ margin: 0 }}>
            Protégez votre compte avec un code à 6 chiffres généré par une application sur votre téléphone, demandé à chaque connexion.
            {admin && ' Obligatoire pour les administrateurs.'}
          </p>
          <button className="bouton bouton-principal" style={{ alignSelf: 'flex-start' }} onClick={demarrer}>
            <IconShieldCheck size={18} /> Activer la double authentification
          </button>
        </>
      )}
    </section>
  )
}
