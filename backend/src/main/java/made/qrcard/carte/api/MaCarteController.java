package made.qrcard.carte.api;

import made.qrcard.carte.dto.CarteDto.*;
import made.qrcard.carte.service.CarteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static made.qrcard.commun.securite.SecuriteUtils.idCourant;


/**
 * Carte personnelle (/api/me/carte) ou professionnelle d'une organisation (/api/me/organisations/{id}/carte)
 * du compte connecte : seul son titulaire peut la modifier.
 */
@RestController
public class MaCarteController
{

    private static final String PERSO = "/api/me/carte";
    private static final String PRO = "/api/me/organisations/{organisationId}/carte";

    private final CarteService cartes;

    public MaCarteController(CarteService cartes)
    {
        this.cartes = cartes;
    }

    @GetMapping("/api/me/cartes-pro")
    public List<CarteResponse> cartesPro()
    {
        return cartes.mesCartesPro(idCourant());
    }

    @GetMapping({PERSO, PRO})
    public CarteResponse lire(@PathVariable(required = false) UUID organisationId)
    {
        return cartes.maCarte(idCourant(), organisationId);
    }

    @PutMapping({PERSO, PRO})
    public CarteResponse modifier(@PathVariable(required = false) UUID organisationId, @Valid @RequestBody CarteRequest req)
    {
        return cartes.modifier(idCourant(), organisationId, req);
    }

    @PostMapping({PERSO + "/photo", PRO + "/photo"})
    public CarteResponse photo(@PathVariable(required = false) UUID organisationId, @RequestParam("fichier") MultipartFile fichier)
            throws IOException
    {
        return cartes.enregistrerPhoto(idCourant(), organisationId, fichier);
    }

    @DeleteMapping({PERSO + "/photo", PRO + "/photo"})
    public CarteResponse supprimerPhoto(@PathVariable(required = false) UUID organisationId)
    {
        return cartes.supprimerPhoto(idCourant(), organisationId);
    }

    @PostMapping({PERSO + "/regenerer-lien", PRO + "/regenerer-lien"})
    public CarteResponse regenererLien(@PathVariable(required = false) UUID organisationId)
    {
        return cartes.regenererLien(idCourant(), organisationId);
    }
}
