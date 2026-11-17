package made.qrcard.carte.service;

import made.qrcard.carte.domaine.Carte;
import made.qrcard.carte.domaine.CartePhoto;
import made.qrcard.carte.domaine.CartePhotoRepository;
import made.qrcard.carte.domaine.CarteRepository;
import made.qrcard.carte.domaine.Contact;
import made.qrcard.carte.domaine.TypeContact;
import made.qrcard.carte.dto.CarteDto.*;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.service.CompteCree;
import org.springframework.context.event.EventListener;
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
public class CarteService
{

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LONGUEUR_JETON = 14;
    private static final Set<String> TYPES_PHOTO = Set.of("image/jpeg", "image/png", "image/webp");

    private final CarteRepository cartes;
    private final CartePhotoRepository photos;
    private final EnregistrementsDeCarte enregistrements;
    private final UtilisateurRepository utilisateurs;
    private final MarquesDeCarte marques;
    private final SecureRandom random = new SecureRandom();

    public CarteService(CarteRepository cartes, CartePhotoRepository photos,
                        EnregistrementsDeCarte enregistrements, UtilisateurRepository utilisateurs,
                        MarquesDeCarte marques)
    {
        this.cartes = cartes;
        this.photos = photos;
        this.enregistrements = enregistrements;
        this.utilisateurs = utilisateurs;
        this.marques = marques;
    }

    /** Chaque nouveau compte recoit sa carte, dans la meme transaction. */
    @EventListener
    public void surCompteCree(CompteCree evenement)
    {
        creerPour(evenement.utilisateur());
    }

    @Transactional
    public Carte creerPour(Utilisateur u)
    {
        return cartes.save(nouvelleCarte(u, u.getNomAffiche()));
    }

    /** Carte professionnelle, distincte de la carte personnelle : autre lien, autre QR code. */
    @Transactional
    public Carte creerProfessionnelle(Utilisateur u, UUID organisationId, String nom, String poste, String emailPro)
    {
        Carte c = nouvelleCarte(u, nom == null || nom.isBlank() ? u.getNomAffiche() : nom);
        c.setOrganisationId(organisationId);
        c.setProfession(propre(poste));
        if (emailPro != null)
        {
            Contact contact = new Contact();
            contact.setCarte(c);
            contact.setType(TypeContact.EMAIL);
            contact.setValeur(TypeContact.EMAIL.normaliser(emailPro));
            c.getContacts().add(contact);
        }
        return cartes.save(c);
    }

    /** Au depart d'une organisation : le lien ne repond plus et la carte quitte les carnets. Les autres cartes ne bougent pas. */
    @Transactional
    public void supprimerProfessionnelle(UUID utilisateurId, UUID organisationId)
    {
        cartes.findByUtilisateur_IdAndOrganisationId(utilisateurId, organisationId).ifPresent(cartes::delete);
    }

    /** Cartes professionnelles du compte, une par organisation active (celles d'une organisation bloquee sont indisponibles). */
    @Transactional(readOnly = true)
    public List<CarteResponse> mesCartesPro(UUID utilisateurId)
    {
        return cartes.findByUtilisateur_IdAndOrganisationIdIsNotNullOrderByCreatedAtAsc(utilisateurId).stream()
                .filter(c -> marque(c).isPresent())
                .map(c -> versDto(c, utilisateurId))
                .toList();
    }

    /** Carte professionnelle d'un membre dans une organisation donnee, pour la liste des membres. */
    @Transactional(readOnly = true)
    public Optional<Carte> professionnelle(UUID utilisateurId, UUID organisationId)
    {
        return cartes.findByUtilisateur_IdAndOrganisationId(utilisateurId, organisationId);
    }

    @Transactional
    public CarteResponse maCarte(UUID utilisateurId, UUID organisationId)
    {
        return versDto(carteDe(utilisateurId, organisationId), utilisateurId);
    }

