package made.qrcard.service;

import made.qrcard.dto.Dto.*;
import made.qrcard.entite.Utilisateur;
import made.qrcard.repository.UtilisateurRepository;
import made.qrcard.securite.CryptoService;
import made.qrcard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfilService {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final TotpService totp;
    private final CryptoService crypto;
    private final AuthService auth;

    public ProfilService(UtilisateurRepository utilisateurs, PasswordEncoder encoder, TotpService totp,
                         CryptoService crypto, AuthService auth) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.totp = totp;
        this.crypto = crypto;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public ProfilResponse profil(UUID id) {
        return versDto(charger(id));
    }

    @Transactional
    public ProfilResponse preferences(UUID id, PreferencesRequest req) {
        Utilisateur u = charger(id);
        u.setModeAffichage(req.modeAffichage());
        return versDto(u);
    }

    @Transactional
    public ProfilResponse changerMotDePasse(UUID id, MotDePasseRequest req) {
        Utilisateur u = charger(id);
        // Un compte cree avec Google peut definir un mot de passe sans en avoir d'ancien
        if (u.getMotDePasse() != null
                && (req.ancien() == null || !encoder.matches(req.ancien(), u.getMotDePasse()))) {
            throw ApiException.invalide("Mot de passe actuel incorrect");
        }
        if (u.getMotDePasse() != null && encoder.matches(req.nouveau(), u.getMotDePasse())) {
            throw ApiException.invalide("Le nouveau mot de passe doit être différent de l'ancien");
        }
        u.setMotDePasse(encoder.encode(req.nouveau()));
        u.setChangerMotDePasse(false);
        return versDto(u);
    }

    /** Etape 1 : un nouveau secret, pas encore actif tant qu'un code n'a pas ete confirme. */
    @Transactional
    public TotpInitResponse initialiser2fa(UUID id) {
        Utilisateur u = charger(id);
        if (u.isTotpActif()) throw new ApiException(HttpStatus.CONFLICT, "La double authentification est déjà active");
        String secret = totp.nouveauSecret();
        u.setTotpSecret(crypto.chiffrer(secret));
        return new TotpInitResponse(secret, totp.uri(u.getEmail(), secret));
    }

    @Transactional
    public CodesSecoursResponse activer2fa(UUID id, CodeRequest req) {
        Utilisateur u = charger(id);
        if (u.isTotpActif()) throw new ApiException(HttpStatus.CONFLICT, "La double authentification est déjà active");
        if (u.getTotpSecret() == null) throw ApiException.invalide("Commencez par scanner le QR code");
        if (!totp.verifier(crypto.dechiffrer(u.getTotpSecret()), req.code().replaceAll("\\s", ""))) {
            throw ApiException.invalide("Code incorrect. Vérifiez l'heure de votre téléphone et réessayez.");
        }
        u.setTotpActif(true);
        return new CodesSecoursResponse(auth.genererCodesSecours(u));
    }

    @Transactional
    public ProfilResponse desactiver2fa(UUID id, CodeRequest req) {
        Utilisateur u = charger(id);
        if (u.estAdmin()) throw ApiException.interdit("La double authentification est obligatoire pour les administrateurs");
        if (!u.isTotpActif()) return versDto(u);
        if (!totp.verifier(crypto.dechiffrer(u.getTotpSecret()), req.code().replaceAll("\\s", ""))) {
            throw ApiException.invalide("Code incorrect");
        }
        u.setTotpActif(false);
        u.setTotpSecret(null);
        u.setCodesSecours(null);
        return versDto(u);
    }

    private Utilisateur charger(UUID id) {
        return utilisateurs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Connexion requise"));
    }

    static ProfilResponse versDto(Utilisateur u) {
        boolean securiteRequise = u.estAdmin() && (!u.isTotpActif() || u.isChangerMotDePasse());
        return new ProfilResponse(u.getId(), u.getEmail(), u.getNomAffiche(), u.getRole().name(), u.getModeAffichage(),
                u.isTotpActif(), u.getMotDePasse() != null, u.getGoogleId() != null,
                u.isChangerMotDePasse(), securiteRequise);
    }
}
