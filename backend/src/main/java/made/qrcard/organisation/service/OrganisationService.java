package made.qrcard.organisation.service;

import made.qrcard.carte.domaine.Carte;
import made.qrcard.carte.domaine.CarteRepository;
import made.qrcard.carte.service.CarteService;
import made.qrcard.commun.web.ApiException;
import made.qrcard.commun.web.PageResponse;
import made.qrcard.compte.domaine.Role;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.organisation.domaine.*;
import made.qrcard.organisation.dto.OrganisationDto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le compte appartient a la personne : l'organisation ne voit que le rattachement
 * (adresse invitee, role) et la carte professionnelle, jamais le compte ni la carte personnelle.
 */
@Service
public class OrganisationService
{

    private static final Set<String> TYPES_LOGO = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int TAILLE_JOURNAL = 20;
    static final int ORGANISATIONS_MAX = 20;

    private final OrganisationRepository organisations;
    private final OrganisationLogoRepository logos;
    private final MembreOrganisationRepository membres;
    private final JournalOrganisationRepository journal;
    private final UtilisateurRepository utilisateurs;
    private final CarteRepository cartes;
    private final CarteService carteService;

    public OrganisationService(OrganisationRepository organisations, OrganisationLogoRepository logos,
                             MembreOrganisationRepository membres, JournalOrganisationRepository journal,
                             UtilisateurRepository utilisateurs, CarteRepository cartes, CarteService carteService)
    {
        this.organisations = organisations;
        this.logos = logos;
        this.membres = membres;
        this.journal = journal;
        this.utilisateurs = utilisateurs;
        this.cartes = cartes;
        this.carteService = carteService;
    }

