package com.qrcard.config;

import com.qrcard.repository.UtilisateurRepository;
import com.qrcard.securite.GoogleLoginHandler;
import com.qrcard.securite.JwtAuthFilter;
import com.qrcard.securite.JwtService;
import com.qrcard.securite.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UtilisateurRepository utilisateurs,
                                           ObjectProvider<ClientRegistrationRepository> clientsGoogle,
                                           GoogleLoginHandler googleHandler,
                                           AppProperties props) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // La session ne sert qu'a l'aller-retour avec Google ; l'API est authentifiee par JWT
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**", "/api/public/**", "/actuator/health",
                                "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> json(res, 401, "Connexion requise"))
                        .accessDeniedHandler((req, res, ex) -> json(res, 403, "Accès refusé")))
                .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtService, utilisateurs), UsernamePasswordAuthenticationFilter.class);

        if (clientsGoogle.getIfAvailable() != null) {
            http.oauth2Login(o -> o
                    .successHandler(googleHandler)
                    .failureHandler((req, res, ex) -> res.sendRedirect(props.frontendUrl() + "/connexion?erreur=google")));
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void json(HttpServletResponse res, int statut, String message) throws IOException {
        res.setStatus(statut);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
