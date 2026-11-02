const CLE_JETON = 'qrcard_jeton'

// Le stockage du navigateur peut etre indisponible (navigation privee stricte)
export const jetonStocke = {
  lire() {
    try { return localStorage.getItem(CLE_JETON) } catch { return null }
  },
  ecrire(valeur) {
    try { localStorage.setItem(CLE_JETON, valeur) } catch { /* ignore */ }
  },
  effacer() {
    try { localStorage.removeItem(CLE_JETON) } catch { /* ignore */ }
  },
}

export const session = {
  lire(cle) {
    try { return sessionStorage.getItem(cle) } catch { return null }
  },
  ecrire(cle, valeur) {
    try { sessionStorage.setItem(cle, valeur) } catch { /* ignore */ }
  },
  effacer(cle) {
    try { sessionStorage.removeItem(cle) } catch { /* ignore */ }
  },
}

export class ErreurApi extends Error {
  constructor(message, statut) {
    super(message)
    this.statut = statut
  }
}

export async function api(chemin, { methode = 'GET', corps, formulaire } = {}) {
  const entetes = {}
  const jeton = jetonStocke.lire()
  if (jeton) entetes.Authorization = `Bearer ${jeton}`

  let body
  if (formulaire) {
    body = formulaire
  } else if (corps !== undefined) {
    entetes['Content-Type'] = 'application/json'
    body = JSON.stringify(corps)
  }

  let reponse
  try {
    reponse = await fetch(chemin, { method: methode, headers: entetes, body })
  } catch {
    throw new ErreurApi('Serveur injoignable. Vérifiez votre connexion.', 0)
  }
  if (reponse.status === 204) return null
  const donnees = await reponse.json().catch(() => null)
  if (!reponse.ok) {
    throw new ErreurApi(donnees?.message || `Erreur ${reponse.status}`, reponse.status)
  }
  return donnees
}

export const urlCarte = (jeton) => `${window.location.origin}/c/${jeton}`
export const urlPhoto = (carte) =>
  carte.photoVersion ? `/api/public/cartes/${carte.jeton}/photo?v=${carte.photoVersion}` : null
