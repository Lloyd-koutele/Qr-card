package made.qrcard.securite;

import made.qrcard.config.AppProperties;
import made.qrcard.entite.Utilisateur;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Deux sortes de jetons : "acces" (session normale) et "mfa" (5 minutes, ne permet
 * que de saisir le code 2FA). Le filtre n'accepte que les jetons d'acces.
 */
@Service
public class JwtService {

    public static final String TYPE_ACCES = "acces";
    public static final String TYPE_MFA = "mfa";

    private final SecretKey cle;
    private final Duration validite;

    public JwtService(AppProperties props) {
        String secret = props.jwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 caractères");
        }
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validite = Duration.ofMinutes(props.jwtValiditeMinutes());
    }

    public String genererAcces(Utilisateur u) {
        return generer(u.getId(), TYPE_ACCES, validite);
    }

    public String genererMfa(UUID id) {
        return generer(id, TYPE_MFA, Duration.ofMinutes(5));
    }

    public Optional<UUID> lire(String token, String typeAttendu) {
        try {
            var claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(token).getPayload();
            if (!typeAttendu.equals(claims.get("typ", String.class))) return Optional.empty();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String generer(UUID id, String type, Duration duree) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(id.toString())
                .claim("typ", type)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(duree)))
                .signWith(cle)
                .compact();
    }
}
