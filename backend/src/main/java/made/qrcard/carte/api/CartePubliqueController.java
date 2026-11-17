package made.qrcard.carte.api;

import made.qrcard.carte.domaine.CartePhoto;
import made.qrcard.carte.dto.CarteDto.*;
import made.qrcard.carte.service.CarteService;
import made.qrcard.carte.service.VcardService;
import made.qrcard.carte.service.VcardService.FichierVcard;
import made.qrcard.commun.securite.SecuriteUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lecture seule d'une carte par son lien. Le jeton du lien suffit : pas de
 * compte requis. Si le lecteur est connecte, on indique s'il l'a enregistree.
 */
@RestController
@RequestMapping("/api/public/cartes/{jeton}")
public class CartePubliqueController
{

    private final CarteService cartes;
    private final VcardService vcards;

    public CartePubliqueController(CarteService cartes, VcardService vcards)
    {
        this.cartes = cartes;
        this.vcards = vcards;
    }

    @GetMapping
    public ResponseEntity<CarteResponse> lire(@PathVariable String jeton)
    {
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex, nofollow")
                .cacheControl(CacheControl.noStore())
                .body(cartes.lire(jeton, SecuriteUtils.idOptionnel()));
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(@PathVariable String jeton)
    {
        CartePhoto photo = cartes.photo(jeton);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(photo.getDonnees());
    }

    @GetMapping("/vcard")
    public ResponseEntity<byte[]> vcard(@PathVariable String jeton)
    {
        FichierVcard fichier = vcards.vcard(jeton);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType("text", "vcard", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier.nom()).build().toString())
                .body(fichier.contenu().getBytes(StandardCharsets.UTF_8));
    }
}
