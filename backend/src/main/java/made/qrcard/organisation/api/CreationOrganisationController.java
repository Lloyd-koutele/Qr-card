package made.qrcard.organisation.api;

import jakarta.validation.Valid;
import made.qrcard.compte.dto.CompteDto.InscriptionAttenteResponse;
import made.qrcard.organisation.dto.OrganisationDto.CreationPubliqueRequest;
import made.qrcard.organisation.service.CreationOrganisation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Sous /api/auth : meme limite de requetes que l'inscription, puisqu'un email est envoye. */
@RestController
public class CreationOrganisationController
{

    private final CreationOrganisation creation;

    public CreationOrganisationController(CreationOrganisation creation)
    {
        this.creation = creation;
    }

    @PostMapping("/api/auth/inscription/organisation")
    public InscriptionAttenteResponse demander(@Valid @RequestBody CreationPubliqueRequest req)
    {
        return creation.demander(req);
    }
}
