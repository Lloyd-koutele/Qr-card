package made.qrcard.securite;

import made.qrcard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecuriteUtils {

    private SecuriteUtils() {
    }

    /** Identifiant de l'utilisateur connecte, s'il y en a un. */
    public static Optional<UUID> idOptionnel() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID id) return Optional.of(id);
        return Optional.empty();
    }

    public static UUID idCourant() {
        return idOptionnel().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"));
    }
}
