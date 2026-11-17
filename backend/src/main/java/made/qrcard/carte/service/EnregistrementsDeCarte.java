package made.qrcard.carte.service;

import java.util.UUID;

/** Ce que le module carte doit savoir des carnets ; implemente par le module carnet. */
public interface EnregistrementsDeCarte
{

    boolean estEnregistree(UUID utilisateurId, UUID carteId);

    long compter(UUID carteId);

    /** Retire la carte de tous les carnets. */
    void oublier(UUID carteId);
}
