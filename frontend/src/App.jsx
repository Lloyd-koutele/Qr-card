import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { destination, useAuth, useModeAffichage } from './auth'
import { session } from './api'
import Layout from './components/Layout'
import Accueil from './pages/Accueil'
import Connexion from './pages/Connexion'
import Inscription from './pages/Inscription'
import CodeDeuxFacteurs from './pages/CodeDeuxFacteurs'
import RetourGoogle from './pages/RetourGoogle'
import MaCarte from './pages/MaCarte'
import Carnet from './pages/Carnet'
import Parametres from './pages/Parametres'
import Admin from './pages/Admin'
import CartePublique from './pages/CartePublique'
import Introuvable from './pages/Introuvable'

function Protege({ children, role }) {
  const { profil, chargement } = useAuth()
  const lieu = useLocation()
  if (chargement) return <div className="chargement">Chargement…</div>
  if (!profil) {
    session.ecrire('qrcard_retour', lieu.pathname)
    return <Navigate to="/connexion" replace />
  }
  // Un admin doit d'abord changer son mot de passe et activer la 2FA
  if (profil.securiteRequise && lieu.pathname !== '/parametres') return <Navigate to="/parametres" replace />
  if (role && profil.role !== role) return <Navigate to={destination(profil)} replace />
  return children
}

function InviteSeulement({ children }) {
  const { profil, chargement } = useAuth()
  if (chargement) return <div className="chargement">Chargement…</div>
  if (profil) return <Navigate to={destination(profil)} replace />
  return children
}

export default function App() {
  const { profil } = useAuth()
  useModeAffichage(profil)

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<InviteSeulement><Accueil /></InviteSeulement>} />
        <Route path="/connexion" element={<InviteSeulement><Connexion /></InviteSeulement>} />
        <Route path="/connexion/code" element={<InviteSeulement><CodeDeuxFacteurs /></InviteSeulement>} />
        <Route path="/inscription" element={<InviteSeulement><Inscription /></InviteSeulement>} />
        <Route path="/auth/callback" element={<RetourGoogle />} />
        <Route path="/c/:jeton" element={<CartePublique />} />
        <Route path="/ma-carte" element={<Protege><MaCarte /></Protege>} />
        <Route path="/carnet" element={<Protege><Carnet /></Protege>} />
        <Route path="/parametres" element={<Protege><Parametres /></Protege>} />
        <Route path="/admin" element={<Protege role="SUPER_ADMIN"><Admin /></Protege>} />
        <Route path="*" element={<Introuvable />} />
      </Route>
    </Routes>
  )
}
