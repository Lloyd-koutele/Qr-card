package com.qrcard.service;

import com.qrcard.dto.Dto.*;
import com.qrcard.entite.*;
import com.qrcard.repository.CarteEnregistreeRepository;
import com.qrcard.repository.CartePhotoRepository;
import com.qrcard.repository.CarteRepository;
import com.qrcard.repository.UtilisateurRepository;
import com.qrcard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CarteService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LONGUEUR_JETON = 14;
    private static final Set<String> TYPES_PHOTO = Set.of("image/jpeg", "image/png", "image/webp");

    private final CarteRepository cartes;
    private final CartePhotoRepository photos;
    private final CarteEnregistreeRepository enregistrements;
    private final UtilisateurRepository utilisateurs;
    private final SecureRandom random = new SecureRandom();

    public CarteService(CarteRepository cartes, CartePhotoRepository photos,
                        CarteEnregistreeRepository enregistrements, UtilisateurRepository utilisateurs) {
        this.cartes = cartes;
        this.photos = photos;
        this.enregistrements = enregistrements;
        this.utilisateurs = utilisateurs;
    }

    @Transactional
    public Carte creerPour(Utilisateur u) {
        Carte c = new Carte();
        c.setUtilisateur(u);
        c.setJeton(nouveauJeton());
        c.setNumero(String.format("QRC-%d-%05d", Year.now().getValue(), cartes.prochainNumero()));
        String[] morceaux = u.getNomAffiche().trim().split("\\s+", 2);
        c.setPrenom(morceaux[0]);
        if (morceaux.length > 1) c.setNom(morceaux[1]);
        return cartes.save(c);
    }

    @Transactional
    public CarteResponse maCarte(UUID utilisateurId) {
        return versDto(carteDe(utilisateurId), utilisateurId);
    }

    @Transactional
    public CarteResponse modifier(UUID utilisateurId, CarteRequest req) {
        Carte c = carteDe(utilisateurId);
        c.setNom(propre(req.nom()));
        c.setPrenom(propre(req.prenom()));
        c.setProfession(propre(req.profession()));
        c.setBio(propre(req.bio()));
        c.setTheme(req.theme());

        c.getContacts().clear();
        List<ContactInput> saisis = req.contacts() == null ? List.of() : req.contacts();
        for (int i = 0; i < saisis.size(); i++) {
            ContactInput saisi = saisis.get(i);
            Contact contact = new Contact();
            contact.setCarte(c);
            contact.setType(saisi.type());
            contact.setValeur(saisi.type().normaliser(saisi.valeur()));
            contact.setOrdre(i);
            c.getContacts().add(contact);
        }
        c.setUpdatedAt(Instant.now());
        return versDto(c, utilisateurId);
    }

    @Transactional
    public CarteResponse enregistrerPhoto(UUID utilisateurId, MultipartFile fichier) throws IOException {
        if (fichier == null || fichier.isEmpty()) throw ApiException.invalide("Aucun fichier reçu");
        if (!TYPES_PHOTO.contains(fichier.getContentType())) throw ApiException.invalide("Format accepté : JPEG, PNG ou WebP");
        Carte c = carteDe(utilisateurId);
        CartePhoto photo = photos.findById(c.getId()).orElseGet(CartePhoto::new);
        photo.setCarteId(c.getId());
        photo.setDonnees(fichier.getBytes());
        photo.setType(fichier.getContentType());
        photos.save(photo);
        c.setPhotoVersion(System.currentTimeMillis());
        return versDto(c, utilisateurId);
    }

    @Transactional
    public CarteResponse supprimerPhoto(UUID utilisateurId) {
        Carte c = carteDe(utilisateurId);
        photos.deleteById(c.getId());
        c.setPhotoVersion(null);
        return versDto(c, utilisateurId);
    }

    /** Nouveau lien : l'ancien QR code ne marche plus et la carte disparait des carnets. */
    @Transactional
    public CarteResponse regenererLien(UUID utilisateurId) {
        Carte c = carteDe(utilisateurId);
        c.setJeton(nouveauJeton());
        enregistrements.supprimerPourCarte(c.getId());
        return versDto(c, utilisateurId);
    }

    @Transactional(readOnly = true)
    public CarteResponse lire(String jeton, Optional<UUID> lecteur) {
        return versDto(carteDisponible(jeton), lecteur.orElse(null));
    }

    @Transactional(readOnly = true)
    public CartePhoto photo(String jeton) {
        Carte c = carteDisponible(jeton);
        return photos.findById(c.getId()).orElseThrow(() -> ApiException.introuvable("Pas de photo"));
    }

    @Transactional(readOnly = true)
    public String vcard(String jeton) {
        Carte c = carteDisponible(jeton);
        StringBuilder v = new StringBuilder("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        String nom = echapper(c.getNom()), prenom = echapper(c.getPrenom());
        v.append("N:").append(nom).append(';').append(prenom).append(";;;\r\n");
        v.append("FN:").append((prenom + " " + nom).trim()).append("\r\n");
        if (c.getProfession() != null) v.append("TITLE:").append(echapper(c.getProfession())).append("\r\n");
        for (Contact ct : c.getContacts()) {
            switch (ct.getType()) {
                case TELEPHONE -> v.append("TEL;TYPE=CELL:").append(ct.getValeur()).append("\r\n");
                case EMAIL -> v.append("EMAIL:").append(ct.getValeur()).append("\r\n");
                default -> v.append("URL;TYPE=").append(ct.getType().libelle().replace(" ", ""))
                        .append(':').append(ct.getType().lien(ct.getValeur())).append("\r\n");
            }
        }
        if (c.getBio() != null) v.append("NOTE:").append(echapper(c.getBio())).append("\r\n");
        return v.append("END:VCARD\r\n").toString();
    }

    /** Carte lisible : elle existe et son proprietaire n'est pas bloque. */
    Carte carteDisponible(String jeton) {
        return cartes.findByJeton(jeton)
                .filter(c -> c.getUtilisateur().estActif())
                .orElseThrow(() -> ApiException.introuvable("Carte indisponible"));
    }

    CarteResponse versDto(Carte c, UUID lecteur) {
        boolean proprietaire = lecteur != null && lecteur.equals(c.getUtilisateur().getId());
        boolean enregistree = lecteur != null && !proprietaire
                && enregistrements.existsByUtilisateurIdAndCarte_Id(lecteur, c.getId());
        List<ContactDto> contacts = c.getContacts().stream()
                .map(ct -> new ContactDto(ct.getType(), ct.getType().libelle(), ct.getValeur(), ct.getType().lien(ct.getValeur())))
                .toList();
        return new CarteResponse(c.getJeton(), c.getNumero(), c.getNom(), c.getPrenom(), c.getProfession(), c.getBio(),
                c.getTheme(), c.getPhotoVersion(), contacts, c.getCreatedAt(), proprietaire, enregistree,
                lecteur != null, proprietaire ? enregistrements.countByCarte_Id(c.getId()) : null);
    }

    private Carte carteDe(UUID utilisateurId) {
        return cartes.findByUtilisateur_Id(utilisateurId).orElseGet(() -> creerPour(
                utilisateurs.findById(utilisateurId)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"))));
    }

    private String nouveauJeton() {
        String jeton;
        do {
            StringBuilder sb = new StringBuilder(LONGUEUR_JETON);
            for (int i = 0; i < LONGUEUR_JETON; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            jeton = sb.toString();
        } while (cartes.existsByJeton(jeton));
        return jeton;
    }

    private static String propre(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
