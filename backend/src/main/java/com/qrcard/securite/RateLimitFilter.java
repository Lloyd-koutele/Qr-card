package com.qrcard.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite simple par adresse IP et par minute : connexion (force brute) et
 * cartes publiques (aspiration). Suffisant pour une seule instance de l'API.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_AUTH = 30;
    private static final int LIMITE_PUBLIC = 120;

    private record Fenetre(long minute, AtomicInteger compteur) {
    }

    private final Map<String, Fenetre> fenetres = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String chemin = request.getRequestURI();
        int limite;
        String categorie;
        if (chemin.startsWith("/api/auth/") || chemin.startsWith("/login/oauth2/")) {
            limite = LIMITE_AUTH;
            categorie = "auth";
        } else if (chemin.startsWith("/api/public/")) {
            limite = LIMITE_PUBLIC;
            categorie = "public";
        } else {
            chain.doFilter(request, response);
            return;
        }

        long minute = System.currentTimeMillis() / 60_000;
        if (fenetres.size() > 50_000) fenetres.clear();
        Fenetre f = fenetres.compute(categorie + "|" + ip(request),
                (k, ancienne) -> ancienne == null || ancienne.minute() != minute ? new Fenetre(minute, new AtomicInteger()) : ancienne);

        if (f.compteur().incrementAndGet() > limite) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Trop de requêtes, réessayez dans une minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String ip(HttpServletRequest request) {
        String transmis = request.getHeader("X-Forwarded-For");
        if (transmis != null && !transmis.isBlank()) return transmis.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
