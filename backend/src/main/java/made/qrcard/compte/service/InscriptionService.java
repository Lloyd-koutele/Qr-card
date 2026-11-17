package made.qrcard.compte.service;

import made.qrcard.commun.email.EnvoiEmail;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.domaine.InscriptionAttente;
import made.qrcard.compte.domaine.InscriptionAttenteRepository;
import made.qrcard.compte.domaine.Role;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.dto.CompteDto.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Inscription par email en deux temps : la demande envoie un code a 6 chiffres,
 * et le compte n'existe qu'une fois ce code saisi. Une adresse ne peut donc pas
 * etre reservee par quelqu'un qui ne la possede pas.
 */
@Service
public class InscriptionService
{

    private static final Duration VALIDITE_CODE = Duration.ofMinutes(15);
    private static final Duration DELAI_RENVOI = Duration.ofSeconds(60);
    private static final Duration CONSERVATION = Duration.ofHours(1);
    private static final int ESSAIS_MAX = 5;

    private final InscriptionAttenteRepository attentes;
    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final EnvoiEmail emails;
    private final AuthService auth;
    private final ApplicationEventPublisher evenements;
    private final SecureRandom random = new SecureRandom();

    public InscriptionService(InscriptionAttenteRepository attentes, UtilisateurRepository utilisateurs,
                              PasswordEncoder encoder, EnvoiEmail emails, AuthService auth,
                              ApplicationEventPublisher evenements)
    {
        this.attentes = attentes;
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.emails = emails;
        this.auth = auth;
        this.evenements = evenements;
    }

    public InscriptionAttenteResponse demander(InscriptionRequest req)
    {
        return demander(req, null);
    }

    /**
     * complement : donnees d'un autre module, rendues telles quelles dans {@link CompteCree}
     * une fois l'email verifie. Pas de transaction : l'envoi du mail ne doit pas garder une connexion a la base ouverte.
     */
    public InscriptionAttenteResponse demander(InscriptionRequest req, String complement)
    {
        String email = AuthService.normaliserEmail(req.email());
        if (utilisateurs.existsByEmail(email))
        {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        attentes.supprimerExpireesAvant(Instant.now().minus(CONSERVATION));
        InscriptionAttente precedente = attentes.findById(email).orElse(null);
        if (precedente != null) verifierRenvoiPossible(precedente);

        InscriptionAttente a = new InscriptionAttente();
        a.setEmail(email);
        a.setJeton(nouveauJeton());
        a.setNomAffiche(req.nomAffiche().trim());
        a.setMotDePasse(encoder.encode(req.motDePasse()));
        a.setComplement(complement);
        // Redemander un code ne donne pas plus d'essais tant que la demande precedente court
        if (precedente != null && precedente.getExpireLe().isAfter(Instant.now())) a.setEssais(precedente.getEssais());
        envoyerCode(a);
        attentes.save(a);
        return new InscriptionAttenteResponse(a.getJeton(), email, DELAI_RENVOI.toSeconds());
    }

    public InscriptionAttenteResponse renvoyer(RenvoiCodeRequest req)
    {
        InscriptionAttente a = charger(req.jeton());
        verifierRenvoiPossible(a);
        envoyerCode(a);
        attentes.save(a);
        return new InscriptionAttenteResponse(a.getJeton(), a.getEmail(), DELAI_RENVOI.toSeconds());
    }

    // Le compteur d'essais doit etre enregistre meme quand le code est refuse
    @Transactional(noRollbackFor = ApiException.class)
    public ConnexionResponse verifier(VerificationEmailRequest req)
    {
        InscriptionAttente a = charger(req.jeton());
        if (a.getExpireLe().isBefore(Instant.now()))
        {
            throw ApiException.invalide("Ce code a expiré. Demandez-en un nouveau.");
        }
        if (a.getEssais() >= ESSAIS_MAX) throw trop();
        if (!encoder.matches(req.code().replaceAll("\\s", ""), a.getCode()))
        {
            a.setEssais(a.getEssais() + 1);
            if (a.getEssais() >= ESSAIS_MAX) throw trop();
            throw ApiException.invalide("Code incorrect");
        }
        attentes.delete(a);
        // Le compte a pu etre cree entre-temps avec Google
        if (utilisateurs.existsByEmail(a.getEmail()))
        {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        Utilisateur u = new Utilisateur();
        u.setEmail(a.getEmail());
        u.setNomAffiche(a.getNomAffiche());
        u.setMotDePasse(a.getMotDePasse());
        u.setRole(Role.USER);
        utilisateurs.save(u);
        evenements.publishEvent(new CompteCree(u, a.getComplement()));
        return auth.connexionReussie(u);
    }

    /**
     * Compte dont l'adresse est deja prouvee (lien d'invitation recu dans cette boite) :
     * pas de code a 6 chiffres. Le lien va a la personne, elle peut donc apprendre qu'un compte existe.
     */
    @Transactional
    public Utilisateur creerCompteVerifie(String adresse, String nomAffiche, String motDePasse)
    {
        String email = AuthService.normaliserEmail(adresse);
        if (utilisateurs.existsByEmail(email))
        {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cette adresse : connectez-vous pour répondre à l'invitation.");
        }
        attentes.findById(email).ifPresent(attentes::delete);
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setNomAffiche(nomAffiche.trim());
        u.setMotDePasse(encoder.encode(motDePasse));
        u.setRole(Role.USER);
        utilisateurs.save(u);
        evenements.publishEvent(new CompteCree(u));
        return u;
    }

    private InscriptionAttente charger(String jeton)
    {
        return attentes.findByJeton(jeton)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "Cette demande a expiré. Recommencez l'inscription."));
    }

    private static ApiException trop()
    {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop d'essais. Réessayez dans 15 minutes.");
    }

    private void verifierRenvoiPossible(InscriptionAttente a)
    {
        boolean enCours = a.getExpireLe().isAfter(Instant.now());
        if (enCours && a.getEssais() >= ESSAIS_MAX) throw trop();
        if (a.getEnvoyeLe().plus(DELAI_RENVOI).isAfter(Instant.now()))
        {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Un code vient d'être envoyé. Patientez une minute.");
        }
    }

    private void envoyerCode(InscriptionAttente a)
    {
        String code = String.format("%06d", random.nextInt(1_000_000));
        emails.envoyer(a.getEmail(), "Votre code de vérification MadeQrCard", """
                Bonjour %s,

                Votre code de vérification MadeQrCard est : %s

                Il est valable au plus %d minutes. Si vous n'avez pas demandé à créer un compte, ignorez ce message.
                """.formatted(a.getNomAffiche(), code, VALIDITE_CODE.toMinutes()));
        Instant maintenant = Instant.now();
        a.setCode(encoder.encode(code));
        a.setEnvoyeLe(maintenant);
        // Le blocage apres trop d'essais doit pouvoir expirer : on ne prolonge pas une demande deja lancee
        if (a.getExpireLe() == null || a.getExpireLe().isBefore(maintenant))
        {
            a.setExpireLe(maintenant.plus(VALIDITE_CODE));
            a.setEssais(0);
        }
    }

    private String nouveauJeton()
    {
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }
}
