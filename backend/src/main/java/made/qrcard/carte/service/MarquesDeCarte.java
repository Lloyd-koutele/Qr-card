package made.qrcard.carte.service;

import made.qrcard.carte.dto.CarteDto.MarqueDto;

import java.util.Optional;
import java.util.UUID;

/** Ce que le module carte doit savoir d'une organisation ; implemente par le module organisation. */
public interface MarquesDeCarte
{

    /** Vide si l'organisation n'existe plus ou si elle est bloquee : ses cartes sont alors indisponibles. */
    Optional<MarqueDto> marque(UUID organisationId);
}
