package made.qrcard.organisation.service;

import made.qrcard.carte.service.CarteService;
import made.qrcard.commun.config.AppProperties;
import made.qrcard.commun.email.EnvoiEmail;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.domaine.Role;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.dto.CompteDto.ConnexionResponse;
import made.qrcard.compte.service.AuthService;
import made.qrcard.compte.service.InscriptionService;
import made.qrcard.organisation.domaine.*;
import made.qrcard.organisation.dto.OrganisationDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * L'organisation invite une adresse, elle ne cree ni n'ajoute jamais de compte.
 * L'envoi ne consulte pas les comptes : la reponse est la meme qu'un compte
 * existe ou non, et rien n'est rattache tant que la personne n'a pas accepte.
 */
@Service
public class InvitationService
{

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);
    private static final Duration VALIDITE = Duration.ofDays(7);
    /** Une adresse ne recoit qu'une invitation par jour de la meme organisation, meme apres un refus. */
    private static final Duration DELAI_RENVOI = Duration.ofHours(24);
    private static final int QUOTA_PAR_JOUR = 100;

    private final InvitationOrganisationRepository invitations;
    private final MembreOrganisationRepository membres;
    private final UtilisateurRepository utilisateurs;
    private final OrganisationService organisations;
    private final CarteService cartes;
    private final InscriptionService inscriptions;
    private final AuthService auth;
    private final EnvoiEmail emails;
    private final TaskExecutor arrierePlan;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public InvitationService(InvitationOrganisationRepository invitations, MembreOrganisationRepository membres,
                             UtilisateurRepository utilisateurs, OrganisationService organisations, CarteService cartes,
                             InscriptionService inscriptions, AuthService auth, EnvoiEmail emails,
                             TaskExecutor arrierePlan, AppProperties props)
    {
        this.invitations = invitations;
        this.membres = membres;
        this.utilisateurs = utilisateurs;
        this.organisations = organisations;
        this.cartes = cartes;
        this.inscriptions = inscriptions;
        this.auth = auth;
        this.emails = emails;
        this.arrierePlan = arrierePlan;
        this.props = props;
    }

    // Pas de transaction : chaque invitation est enregistree avant que son email parte en arriere-plan
    public InvitationsResultat inviter(UUID utilisateurId, UUID organisationId, InvitationsRequest req)
    {
        MembreOrganisation admin = organisations.adminActif(utilisateurId, organisationId);
        String nomOrganisation = organisations.charger(organisationId).getNom();
        Instant maintenant = Instant.now();
        long restant = QUOTA_PAR_JOUR - invitations.countByOrganisationIdAndEnvoyeLeAfter(organisationId, maintenant.minus(DELAI_RENVOI));

        int envoyees = 0;
        List<InvitationIgnoree> ignorees = new ArrayList<>();
        for (InvitationSaisie saisie : req.invitations())
        {
            String email = AuthService.normaliserEmail(saisie.email());
            InvitationOrganisation i = invitations.findByOrganisationIdAndEmail(organisationId, email).orElse(null);
            if (membres.existsByOrganisationIdAndEmail(organisationId, email))
            {
                ignorees.add(new InvitationIgnoree(email, "Déjà membre de l'organisation"));
            }
            else if (i != null && i.getEnvoyeLe().plus(DELAI_RENVOI).isAfter(maintenant))
            {
                ignorees.add(new InvitationIgnoree(email, "Déjà invitée il y a moins de 24 heures"));
            }
            else if (envoyees >= restant)
            {
                ignorees.add(new InvitationIgnoree(email, "Limite de " + QUOTA_PAR_JOUR + " invitations par jour atteinte"));
            }
            else
            {
                envoyer(i, organisationId, nomOrganisation, email, saisie, maintenant);
                organisations.tracer(organisationId, admin.getEmail(), "INVITATION", email);
                envoyees++;
            }
        }
        return new InvitationsResultat(envoyees, ignorees);
    }

    @Transactional(readOnly = true)
    public List<InvitationItem> lister(UUID utilisateurId, UUID organisationId)
    {
        Instant maintenant = Instant.now();
        return invitations.findByOrganisationIdAndStatutNotOrderByEnvoyeLeDesc(organisations.admin(utilisateurId, organisationId).getOrganisationId(), StatutInvitation.ACCEPTEE)
                .stream()
                .map(i -> new InvitationItem(i.getId(), i.getEmail(), i.getNom(), i.getPoste(),
                        i.estValable(maintenant) ? "EN_ATTENTE" : "TERMINEE", i.getEnvoyeLe(), i.getExpireLe()))
                .toList();
    }

    /** Le lien cesse de fonctionner. La ligne reste pour que le delai de 24 heures s'applique toujours. */
    @Transactional
    public void annuler(UUID utilisateurId, UUID organisationId, UUID invitationId)
    {
        MembreOrganisation admin = organisations.adminActif(utilisateurId, organisationId);
        InvitationOrganisation i = invitations.findById(invitationId)
                .filter(x -> x.getOrganisationId().equals(admin.getOrganisationId()))
                .orElseThrow(() -> ApiException.introuvable("Invitation introuvable"));
        if (i.estValable(Instant.now()))
        {
            i.setStatut(StatutInvitation.REFUSEE);
            organisations.tracer(admin.getOrganisationId(), admin.getEmail(), "ANNULATION_INVITATION", i.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public InvitationPublique consulter(String jeton)
    {
        InvitationOrganisation i = valable(jeton);
        Organisation e = organisations.charger(i.getOrganisationId());
        return new InvitationPublique(e.getId(), e.getNom(), e.getCouleur(), e.urlLogo(), i.getEmail(), i.getNom(), i.getPoste());
    }

    /** Le compte connecte peut avoir une autre adresse que celle invitee : l'organisation ne la verra pas. */
    @Transactional
    public void accepter(UUID utilisateurId, String jeton)
    {
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"));
        rattacher(u, valable(jeton));
    }

    @Transactional
    public void refuser(String jeton)
    {
        valable(jeton).setStatut(StatutInvitation.REFUSEE);
    }

    /** Recevoir le lien prouve l'acces a la boite : le code a 6 chiffres n'est pas redemande. */
    @Transactional
    public ConnexionResponse creerCompte(String jeton, CompteInvitationRequest req)
    {
        InvitationOrganisation i = valable(jeton);
        Utilisateur u = inscriptions.creerCompteVerifie(i.getEmail(), req.nomAffiche(), req.motDePasse());
        rattacher(u, i);
        return auth.connexionReussie(u);
    }

    private void rattacher(Utilisateur u, InvitationOrganisation i)
    {
        if (u.getRole() == Role.SUPER_ADMIN) throw ApiException.interdit("Un administrateur général ne peut pas rejoindre une organisation");
        if (membres.existsByOrganisationIdAndUtilisateurId(i.getOrganisationId(), u.getId()))
        {
            throw new ApiException(HttpStatus.CONFLICT, "Vous appartenez déjà à cette organisation");
        }
        organisations.verifierPlace(u.getId());
        if (membres.existsByOrganisationIdAndEmail(i.getOrganisationId(), i.getEmail()))
        {
            throw new ApiException(HttpStatus.CONFLICT, "Cette adresse est déjà rattachée à l'organisation");
        }
        MembreOrganisation m = new MembreOrganisation();
        m.setOrganisationId(i.getOrganisationId());
        m.setUtilisateurId(u.getId());
        m.setEmail(i.getEmail());
        m.setRole(RoleOrganisation.MEMBRE);
        membres.save(m);
        i.setStatut(StatutInvitation.ACCEPTEE);
        cartes.creerProfessionnelle(u, i.getOrganisationId(), i.getNom(), i.getPoste(), i.getEmail());
        organisations.tracer(i.getOrganisationId(), i.getEmail(), "ACCEPTATION", i.getEmail());
    }

    private InvitationOrganisation valable(String jeton)
    {
        Instant maintenant = Instant.now();
        return invitations.findByJetonEmpreinte(empreinte(jeton))
                .filter(i -> i.estValable(maintenant))
                .filter(i -> organisations.charger(i.getOrganisationId()).estActive())
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "Cette invitation n'est plus valable. Demandez-en une nouvelle à l'organisation."));
    }

    private void envoyer(InvitationOrganisation existante, UUID organisationId, String nomOrganisation, String email,
                         InvitationSaisie saisie, Instant maintenant)
    {
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        InvitationOrganisation i = existante != null ? existante : new InvitationOrganisation();
        i.setOrganisationId(organisationId);
        i.setEmail(email);
        i.setNom(propre(saisie.nom()));
        i.setPoste(propre(saisie.poste()));
        i.setJetonEmpreinte(empreinte(jeton));
        i.setStatut(StatutInvitation.EN_ATTENTE);
        i.setExpireLe(maintenant.plus(VALIDITE));
        i.setEnvoyeLe(maintenant);
        invitations.save(i);

        String salutation = i.getNom() == null ? "Bonjour," : "Bonjour " + i.getNom() + ",";
        String texte = """
                %s

                %s vous invite à créer votre carte professionnelle sur MadeQrCard.

                Pour accepter ou refuser : %s/invitation/%s

                Ce lien est valable %d jours. Tant que vous n'avez pas accepté, vous n'êtes pas rattaché à cette organisation.
                Si vous avez déjà un compte, il reste le vôtre : l'organisation n'a accès ni à votre carte personnelle, ni à votre carnet, ni à l'adresse de votre compte.

                Si vous ne connaissez pas cette organisation, ignorez ce message.
                """.formatted(salutation, nomOrganisation, props.frontendUrl(), jeton, VALIDITE.toDays());
        arrierePlan.execute(() ->
        {
            try
            {
                emails.envoyer(email, nomOrganisation + " vous invite sur MadeQrCard", texte);
            }
            catch (RuntimeException e)
            {
                log.warn("Invitation non envoyée : {}", e.getMessage());
            }
        });
    }

    static String empreinte(String jeton)
    {
        try
        {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static String propre(String s)
    {
        return s == null || s.isBlank() ? null : s.replaceAll("\\p{Cntrl}", " ").trim();
    }
}
