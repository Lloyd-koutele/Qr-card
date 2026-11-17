export const TYPES_ORGANISATION = [
  { valeur: 'ENTREPRISE', libelle: 'Entreprise' },
  { valeur: 'ASSOCIATION', libelle: 'Association ou ONG' },
  { valeur: 'ECOLE', libelle: 'École ou université' },
  { valeur: 'ADMINISTRATION', libelle: 'Administration' },
  { valeur: 'AUTRE', libelle: 'Autre' },
]

/** Pour « Autre », la precision saisie remplace le libelle quand il y en a une. */
export const libelleType = (valeur, precision) =>
  (valeur === 'AUTRE' && precision) || (TYPES_ORGANISATION.find((t) => t.valeur === valeur)?.libelle ?? valeur)

/** Organisation saisie avant une connexion Google : creee au retour, une fois le compte connecte. */
export const CLE_A_CREER = 'madeqrcard_organisation_a_creer'
