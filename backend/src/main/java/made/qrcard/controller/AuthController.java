package made.qrcard.controller;

import made.qrcard.config.AppProperties;
import made.qrcard.dto.Dto.*;
import made.qrcard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AppProperties props;

    public AuthController(AuthService auth, AppProperties props) {
        this.auth = auth;
        this.props = props;
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(props.googleActif());
    }

    @PostMapping("/inscription")
    public ConnexionResponse inscription(@Valid @RequestBody InscriptionRequest req) {
        return auth.inscrire(req);
    }

    @PostMapping("/connexion")
    public ConnexionResponse connexion(@Valid @RequestBody ConnexionRequest req) {
        return auth.connecter(req);
    }

    @PostMapping("/2fa")
    public ConnexionResponse deuxiemeFacteur(@Valid @RequestBody MfaRequest req) {
        return auth.validerCode(req);
    }
}
