package com.qrcard.securite;

import com.qrcard.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le compte est relu a chaque requete : un blocage par l'admin prend effet
 * immediatement, sans attendre l'expiration du jeton.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurs;

    public JwtAuthFilter(JwtService jwtService, UtilisateurRepository utilisateurs) {
        this.jwtService = jwtService;
        this.utilisateurs = utilisateurs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete != null && entete.startsWith("Bearer ")) {
            jwtService.lire(entete.substring(7), JwtService.TYPE_ACCES)
                    .flatMap(utilisateurs::findById)
                    .filter(u -> u.estActif())
                    .ifPresent(u -> {
                        var auth = new UsernamePasswordAuthenticationToken(u.getId(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
