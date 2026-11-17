package made.qrcard.organisation.api;

import jakarta.validation.Valid;
import made.qrcard.commun.web.PageResponse;
import made.qrcard.organisation.dto.OrganisationDto.*;
import made.qrcard.organisation.service.OrganisationService;
import made.qrcard.organisation.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static made.qrcard.commun.securite.SecuriteUtils.idCourant;

/**
 * Les organisations du compte connecte. Chaque route porte l'organisation visee ;
 * les services verifient que le compte en est membre, ou administrateur.
 */
@RestController
@RequestMapping("/api/organisations")
public class OrganisationController
{

    private final OrganisationService organisations;
    private final InvitationService invitations;

    public OrganisationController(OrganisationService organisations, InvitationService invitations)
    {
        this.organisations = organisations;
        this.invitations = invitations;
    }

    @GetMapping
    public List<OrganisationResponse> miennes()
    {
        return organisations.miennes(idCourant());
    }

    @PostMapping
    public OrganisationResponse creer(@Valid @RequestBody OrganisationRequest req)
    {
        return organisations.creer(idCourant(), req);
    }

    @GetMapping("/{id}")
    public OrganisationResponse lire(@PathVariable UUID id)
    {
        return organisations.lire(idCourant(), id);
    }

    @PutMapping("/{id}")
    public OrganisationResponse modifier(@PathVariable UUID id, @Valid @RequestBody OrganisationRequest req)
    {
        return organisations.modifier(idCourant(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id)
    {
        organisations.supprimer(idCourant(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/quitter")
    public ResponseEntity<Void> quitter(@PathVariable UUID id)
    {
        organisations.quitter(idCourant(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/logo")
    public OrganisationResponse logo(@PathVariable UUID id, @RequestParam("fichier") MultipartFile fichier) throws IOException
    {
        return organisations.enregistrerLogo(idCourant(), id, fichier);
    }

    @DeleteMapping("/{id}/logo")
    public OrganisationResponse supprimerLogo(@PathVariable UUID id)
    {
        return organisations.supprimerLogo(idCourant(), id);
    }

    @GetMapping("/{id}/membres")
    public List<MembreItem> membres(@PathVariable UUID id)
    {
        return organisations.membres(idCourant(), id);
    }

    @PutMapping("/{id}/membres/{membreId}/role")
    public MembreItem role(@PathVariable UUID id, @PathVariable UUID membreId, @Valid @RequestBody RoleRequest req)
    {
        return organisations.changerRole(idCourant(), id, membreId, req.role());
    }

    @DeleteMapping("/{id}/membres/{membreId}")
    public ResponseEntity<Void> retirer(@PathVariable UUID id, @PathVariable UUID membreId)
    {
        organisations.retirer(idCourant(), id, membreId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invitations")
    public List<InvitationItem> invitations(@PathVariable UUID id)
    {
        return invitations.lister(idCourant(), id);
    }

    @PostMapping("/{id}/invitations")
    public InvitationsResultat inviter(@PathVariable UUID id, @Valid @RequestBody InvitationsRequest req)
    {
        return invitations.inviter(idCourant(), id, req);
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    public ResponseEntity<Void> annuler(@PathVariable UUID id, @PathVariable UUID invitationId)
    {
        invitations.annuler(idCourant(), id, invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/journal")
    public PageResponse<JournalItem> journal(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page)
    {
        return organisations.journal(idCourant(), id, page);
    }
}
