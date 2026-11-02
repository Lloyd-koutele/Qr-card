package com.qrcard.securite;

import com.qrcard.config.AppProperties;
import com.qrcard.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Retour de Google : on cree ou retrouve le compte, puis on renvoie vers le
 * front avec un jeton (ou un jeton 2FA) dans le fragment de l'URL, qui n'est
 * jamais envoye au serveur ni journalise.
 */
@Component
public class GoogleLoginHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AppProperties props;

    public GoogleLoginHandler(AuthService authService, AppProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var resultat = authentication.getPrincipal() instanceof OidcUser oidc
                ? authService.connexionGoogle(oidc.getSubject(), oidc.getEmail(),
                Boolean.TRUE.equals(oidc.getEmailVerified()), oidc.getFullName())
                : AuthService.GoogleResultat.erreur("google");

        // La session Spring n'a servi qu'a l'echange avec Google
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();

        String base = props.frontendUrl();
        if (resultat.erreur() != null) {
            response.sendRedirect(base + "/connexion?erreur=" + resultat.erreur());
        } else if (resultat.mfaToken() != null) {
            response.sendRedirect(base + "/auth/callback#mfa=" + resultat.mfaToken());
        } else {
            response.sendRedirect(base + "/auth/callback#token=" + resultat.token());
        }
    }
}
