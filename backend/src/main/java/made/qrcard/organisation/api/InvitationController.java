package made.qrcard.organisation.api;

import jakarta.validation.Valid;
import made.qrcard.compte.dto.CompteDto.ConnexionResponse;
import made.qrcard.organisation.domaine.OrganisationLogo;
import made.qrcard.organisation.dto.OrganisationDto.*;
import made.qrcard.organisation.service.OrganisationService;
import made.qrcard.organisation.service.InvitationService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

import static made.qrcard.commun.securite.SecuriteUtils.idCourant;

/** Le jeton du lien recu par email suffit pour consulter et refuser ; accepter demande d'etre connecte. */
@RestController
public class InvitationController
{

    private final InvitationService invitations;
    private final OrganisationService organisations;

    public InvitationController(InvitationService invitations, OrganisationService organisations)
    {
        this.invitations = invitations;
        this.organisations = organisations;
    }

    @GetMapping("/api/public/invitations/{jeton}")
    public ResponseEntity<InvitationPublique> consulter(@PathVariable String jeton)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(invitations.consulter(jeton));
    }

    @PostMapping("/api/public/invitations/{jeton}/refuser")
    public ResponseEntity<Void> refuser(@PathVariable String jeton)
    {
        invitations.refuser(jeton);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/public/invitations/{jeton}/compte")
    public ConnexionResponse creerCompte(@PathVariable String jeton, @Valid @RequestBody CompteInvitationRequest req)
    {
        return invitations.creerCompte(jeton, req);
    }

    @PostMapping("/api/invitations/{jeton}/accepter")
    public ResponseEntity<Void> accepter(@PathVariable String jeton)
    {
        invitations.accepter(idCourant(), jeton);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/public/organisations/{id}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable UUID id)
    {
        return organisations.logo(id)
                .map(InvitationController::image)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> image(OrganisationLogo logo)
    {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.getType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(logo.getDonnees());
    }
}
