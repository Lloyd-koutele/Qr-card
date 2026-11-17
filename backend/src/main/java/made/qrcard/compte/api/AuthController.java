package made.qrcard.compte.api;

import made.qrcard.commun.config.AppProperties;
import made.qrcard.compte.dto.CompteDto.*;
import made.qrcard.compte.securite.RetourGoogleMobile;
import made.qrcard.compte.service.AuthService;
import made.qrcard.compte.service.InscriptionService;
import made.qrcard.compte.service.ReinitialisationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{

    private final AuthService auth;
    private final InscriptionService inscriptions;
    private final ReinitialisationService reinitialisations;
    private final AppProperties props;
    private final RetourGoogleMobile googleMobile;

    public AuthController(AuthService auth, InscriptionService inscriptions,
                          ReinitialisationService reinitialisations, AppProperties props,
                          RetourGoogleMobile googleMobile)
    {
        this.googleMobile = googleMobile;
        this.auth = auth;
        this.inscriptions = inscriptions;
        this.reinitialisations = reinitialisations;
        this.props = props;
    }

    @GetMapping("/config")
    public ConfigResponse config()
    {
        return new ConfigResponse(props.googleActif());
    }

    @PostMapping("/inscription")
    public InscriptionAttenteResponse inscription(@Valid @RequestBody InscriptionRequest req)
    {
        return inscriptions.demander(req);
    }

    @PostMapping("/inscription/verifier")
    public ConnexionResponse verifierEmail(@Valid @RequestBody VerificationEmailRequest req)
    {
        return inscriptions.verifier(req);
    }

    @PostMapping("/inscription/renvoyer")
    public InscriptionAttenteResponse renvoyerCode(@Valid @RequestBody RenvoiCodeRequest req)
    {
        return inscriptions.renvoyer(req);
    }

    @PostMapping("/mot-de-passe/oublie")
    public ResponseEntity<Void> motDePasseOublie(@Valid @RequestBody MotDePasseOublieRequest req)
    {
        reinitialisations.demander(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mot-de-passe/reinitialiser")
    public ResponseEntity<Void> reinitialiserMotDePasse(@Valid @RequestBody ReinitialisationRequest req)
    {
        reinitialisations.reinitialiser(req);
        return ResponseEntity.noContent().build();
    }

    /** Application mobile : echange le code recu de madeqrcard://auth contre une session. */
    @PostMapping("/google/echange")
    public ConnexionResponse echangeGoogle(@Valid @RequestBody GoogleEchangeRequest req)
    {
        return googleMobile.echanger(req.code(), req.verificateur());
    }

    @PostMapping("/connexion")
    public ConnexionResponse connexion(@Valid @RequestBody ConnexionRequest req)
    {
        return auth.connecter(req);
    }

    @PostMapping("/2fa")
    public ConnexionResponse deuxiemeFacteur(@Valid @RequestBody MfaRequest req)
    {
        return auth.validerCode(req);
    }
}