    @Transactional
    public CarteResponse modifier(UUID utilisateurId, UUID organisationId, CarteRequest req)
    {
        Carte c = carteDe(utilisateurId, organisationId);
        c.setNom(propre(req.nom()));
        c.setPrenom(propre(req.prenom()));
        c.setProfession(propre(req.profession()));
        c.setBio(propre(req.bio()));
        // Une carte professionnelle garde la couleur de son organisation
        if (organisationId == null)
        {
            String couleur = propre(req.couleur());
            c.setCouleur(couleur == null ? null : couleur.toLowerCase());
        }

        c.getContacts().clear();
        List<ContactInput> saisis = req.contacts() == null ? List.of() : req.contacts();
        for (int i = 0; i < saisis.size(); i++)
        {
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
    public CarteResponse enregistrerPhoto(UUID utilisateurId, UUID organisationId, MultipartFile fichier) throws IOException
    {
        if (fichier == null || fichier.isEmpty()) throw ApiException.invalide("Aucun fichier reçu");
        if (!TYPES_PHOTO.contains(fichier.getContentType())) throw ApiException.invalide("Format accepté : JPEG, PNG ou WebP");
        Carte c = carteDe(utilisateurId, organisationId);
        CartePhoto photo = photos.findById(c.getId()).orElseGet(CartePhoto::new);
        photo.setCarteId(c.getId());
        photo.setDonnees(fichier.getBytes());
        photo.setType(fichier.getContentType());
        photos.save(photo);
        c.setPhotoVersion(System.currentTimeMillis());
        return versDto(c, utilisateurId);
    }

    @Transactional
    public CarteResponse supprimerPhoto(UUID utilisateurId, UUID organisationId)
    {
        Carte c = carteDe(utilisateurId, organisationId);
        photos.deleteById(c.getId());
        c.setPhotoVersion(null);
        return versDto(c, utilisateurId);
    }

    /** Nouveau lien : l'ancien QR code ne marche plus et la carte disparait des carnets. */
    @Transactional
    public CarteResponse regenererLien(UUID utilisateurId, UUID organisationId)
    {
        Carte c = carteDe(utilisateurId, organisationId);
        c.setJeton(nouveauJeton());
        enregistrements.oublier(c.getId());
        return versDto(c, utilisateurId);
    }

    @Transactional(readOnly = true)
    public CarteResponse lire(String jeton, Optional<UUID> lecteur)
    {
        return versDto(carteDisponible(jeton), lecteur.orElse(null));
    }

    @Transactional(readOnly = true)
    public CartePhoto photo(String jeton)
    {
        Carte c = carteDisponible(jeton);
        return photos.findById(c.getId()).orElseThrow(() -> ApiException.introuvable("Pas de photo"));
    }

    /** Son proprietaire n'est pas bloque et, pour une carte professionnelle, son organisation non plus. */
    public boolean estDisponible(Carte c)
    {
        return c.getUtilisateur().estActif() && (c.getOrganisationId() == null || marque(c).isPresent());
    }

    public Carte carteDisponible(String jeton)
    {
        return cartes.findByJeton(jeton)
                .filter(this::estDisponible)
                .orElseThrow(() -> ApiException.introuvable("Carte indisponible"));
    }

    public CarteResponse versDto(Carte c, UUID lecteur)
    {
        boolean proprietaire = lecteur != null && lecteur.equals(c.getUtilisateur().getId());
        boolean enregistree = lecteur != null && !proprietaire
                && enregistrements.estEnregistree(lecteur, c.getId());
        List<ContactDto> contacts = c.getContacts().stream()
                .map(ct -> new ContactDto(ct.getType(), ct.getType().libelle(), ct.getValeur(), ct.getType().lien(ct.getValeur())))
                .toList();
        return new CarteResponse(c.getJeton(), c.getNumero(), c.getNom(), c.getPrenom(), c.getProfession(), c.getBio(),
                c.getOrganisationId() == null ? c.getCouleur() : null, c.getPhotoVersion(), contacts, c.getCreatedAt(), proprietaire, enregistree,
                lecteur != null, proprietaire ? enregistrements.compter(c.getId()) : null, marque(c).orElse(null));
    }

    Optional<MarqueDto> marque(Carte c)
    {
        return c.getOrganisationId() == null ? Optional.empty() : marques.marque(c.getOrganisationId());
    }

    private Carte nouvelleCarte(Utilisateur u, String nomComplet)
    {
        Carte c = new Carte();
        c.setUtilisateur(u);
        c.setJeton(nouveauJeton());
        c.setNumero(String.format("QRC-%d-%05d", Year.now().getValue(), cartes.prochainNumero()));
        String[] morceaux = nomComplet.trim().split("\\s+", 2);
        c.setPrenom(morceaux[0]);
        if (morceaux.length > 1) c.setNom(morceaux[1]);
        return c;
    }

    /** organisationId vide : carte personnelle. Sinon la carte professionnelle, qui n'existe que pour un membre. */
    private Carte carteDe(UUID utilisateurId, UUID organisationId)
    {
        if (organisationId != null)
        {
            return cartes.findByUtilisateur_IdAndOrganisationId(utilisateurId, organisationId)
                    .orElseThrow(() -> ApiException.introuvable("Vous n'avez pas de carte dans cette organisation"));
        }
        return cartes.findByUtilisateur_IdAndOrganisationIdIsNull(utilisateurId).orElseGet(() -> creerPour(
                utilisateurs.findById(utilisateurId)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"))));
    }

    private String nouveauJeton()
    {
        String jeton;
        do
        {
            StringBuilder sb = new StringBuilder(LONGUEUR_JETON);
            for (int i = 0; i < LONGUEUR_JETON; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            jeton = sb.toString();
        } while (cartes.existsByJeton(jeton));
        return jeton;
    }

    private static String propre(String s)
    {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
