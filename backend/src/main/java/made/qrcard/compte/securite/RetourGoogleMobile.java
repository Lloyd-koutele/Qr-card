package made.qrcard.compte.securite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.dto.CompteDto.ConnexionResponse;
import made.qrcard.compte.service.AuthService.GoogleResultat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connexion Google depuis l'application mobile. Une autre application pourrait
 * declarer le meme schema madeqrcard:// : le lien de retour ne porte donc qu'un
 * code a usage unique, echangeable seulement avec le verificateur que l'application
 * a garde pour elle (meme principe que PKCE).
 */
@Component
public class RetourGoogleMobile
{

    public static final String SCHEMA = "madeqrcard://auth";
    static final String ATTRIBUT_DEFI = "madeqrcard_defi_mobile";
    private static final Duration VALIDITE = Duration.ofMinutes(2);

    private record Attente(GoogleResultat resultat, String defi, Instant expire)
    {
    }

    private final Map<String, Attente> attentes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Defi transmis par l'application au depart vers Google, vide pour le site web. */
    public static String defiDe(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(ATTRIBUT_DEFI);
    }

    public String emettre(GoogleResultat resultat, String defi)
    {
        Instant maintenant = Instant.now();
        attentes.values().removeIf(a -> a.expire().isBefore(maintenant));
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        attentes.put(code, new Attente(resultat, defi, maintenant.plus(VALIDITE)));
        return code;
    }

    /** Le code est consomme meme si le verificateur est faux : pas de second essai. */
    public ConnexionResponse echanger(String code, String verificateur)
    {
        Attente a = attentes.remove(code);
        if (a == null || a.expire().isBefore(Instant.now())
                || !MessageDigest.isEqual(empreinte(verificateur).getBytes(StandardCharsets.US_ASCII),
                a.defi().getBytes(StandardCharsets.US_ASCII)))
        {
            throw ApiException.invalide("La connexion Google a expiré. Recommencez.");
        }
        GoogleResultat r = a.resultat();
        return r.mfaToken() != null ? new ConnexionResponse(null, r.mfaToken(), true) : new ConnexionResponse(r.token(), null, false);
    }

    static String empreinte(String verificateur)
    {
        try
        {
            byte[] hache = MessageDigest.getInstance("SHA-256").digest(verificateur.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hache);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
