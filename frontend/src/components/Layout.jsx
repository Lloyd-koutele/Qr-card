import { useEffect } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { IconLogout, IconQrcode } from '@tabler/icons-react'
import { useAuth } from '../auth'
import { session } from '../api'

export default function Layout() {
  const { profil, deconnecter } = useAuth()
  const naviguer = useNavigate()
  const admin = profil?.role === 'SUPER_ADMIN'
  const lieu = useLocation()

  // La page memorisee avant la connexion est oubliee une fois atteinte
  useEffect(() => {
    if (profil && session.lire('madeqrcard_retour') === lieu.pathname) session.effacer('madeqrcard_retour')
  }, [profil, lieu.pathname])

  const sortir = () => {
    deconnecter()
    naviguer('/connexion')
  }

  return (
    <div className="app">
      <header className="barre">
        <NavLink to="/" className="logo">
          <span className="logo-icone"><IconQrcode size={20} stroke={2} /></span>
          <span className="logo-texte">MadeQrCard</span>
        </NavLink>
        <nav className="nav">
          {profil ? (
            <>
              {admin ? (
                <NavLink to="/admin">Administration</NavLink>
              ) : (
                <>
                  <NavLink to="/ma-carte">Ma carte</NavLink>
                  <NavLink to="/carnet">Carnet</NavLink>
                </>
              )}
              <NavLink to="/parametres">Paramètres</NavLink>
              <button className="bouton-icone" onClick={sortir} title="Se déconnecter" aria-label="Se déconnecter">
                <IconLogout size={18} />
              </button>
            </>
          ) : (
            <>
              <NavLink to="/connexion">Connexion</NavLink>
              <NavLink to="/inscription" className="bouton bouton-principal bouton-petit">Créer un compte</NavLink>
            </>
          )}
        </nav>
      </header>
      <main className="contenu">
        <Outlet />
      </main>
    </div>
  )
}
