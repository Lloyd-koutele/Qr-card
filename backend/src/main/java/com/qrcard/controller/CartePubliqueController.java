package com.qrcard.controller;

import com.qrcard.dto.Dto.CarteResponse;
import com.qrcard.entite.CartePhoto;
import com.qrcard.service.CarteService;
import com.qrcard.securite.SecuriteUtils;
import org.springframework.http.CacheControl;
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
public class CartePubliqueController {

    private final CarteService cartes;

    public CartePubliqueController(CarteService cartes) {
        this.cartes = cartes;
    }

    @GetMapping
    public ResponseEntity<CarteResponse> lire(@PathVariable String jeton) {
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex, nofollow")
                .cacheControl(CacheControl.noStore())
                .body(cartes.lire(jeton, SecuriteUtils.idOptionnel()));
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(@PathVariable String jeton) {
        CartePhoto photo = cartes.photo(jeton);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(photo.getDonnees());
    }

    @GetMapping("/vcard")
    public ResponseEntity<byte[]> vcard(@PathVariable String jeton) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "vcard", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contact.vcf\"")
                .body(cartes.vcard(jeton).getBytes(StandardCharsets.UTF_8));
    }
}
