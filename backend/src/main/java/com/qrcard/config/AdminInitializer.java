package com.qrcard.config;

import com.qrcard.entite.Role;
import com.qrcard.entite.Utilisateur;
import com.qrcard.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Le premier compte du systeme est l'administrateur general. Il est cree au
 * demarrage a partir de ADMIN_EMAIL et ADMIN_PASSWORD, uniquement si la base est
 * vide. A sa premiere connexion il doit changer son mot de passe et activer la 2FA.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminInitializer(UtilisateurRepository utilisateurs, PasswordEncoder encoder, AppProperties props) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurs.count() > 0) return;
        if (props.adminEmail() == null || props.adminEmail().isBlank()
                || props.adminMotDePasse() == null || props.adminMotDePasse().length() < 8) {
            log.warn("Aucun compte en base : renseignez ADMIN_EMAIL et ADMIN_PASSWORD (8 caractères minimum) pour créer l'administrateur");
            return;
        }
        Utilisateur admin = new Utilisateur();
        admin.setEmail(props.adminEmail().trim().toLowerCase(Locale.ROOT));
        admin.setNomAffiche("Administrateur");
        admin.setMotDePasse(encoder.encode(props.adminMotDePasse()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setChangerMotDePasse(true);
        utilisateurs.save(admin);
        log.info("Administrateur général créé : {}", admin.getEmail());
    }
}
