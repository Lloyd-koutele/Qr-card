package made.qrcard.organisation.service;

import made.qrcard.carte.dto.CarteDto.MarqueDto;
import made.qrcard.carte.service.MarquesDeCarte;
import made.qrcard.organisation.domaine.Organisation;
import made.qrcard.organisation.domaine.OrganisationRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MarquesOrganisation implements MarquesDeCarte
{

    private final OrganisationRepository organisations;

    public MarquesOrganisation(OrganisationRepository organisations)
    {
        this.organisations = organisations;
    }

    @Override
    public Optional<MarqueDto> marque(UUID organisationId)
    {
        return organisations.findById(organisationId)
                .filter(Organisation::estActive)
                .map(e -> new MarqueDto(e.getId(), e.getNom(), e.getCouleur(), e.getAdresse(), e.urlLogo()));
    }
}
