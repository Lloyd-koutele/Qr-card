package made.qrcard.compte.service;

import made.qrcard.commun.email.EnvoiEmail;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.domaine.ReinitialisationMotDePasse;
import made.qrcard.compte.domaine.ReinitialisationMotDePasseRepository;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.dto.CompteDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Mot de passe oublie. La reponse est la meme que le compte existe ou non, et
 * l'envoi se fait en arriere-plan pour que le temps de reponse ne le trahisse pas.
 * La double authentification reste demandee a la connexion suivante.
 */
@Service
public class ReinitialisationService
{

    private static final Logger log = LoggerFactory.getLogger(ReinitialisationService.class);

    private static final Duration VALIDITE = Duration.ofMinutes(15);
    private static final Duration DELAI_RENVOI = Duration.ofSeconds(60);
    private static final int ESSAIS_MAX = 5;
    private static final String CODE_REFUSE = "Code incorrect ou expiré";

    private final ReinitialisationMotDePasseRepository demandes;
    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final EnvoiEmail emails;
    private final TaskExecutor arrierePlan;
    private final SecureRandom random = new SecureRandom();

    public ReinitialisationService(ReinitialisationMotDePasseRepository demandes, UtilisateurRepository utilisateurs,
                                   PasswordEncoder encoder, EnvoiEmail emails, TaskExecutor arrierePlan)
    {
        this.demandes = demandes;
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.emails = emails;
        this.arrierePlan = arrierePlan;
    }

    public void demander(MotDePasseOublieRequest req)
    {
        String email = AuthService.normaliserEmail(req.email());
        arrierePlan.execute(() ->
        {
            try
            {
                envoyerCode(email);
            }
            catch (ApiException e)
            {
                log.warn("Code de réinitialisation non envoyé : {}", e.getMessage());
            }
        });
    }

    // Le compteur d'essais doit etre enregistre meme quand le code est refuse
    @Transactional(noRollbackFor = ApiException.class)
    public void reinitialiser(ReinitialisationRequest req)
    {
        Utilisateur u = utilisateurs.findByEmail(AuthService.normaliserEmail(req.email()))
                .filter(Utilisateur::estActif)
                .orElseThrow(() -> ApiException.invalide(CODE_REFUSE));
        ReinitialisationMotDePasse d = demandes.findById(u.getId())
                .filter(x -> x.getExpireLe().isAfter(Instant.now()) && x.getEssais() < ESSAIS_MAX)
                .orElseThrow(() -> ApiException.invalide(CODE_REFUSE));
        if (!encoder.matches(req.code().replaceAll("\\s", ""), d.getCode()))
        {
            d.setEssais(d.getEssais() + 1);
            throw ApiException.invalide(CODE_REFUSE);
        }
        demandes.delete(d);
        u.setMotDePasse(encoder.encode(req.nouveau()));
        u.setChangerMotDePasse(false);
        // Deconnecte partout, au cas ou le compte aurait ete pris
        u.setSessionsValidesDepuis(Instant.now());
    }

    private void envoyerCode(String email)
    {
        Utilisateur u = utilisateurs.findByEmail(email).filter(Utilisateur::estActif).orElse(null);
        if (u == null) return;
        Instant maintenant = Instant.now();
        ReinitialisationMotDePasse d = demandes.findById(u.getId())
                .filter(x -> x.getExpireLe().isAfter(maintenant))
                .orElse(null);
        if (d == null)
        {
            d = new ReinitialisationMotDePasse();
            d.setUtilisateurId(u.getId());
            d.setExpireLe(maintenant.plus(VALIDITE));
        }
        else if (d.getEssais() >= ESSAIS_MAX || d.getEnvoyeLe().plus(DELAI_RENVOI).isAfter(maintenant))
        {
            // Un renvoi garde le compteur d'essais : redemander un code ne donne pas plus de tentatives
            return;
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        emails.envoyer(email, "Réinitialisation de votre mot de passe MadeQrCard", """
                Bonjour %s,

                Votre code pour choisir un nouveau mot de passe MadeQrCard est : %s

                Il est valable au plus %d minutes. Si vous n'avez rien demandé, ignorez ce message : votre mot de passe ne change pas.
                """.formatted(u.getNomAffiche(), code, VALIDITE.toMinutes()));
        d.setCode(encoder.encode(code));
        d.setEnvoyeLe(maintenant);
        demandes.save(d);
    }
}
