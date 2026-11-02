import { Link } from 'react-router-dom'

export default function Introuvable() {
  return (
    <div className="etroit" style={{ textAlign: 'center', paddingTop: 40 }}>
      <h1>Page introuvable</h1>
      <p className="sous-titre">Cette adresse ne correspond à aucune page.</p>
      <Link to="/" className="bouton">Retour à l'accueil</Link>
    </div>
  )
}
