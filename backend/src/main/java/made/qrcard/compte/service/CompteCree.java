package made.qrcard.compte.service;

import made.qrcard.compte.domaine.Utilisateur;

/**
 * Publie a la creation d'un compte, pour que les autres modules s'y rattachent sans que le compte les connaisse.
 * complement : donnees remises par un module a l'inscription (JSON), vide le plus souvent.
 */
public record CompteCree(Utilisateur utilisateur, String complement)
{

    public CompteCree(Utilisateur utilisateur)
    {
        this(utilisateur, null);
    }
}
