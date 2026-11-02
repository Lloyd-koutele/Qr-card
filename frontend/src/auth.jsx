import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, jetonStocke, session } from './api'

const AuthContexte = createContext(null)

export function AuthProvider({ children }) {
  const [profil, setProfil] = useState(null)
  const [chargement, setChargement] = useState(true)

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
    session.effacer('qrcard_mfa')
    setProfil(null)
  }, [])

  return (
    <AuthContexte.Provider value={{ profil, setProfil, chargement, connecter, deconnecter, rafraichir }}>
      {children}
    </AuthContexte.Provider>
  )
}

export const useAuth = () => useContext(AuthContexte)

/** Page d'arrivee apres connexion : la page memorisee, sinon l'espace du role. */
export function destination(profil) {
  const retour = session.lire('qrcard_retour')
  if (profil?.securiteRequise) return '/parametres'
  if (retour && retour.startsWith('/')) return retour
  return profil?.role === 'SUPER_ADMIN' ? '/admin' : '/ma-carte'
}

/** Mode clair ou sombre : seulement pour les comptes connectes, les visiteurs restent en clair. */
export function useModeAffichage(profil) {
  useEffect(() => {
    const racine = document.documentElement
    const mode = profil?.modeAffichage
    if (!mode) {
      racine.dataset.theme = 'light'
      return
    }
    if (mode !== 'SYSTEME') {
      racine.dataset.theme = mode === 'SOMBRE' ? 'dark' : 'light'
      return
    }
    const requete = window.matchMedia('(prefers-color-scheme: dark)')
    const appliquer = () => { racine.dataset.theme = requete.matches ? 'dark' : 'light' }
    appliquer()
    requete.addEventListener('change', appliquer)
    return () => requete.removeEventListener('change', appliquer)
  }, [profil?.modeAffichage])
}
