import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { IconEdit, IconExternalLink, IconTrash } from '@tabler/icons-react'
import { api } from '../api'
import CarteIdentite from '../components/CarteIdentite'

export default function Carnet() {
  const [elements, setElements] = useState(null)
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    api('/api/carnet').then(setElements).catch((e) => setErreur(e.message))
  }, [])

  const retirer = async (id) => {
    if (!window.confirm('Retirer cette carte de votre carnet ?')) return
    try {
      await api(`/api/carnet/${id}`, { methode: 'DELETE' })
      setElements(elements.filter((e) => e.id !== id))
    } catch (e) {
      setErreur(e.message)
    }
  }

  const modifierNote = async (element) => {
    const note = window.prompt('Note personnelle', element.note ?? '')
    if (note === null) return
    try {
      const maj = await api(`/api/carnet/${element.id}`, { methode: 'PUT', corps: { note: note.slice(0, 120) } })
      setElements(elements.map((e) => (e.id === element.id ? maj : e)))
    } catch (e) {
      setErreur(e.message)
    }
  }

  if (!elements) return erreur ? <div className="alerte alerte-erreur">{erreur}</div> : <div className="chargement">Chargement…</div>

  return (
    <div>
      <h1>Mon carnet</h1>
      <p className="sous-titre">Les cartes que vous avez enregistrées après avoir scanné leur QR code.</p>
      {erreur && <div className="alerte alerte-erreur" style={{ marginBottom: 16 }}>{erreur}</div>}

      {elements.length === 0 ? (
        <div className="panneau">
          <h2>Votre carnet est vide</h2>
          <p className="discret" style={{ margin: 0 }}>
            Scannez le QR code d'une carte, puis touchez « Enregistrer dans mon carnet » pour la retrouver ici.
          </p>
        </div>
      ) : (
        <div className="grille-carnet">
          {elements.map((e) => (
            <div key={e.id} className="element-carnet">
              {e.carte ? (
                <Link to={`/c/${e.carte.jeton}`}><CarteIdentite carte={e.carte} sansQr /></Link>
              ) : (
                <div className="panneau"><strong>Carte indisponible</strong><p className="discret" style={{ margin: 0 }}>Son propriétaire a changé de lien ou son compte est suspendu.</p></div>
              )}
              {e.note && <p className="discret" style={{ margin: 0 }}>{e.note}</p>}
              <div className="ligne">
                {e.carte && <Link to={`/c/${e.carte.jeton}`} className="bouton bouton-petit"><IconExternalLink size={16} /> Ouvrir</Link>}
                <button className="bouton bouton-petit" onClick={() => modifierNote(e)}><IconEdit size={16} /> Note</button>
                <button className="bouton bouton-petit bouton-danger" onClick={() => retirer(e.id)}><IconTrash size={16} /> Retirer</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
