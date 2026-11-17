package made.qrcard.compte.api;

import made.qrcard.compte.dto.CompteDto.*;
import made.qrcard.compte.service.ProfilService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static made.qrcard.commun.securite.SecuriteUtils.idCourant;


@RestController
@RequestMapping("/api/me")
public class ProfilController
{

    private final ProfilService profils;

    public ProfilController(ProfilService profils)
    {
        this.profils = profils;
    }

    @GetMapping
    public ProfilResponse profil()
    {
        return profils.profil(idCourant());
    }

    @PutMapping("/mot-de-passe")
    public ProfilResponse motDePasse(@Valid @RequestBody MotDePasseRequest req)
    {
        return profils.changerMotDePasse(idCourant(), req);
    }

    @PostMapping("/2fa/initialiser")
    public TotpInitResponse initialiser2fa()
    {
        return profils.initialiser2fa(idCourant());
    }

    @PostMapping("/2fa/activer")
    public CodesSecoursResponse activer2fa(@Valid @RequestBody CodeRequest req)
    {
        return profils.activer2fa(idCourant(), req);
    }

    @PostMapping("/2fa/desactiver")
    public ProfilResponse desactiver2fa(@Valid @RequestBody CodeRequest req)
    {
        return profils.desactiver2fa(idCourant(), req);
    }
}
