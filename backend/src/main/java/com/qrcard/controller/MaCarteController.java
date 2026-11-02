package com.qrcard.controller;

import com.qrcard.dto.Dto.*;
import com.qrcard.service.CarteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.qrcard.securite.SecuriteUtils.idCourant;

/** La carte du compte connecte : seul son proprietaire peut la modifier. */
@RestController
@RequestMapping("/api/me/carte")
public class MaCarteController {

    private final CarteService cartes;

    public MaCarteController(CarteService cartes) {
        this.cartes = cartes;
    }

    @GetMapping
    public CarteResponse lire() {
        return cartes.maCarte(idCourant());
    }

    @PutMapping
    public CarteResponse modifier(@Valid @RequestBody CarteRequest req) {
        return cartes.modifier(idCourant(), req);
    }

    @PostMapping("/photo")
    public CarteResponse photo(@RequestParam("fichier") MultipartFile fichier) throws IOException {
        return cartes.enregistrerPhoto(idCourant(), fichier);
    }

    @DeleteMapping("/photo")
    public CarteResponse supprimerPhoto() {
        return cartes.supprimerPhoto(idCourant());
    }

    @PostMapping("/regenerer-lien")
    public CarteResponse regenererLien() {
        return cartes.regenererLien(idCourant());
    }
}
