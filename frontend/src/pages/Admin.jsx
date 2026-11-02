import { useCallback, useEffect, useState } from 'react'
import { IconLock, IconLockOpen, IconSearch } from '@tabler/icons-react'
import { api } from '../api'

const date = (iso) => (iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—')
const ROLES = { SUPER_ADMIN: 'Administrateur', ORG_ADMIN: 'Admin entreprise', UO_ADMIN: 'Admin unité', USER: 'Utilisateur' }

/**
 * L'administrateur voit les comptes (email, statut, dates) et peut les bloquer.
 * Il n'a accès ni au contenu des cartes ni a la modification des comptes.
 */
export default function Admin() {
  const [stats, setStats] = useState(null)
  const [page, setPage] = useState(null)
  const [recherche, setRecherche] = useState('')
  const [numero, setNumero] = useState(0)
  const [journal, setJournal] = useState(null)
  const [cible, setCible] = useState(null)
  const [erreur, setErreur] = useState('')

  const charger = useCallback(async () => {
    try {
      const [s, p, j] = await Promise.all([
        api('/api/admin/stats'),
        api(`/api/admin/utilisateurs?q=${encodeURIComponent(recherche)}&page=${numero}`),
        api('/api/admin/journal'),
      ])
      setStats(s)
      setPage(p)
      setJournal(j)
      setErreur('')
    } catch (e) {
      setErreur(e.message)
    }
  }, [recherche, numero])

  useEffect(() => {
    const minuteur = setTimeout(charger, 250)
    return () => clearTimeout(minuteur)
  }, [charger])

  const debloquer = async (u) => {
    if (!window.confirm(`Débloquer ${u.email} ?`)) return
    try {
      await api(`/api/admin/utilisateurs/${u.id}/debloquer`, { methode: 'POST' })
      charger()
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <div>
      <h1>Administration</h1>
      <p className="sous-titre">Comptes du système. Le contenu des cartes reste privé.</p>
      {erreur && <div className="alerte alerte-erreur" style={{ marginBottom: 16 }}>{erreur}</div>}

      {stats && (
        <div className="tuiles">
          <div className="tuile"><p>Comptes</p><strong>{stats.total}</strong></div>
          <div className="tuile"><p>Actifs</p><strong>{stats.actifs}</strong></div>
          <div className="tuile"><p>Bloqués</p><strong>{stats.bloques}</strong></div>
          <div className="tuile"><p>Cartes</p><strong>{stats.cartes}</strong></div>
        </div>
      )}

      <section className="panneau pile">
        <div className="ligne ecarte">
          <h2 style={{ margin: 0 }}>Comptes</h2>
          <label style={{ flexDirection: 'row', alignItems: 'center', minWidth: 240 }}>
            <IconSearch size={16} aria-hidden="true" />
            <input value={recherche} onChange={(e) => { setRecherche(e.target.value); setNumero(0) }} placeholder="Rechercher un email" aria-label="Rechercher un email" />
          </label>
        </div>
        <div className="tableau-defilant">
          <table>
            <thead>
              <tr><th>Email</th><th>Rôle</th><th>Connexion</th><th>2FA</th><th>Statut</th><th>Inscrit le</th><th>Dernière connexion</th><th /></tr>
            </thead>
            <tbody>
              {page?.contenu.map((u) => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{ROLES[u.role] ?? u.role}</td>
                  <td className="discret">{u.methode}</td>
                  <td>{u.totpActif ? 'Oui' : 'Non'}</td>
                  <td>
                    <span className={`etiquette ${u.statut === 'ACTIF' ? 'etiquette-actif' : 'etiquette-bloque'}`} title={u.motifBlocage ?? ''}>
                      {u.statut === 'ACTIF' ? 'Actif' : 'Bloqué'}
                    </span>
                  </td>
                  <td className="discret">{date(u.creeLe)}</td>
                  <td className="discret">{date(u.derniereConnexion)}</td>
                  <td>
                    {u.role !== 'SUPER_ADMIN' && (u.statut === 'ACTIF' ? (
                      <button className="bouton bouton-petit bouton-danger" onClick={() => setCible(u)}><IconLock size={14} /> Bloquer</button>
                    ) : (
                      <button className="bouton bouton-petit" onClick={() => debloquer(u)}><IconLockOpen size={14} /> Débloquer</button>
                    ))}
                  </td>
                </tr>
              ))}
              {page?.contenu.length === 0 && <tr><td colSpan={8} className="discret">Aucun compte trouvé</td></tr>}
            </tbody>
          </table>
        </div>
        {page && page.totalPages > 1 && (
          <div className="ligne">
            <button className="bouton bouton-petit" disabled={numero === 0} onClick={() => setNumero(numero - 1)}>Précédent</button>
            <span className="discret">Page {page.page + 1} sur {page.totalPages}</span>
            <button className="bouton bouton-petit" disabled={numero + 1 >= page.totalPages} onClick={() => setNumero(numero + 1)}>Suivant</button>
          </div>
        )}
      </section>

      <section className="panneau pile">
        <h2 style={{ margin: 0 }}>Journal des actions</h2>
        <div className="tableau-defilant">
          <table>
            <thead><tr><th>Date</th><th>Administrateur</th><th>Action</th><th>Compte</th><th>Motif</th></tr></thead>
            <tbody>
              {journal?.contenu.map((a, i) => (
                <tr key={i}>
                  <td className="discret">{date(a.date)}</td>
                  <td>{a.adminEmail}</td>
                  <td>{a.action === 'BLOQUER' ? 'Blocage' : 'Déblocage'}</td>
                  <td>{a.cibleEmail}</td>
                  <td className="discret">{a.motif ?? '—'}</td>
                </tr>
              ))}
              {journal?.contenu.length === 0 && <tr><td colSpan={5} className="discret">Aucune action pour l'instant</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      {cible && <FenetreBlocage utilisateur={cible} fermer={() => setCible(null)} apres={charger} />}
    </div>
  )
}

function FenetreBlocage({ utilisateur, fermer, apres }) {
  const [motif, setMotif] = useState('')
  const [erreur, setErreur] = useState('')

  const valider = async (e) => {
    e.preventDefault()
    if (!motif.trim()) return setErreur('Indiquez le motif du blocage')
    try {
      await api(`/api/admin/utilisateurs/${utilisateur.id}/bloquer`, { methode: 'POST', corps: { motif } })
      fermer()
      apres()
    } catch (err) {
      setErreur(err.message)
    }
  }

  return (
    <div className="voile" onClick={fermer}>
      <form className="fenetre" onClick={(e) => e.stopPropagation()} onSubmit={valider} noValidate>
        <h2 style={{ margin: 0 }}>Bloquer {utilisateur.email}</h2>
        <p className="discret" style={{ margin: 0 }}>
          Le compte ne pourra plus se connecter et sa carte affichera « Carte indisponible ». L'action est inscrite au journal.
        </p>
        {erreur && <div className="alerte alerte-erreur">{erreur}</div>}
        <label>Motif
          <textarea value={motif} maxLength={500} autoFocus onChange={(e) => { setMotif(e.target.value); setErreur('') }} />
        </label>
        <div className="ligne" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="bouton" onClick={fermer}>Annuler</button>
          <button className="bouton bouton-principal">Bloquer le compte</button>
        </div>
      </form>
    </div>
  )
}
