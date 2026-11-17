package made.qrcard.organisation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.dto.CompteDto.InscriptionAttenteResponse;
import made.qrcard.compte.service.CompteCree;
import made.qrcard.compte.service.InscriptionService;
import made.qrcard.organisation.dto.OrganisationDto.CreationPubliqueRequest;
import made.qrcard.organisation.dto.OrganisationDto.OrganisationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Creer une organisation sans compte : l'inscription habituelle (code par email) emporte
 * l'organisation, creee avec le compte dont la personne devient l'administrateur.
 */
@Service
public class CreationOrganisation
{

    private static final Logger log = LoggerFactory.getLogger(CreationOrganisation.class);
    private static final String PREFIXE = "organisation:";

    private final InscriptionService inscriptions;
    private final OrganisationService organisations;
    private final ObjectMapper json;

    public CreationOrganisation(InscriptionService inscriptions, OrganisationService organisations, ObjectMapper json)
    {
        this.inscriptions = inscriptions;
        this.organisations = organisations;
        this.json = json;
    }

    public InscriptionAttenteResponse demander(CreationPubliqueRequest req)
    {
        // Verifie avant d'envoyer le code : une erreur apres la saisie du code laisserait un compte sans organisation
        OrganisationService.valider(req.organisation());
        try
        {
            return inscriptions.demander(req.compte(), PREFIXE + json.writeValueAsString(req.organisation()));
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @EventListener
    public void surCompteCree(CompteCree evenement)
    {
        String complement = evenement.complement();
        if (complement == null || !complement.startsWith(PREFIXE)) return;
        try
        {
            OrganisationRequest req = json.readValue(complement.substring(PREFIXE.length()), OrganisationRequest.class);
            organisations.creer(evenement.utilisateur().getId(), req);
        }
        catch (JsonProcessingException | ApiException e)
        {
            // Le compte reste valable : la personne pourra creer l'organisation depuis l'application
            log.warn("Organisation non créée avec le compte {} : {}", evenement.utilisateur().getId(), e.getMessage());
        }
    }
}
