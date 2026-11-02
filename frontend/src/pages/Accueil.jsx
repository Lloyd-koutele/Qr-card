import { useState } from 'react'
import { Link } from 'react-router-dom'
import CarteIdentite from '../components/CarteIdentite'

const EXEMPLE = {
  nom: 'Koné',
  prenom: 'Aïcha',
  profession: 'Architecte',
  numero: 'QRC-2026-00001',
  theme: 'ROUGE',
  creeLe: new Date().toISOString(),
  contacts: [
    { type: 'TELEPHONE' }, { type: 'WHATSAPP' }, { type: 'TELEGRAM' }, { type: 'INSTAGRAM' },
    { type: 'FACEBOOK' }, { type: 'TIKTOK' }, { type: 'LINKEDIN' }, { type: 'EMAIL' },
  ],
}

export default function Accueil() {
  const [retournee, setRetournee] = useState(false)
  return (
    <section className="accueil">
      <div>
        <h1>Votre carte de visite, dans un QR code</h1>
        <p>
          Réunissez vos numéros et vos réseaux sociaux sur une carte au format carte d'identité.
          Partagez-la d'un scan : WhatsApp, Instagram ou Telegram s'ouvrent directement depuis la carte.
        </p>
        <div className="ligne" style={{ marginTop: 24 }}>
          <Link to="/inscription" className="bouton bouton-principal">Créer ma carte</Link>
          <Link to="/connexion" className="bouton">J'ai déjà un compte</Link>
        </div>
      </div>
      <div className="pile" style={{ alignItems: 'center' }}>
        <CarteIdentite carte={EXEMPLE} retournee={retournee} surRetourner={() => setRetournee(!retournee)} sansQr />
        <span className="discret">Touchez la carte pour la retourner</span>
      </div>
    </section>
  )
}
