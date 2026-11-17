import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, jetonStocke, session } from '../../commun/api'

const AuthContexte = createContext(null)

/** Mode clair ou sombre : choix garde dans ce navigateur, pas sur le serveur. Clair par defaut. */
const CLE_MODE = 'madeqrcard_mode'
const lireMode = () => {
  try { return localStorage.getItem(CLE_MODE) === 'SOMBRE' ? 'SOMBRE' : 'CLAIR' } catch { return 'CLAIR' }
}

export function AuthProvider({ children }) {
  const [profil, setProfil] = useState(null)
  const [chargement, setChargement] = useState(true)
  const [mode, setMode] = useState(lireMode)

  const choisirMode = useCallback((valeur) => {
    try { localStorage.setItem(CLE_MODE, valeur) } catch { /* ignore */ }
    setMode(valeur)
  }, [])

  const rafraichir = useCallback(async () => {
    if (!jetonStocke.lire()) {
      setProfil(null)
      return null
    }
    try {
      const p = await api('/api/me')
      setProfil(p)
      return p
    } catch (e) {
      if (e.statut === 401) jetonStocke.effacer()
      setProfil(null)
      return null
    }
  }, [])

  useEffect(() => {
    rafraichir().finally(() => setChargement(false))
  }, [rafraichir])

  const connecter = useCallback(async (jeton) => {
    jetonStocke.ecrire(jeton)
    return rafraichir()
  }, [rafraichir])

  const deconnecter = useCallback(() => {
    jetonStocke.effacer()
    session.effacer('madeqrcard_mfa')
    setProfil(null)
  }, [])

  return (
    <AuthContexte.Provider value={{ profil, setProfil, chargement, connecter, deconnecter, rafraichir, mode, choisirMode }}>
      {children}
    </AuthContexte.Provider>
  )
}

export const useAuth = () => useContext(AuthContexte)

/** Page d'arrivee apres connexion : la page memorisee, sinon l'espace du role. */
export function destination(profil) {
  const retour = session.lire('madeqrcard_retour')
  if (profil?.securiteRequise) return '/parametres'
  if (retour && retour.startsWith('/')) return retour
  return profil?.role === 'SUPER_ADMIN' ? '/admin' : '/ma-carte'
}

/** Mode clair ou sombre : seulement pour les comptes connectes, les visiteurs restent en clair. */
export function useModeAffichage(profil, mode) {
  useEffect(() => {
    const racine = document.documentElement
    racine.dataset.theme = profil && mode === 'SOMBRE' ? 'dark' : 'light'
  }, [profil, mode])
}
