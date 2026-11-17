import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom'
import { session } from '../commun/api'
import { Accueil } from '../modules/accueil'
import { Admin } from '../modules/admin'
import { Carnet } from '../modules/carnet'
import { CreationPublique, MesOrganisations, Organisation, Invitation } from '../modules/organisation'
import { CartePublique, MaCarte } from '../modules/carte'
import { CodeDeuxFacteurs, Connexion, destination, Inscription, MotDePasseOublie, Parametres, RetourGoogle, useAuth, useModeAffichage, VerificationEmail } from '../modules/compte'
import Layout from './Layout'
import Introuvable from './Introuvable'

function Protege({ children, role }) {
  const { profil, chargement } = useAuth()
  const lieu = useLocation()
  if (chargement) return <div className="chargement">Chargement…</div>
  if (!profil) {
    session.ecrire('madeqrcard_retour', lieu.pathname)
    return <Navigate to="/connexion" replace />
  }
  // Un admin doit d'abord changer son mot de passe et activer la 2FA
  if (profil.securiteRequise && lieu.pathname !== '/parametres') return <Navigate to="/parametres" replace />
  if (role && profil.role !== role) return <Navigate to={destination(profil)} replace />
  return children
}

/** Une carte professionnelle par organisation : la cle force un nouvel etat en changeant d'organisation. */
function CartePro() {
  const { id } = useParams()
  return <MaCarte key={id} organisationId={id} />
}

/** Connecte, on cree l'organisation depuis son espace. */
function CreationSansCompte() {
  const { profil, chargement } = useAuth()
  if (chargement) return <div className="chargement">Chargement…</div>
  if (profil) return <Navigate to="/organisations" replace />
  return <CreationPublique />
}

function InviteSeulement({ children }) {
  const { profil, chargement } = useAuth()
  if (chargement) return <div className="chargement">Chargement…</div>
  if (profil) return <Navigate to={destination(profil)} replace />
  return children
}

export default function App() {
  const { profil, mode } = useAuth()
  useModeAffichage(profil, mode)

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<InviteSeulement><Accueil /></InviteSeulement>} />
        <Route path="/connexion" element={<InviteSeulement><Connexion /></InviteSeulement>} />
        <Route path="/connexion/code" element={<InviteSeulement><CodeDeuxFacteurs /></InviteSeulement>} />
        <Route path="/inscription" element={<InviteSeulement><Inscription /></InviteSeulement>} />
        <Route path="/inscription/code" element={<InviteSeulement><VerificationEmail /></InviteSeulement>} />
        <Route path="/creer-organisation" element={<CreationSansCompte />} />
        <Route path="/mot-de-passe-oublie" element={<InviteSeulement><MotDePasseOublie /></InviteSeulement>} />
        <Route path="/auth/callback" element={<RetourGoogle />} />
        <Route path="/c/:jeton" element={<CartePublique />} />
        <Route path="/invitation/:jeton" element={<Invitation />} />
        <Route path="/ma-carte" element={<Protege><MaCarte key="personnelle" /></Protege>} />
        <Route path="/organisations" element={<Protege><MesOrganisations /></Protege>} />
        <Route path="/organisations/:id" element={<Protege><Organisation /></Protege>} />
        <Route path="/organisations/:id/carte" element={<Protege><CartePro /></Protege>} />
        {/* Anciennes adresses, quand on n'appartenait qu'a une organisation */}
        <Route path="/organisation/*" element={<Navigate to="/organisations" replace />} />
        <Route path="/carnet" element={<Protege><Carnet /></Protege>} />
        <Route path="/parametres" element={<Protege><Parametres /></Protege>} />
        <Route path="/admin" element={<Protege role="SUPER_ADMIN"><Admin /></Protege>} />
        <Route path="*" element={<Introuvable />} />
      </Route>
    </Routes>
  )
}