    /** Organisations du compte, dans l'ordre ou il les a rejointes. */
    @Transactional(readOnly = true)
    public List<OrganisationResponse> miennes(UUID utilisateurId)
    {
        return membres.findByUtilisateurIdOrderByCreatedAtAsc(utilisateurId).stream()
                .map(m -> versDto(charger(m.getOrganisationId()), m))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganisationResponse lire(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation m = membre(utilisateurId, organisationId);
        return versDto(charger(organisationId), m);
    }

    /**
     * Le createur en devient l'administrateur. Son adresse de compte sert de contact : le formulaire le lui signale.
     * Les refus arrivent avant toute ecriture : ils ne doivent pas annuler une inscription qui appelle cette methode.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public OrganisationResponse creer(UUID utilisateurId, OrganisationRequest req)
    {
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"));
        if (u.getRole() == Role.SUPER_ADMIN) throw ApiException.interdit("Un administrateur général ne peut pas créer d'organisation");
        verifierPlace(utilisateurId);
        Organisation e = new Organisation();
        appliquer(e, req);
        organisations.save(e);

        MembreOrganisation m = new MembreOrganisation();
        m.setOrganisationId(e.getId());
        m.setUtilisateurId(utilisateurId);
        m.setEmail(u.getEmail());
        m.setRole(RoleOrganisation.ADMIN);
        membres.save(m);
        carteService.creerProfessionnelle(u, e.getId(), null, null, null);
        tracer(e.getId(), m.getEmail(), "CREATION", e.getNom());
        return versDto(e, m);
    }

    @Transactional
    public OrganisationResponse modifier(UUID utilisateurId, UUID organisationId, OrganisationRequest req)
    {
        MembreOrganisation admin = adminActif(utilisateurId, organisationId);
        Organisation e = charger(admin.getOrganisationId());
        appliquer(e, req);
        tracer(e.getId(), admin.getEmail(), "MODIFICATION", null);
        return versDto(e, admin);
    }

    @Transactional
    public OrganisationResponse enregistrerLogo(UUID utilisateurId, UUID organisationId, MultipartFile fichier) throws IOException
    {
        MembreOrganisation admin = adminActif(utilisateurId, organisationId);
        if (fichier == null || fichier.isEmpty()) throw ApiException.invalide("Aucun fichier reçu");
        if (!TYPES_LOGO.contains(fichier.getContentType())) throw ApiException.invalide("Format accepté : JPEG, PNG ou WebP");
        Organisation e = charger(admin.getOrganisationId());
        OrganisationLogo logo = logos.findById(e.getId()).orElseGet(OrganisationLogo::new);
        logo.setOrganisationId(e.getId());
        logo.setDonnees(fichier.getBytes());
        logo.setType(fichier.getContentType());
        logos.save(logo);
        e.setLogoVersion(System.currentTimeMillis());
        return versDto(e, admin);
    }

    @Transactional
    public OrganisationResponse supprimerLogo(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation admin = adminActif(utilisateurId, organisationId);
        Organisation e = charger(admin.getOrganisationId());
        logos.findById(e.getId()).ifPresent(logos::delete);
        e.setLogoVersion(null);
        return versDto(e, admin);
    }

    @Transactional(readOnly = true)
    public Optional<OrganisationLogo> logo(UUID organisationId)
    {
        return logos.findById(organisationId);
    }

    @Transactional(readOnly = true)
    public List<MembreItem> membres(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation admin = admin(utilisateurId, organisationId);
        Map<UUID, Carte> parTitulaire = cartes.findByOrganisationId(admin.getOrganisationId()).stream()
                .collect(Collectors.toMap(c -> c.getUtilisateur().getId(), Function.identity()));
        return membres.findByOrganisationIdOrderByCreatedAtAsc(admin.getOrganisationId()).stream()
                .map(m -> versDto(m, parTitulaire.get(m.getUtilisateurId())))
                .toList();
    }

    @Transactional
    public MembreItem changerRole(UUID utilisateurId, UUID organisationId, UUID membreId, RoleOrganisation role)
    {
        MembreOrganisation admin = adminActif(utilisateurId, organisationId);
        MembreOrganisation cible = membreDe(admin, membreId);
        if (cible.getRole() != role)
        {
            if (cible.estAdmin()) verifierPasDernierAdmin(cible);
            cible.setRole(role);
            tracer(admin.getOrganisationId(), admin.getEmail(), role == RoleOrganisation.ADMIN ? "ROLE_ADMIN" : "ROLE_MEMBRE", cible.getEmail());
        }
        return versDto(cible, cartes.findByUtilisateur_IdAndOrganisationId(cible.getUtilisateurId(), organisationId).orElse(null));
    }

    @Transactional
    public void retirer(UUID utilisateurId, UUID organisationId, UUID membreId)
    {
        MembreOrganisation admin = adminActif(utilisateurId, organisationId);
        MembreOrganisation cible = membreDe(admin, membreId);
        if (cible.getId().equals(admin.getId())) throw ApiException.invalide("Pour partir vous-même, utilisez « Quitter l'organisation »");
        detacher(cible);
        tracer(admin.getOrganisationId(), admin.getEmail(), "RETRAIT", cible.getEmail());
    }

    @Transactional
    public void quitter(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation m = membre(utilisateurId, organisationId);
        if (m.estAdmin()) verifierPasDernierAdmin(m);
        detacher(m);
        tracer(m.getOrganisationId(), m.getEmail(), "DEPART", m.getEmail());
    }

    /** Efface l'organisation et ses cartes professionnelles. Les comptes et les cartes personnelles restent. */
    @Transactional
    public void supprimer(UUID utilisateurId, UUID organisationId)
    {
        organisations.delete(charger(adminActif(utilisateurId, organisationId).getOrganisationId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalItem> journal(UUID utilisateurId, UUID organisationId, int page)
    {
        MembreOrganisation admin = admin(utilisateurId, organisationId);
        var p = journal.findByOrganisationIdOrderByCreatedAtDesc(admin.getOrganisationId(), PageRequest.of(Math.max(page, 0), TAILLE_JOURNAL))
                .map(j -> new JournalItem(j.getCreatedAt(), j.getAuteur(), j.getAction(), j.getCible()));
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    /** Rattachement du compte a cette organisation ; introuvable pour qui n'en est pas membre. */
    MembreOrganisation membre(UUID utilisateurId, UUID organisationId)
    {
        return membres.findByOrganisationIdAndUtilisateurId(organisationId, utilisateurId)
                .orElseThrow(() -> ApiException.introuvable("Vous n'appartenez pas à cette organisation"));
    }

    /** Administrateur de cette organisation, y compris bloquee : lecture seule dans ce cas. */
    MembreOrganisation admin(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation m = membre(utilisateurId, organisationId);
        if (!m.estAdmin()) throw ApiException.interdit("Réservé aux administrateurs de l'organisation");
        return m;
    }

    /** Une personne peut rejoindre plusieurs organisations, dans une limite raisonnable. */
    void verifierPlace(UUID utilisateurId)
    {
        if (membres.countByUtilisateurId(utilisateurId) >= ORGANISATIONS_MAX)
        {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Vous appartenez déjà à " + ORGANISATIONS_MAX + " organisations, le maximum : quittez-en une avant d'en rejoindre une autre");
        }
    }

    MembreOrganisation adminActif(UUID utilisateurId, UUID organisationId)
    {
        MembreOrganisation admin = admin(utilisateurId, organisationId);
        if (!charger(admin.getOrganisationId()).estActive()) throw ApiException.interdit("Cette organisation est bloquée");
        return admin;
    }

    void tracer(UUID organisationId, String auteur, String action, String cible)
    {
        JournalOrganisation j = new JournalOrganisation();
        j.setOrganisationId(organisationId);
        j.setAuteur(auteur);
        j.setAction(action);
        j.setCible(cible);
        journal.save(j);
    }

    Organisation charger(UUID id)
    {
        return organisations.findById(id).orElseThrow(() -> ApiException.introuvable("Organisation introuvable"));
    }

    private MembreOrganisation membreDe(MembreOrganisation admin, UUID membreId)
    {
        return membres.findById(membreId)
                .filter(m -> m.getOrganisationId().equals(admin.getOrganisationId()))
                .orElseThrow(() -> ApiException.introuvable("Membre introuvable"));
    }

    private void detacher(MembreOrganisation m)
    {
        carteService.supprimerProfessionnelle(m.getUtilisateurId(), m.getOrganisationId());
        membres.delete(m);
    }

    private void verifierPasDernierAdmin(MembreOrganisation m)
    {
        if (membres.countByOrganisationIdAndRole(m.getOrganisationId(), RoleOrganisation.ADMIN) <= 1)
        {
            throw new ApiException(HttpStatus.CONFLICT, "Nommez d'abord un autre administrateur, ou supprimez l'organisation");
        }
    }

    /** Controles qui ne dependent pas de la base, faits aussi avant une creation differee. Aucun pour l'instant. */
    static void valider(OrganisationRequest req)
    {
    }

    private static void appliquer(Organisation e, OrganisationRequest req)
    {
        e.setNom(req.nom().replaceAll("\\p{Cntrl}", " ").trim());
        e.setType(req.type());
        // La precision n'a de sens que pour « Autre » ; vide, on affiche simplement « Autre »
        String precision = req.typePrecision() == null ? "" : req.typePrecision().replaceAll("\\p{Cntrl}", " ").trim();
        e.setTypePrecision(req.type() == TypeOrganisation.AUTRE && !precision.isEmpty() ? precision : null);
        e.setAdresse(req.adresse() == null || req.adresse().isBlank() ? null : req.adresse().trim());
        e.setCouleur(req.couleur().toLowerCase());
    }

    private static OrganisationResponse versDto(Organisation e, MembreOrganisation m)
    {
        return new OrganisationResponse(e.getId(), e.getNom(), e.getType(), e.getTypePrecision(), e.getAdresse(), e.getCouleur(), e.urlLogo(),
                e.getStatut().name(), e.getMotifBlocage(), m.getRole(), m.getEmail());
    }

    private static MembreItem versDto(MembreOrganisation m, Carte c)
    {
        String nom = c == null ? null : String.join(" ", c.getPrenom() == null ? "" : c.getPrenom(), c.getNom() == null ? "" : c.getNom()).trim();
        return new MembreItem(m.getId(), m.getEmail(), nom, c == null ? null : c.getProfession(), m.getRole(), m.getCreatedAt(),
                c == null ? null : c.getJeton());
    }
}
