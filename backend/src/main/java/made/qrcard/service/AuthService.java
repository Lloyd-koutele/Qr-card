package made.qrcard.service;

import made.qrcard.dto.Dto.*;
import made.qrcard.entite.Role;
import made.qrcard.entite.Utilisateur;
import made.qrcard.repository.UtilisateurRepository;
import made.qrcard.securite.CryptoService;
import made.qrcard.securite.JwtService;
import made.qrcard.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final int ESSAIS_MAX = 5;
    private static final Duration VERROUILLAGE = Duration.ofMinutes(15);

    /** Resultat du retour Google : un seul des trois champs est renseigne. */
    public record GoogleResultat(String token, String mfaToken, String erreur) {
        public static GoogleResultat erreur(String code) {
            return new GoogleResultat(null, null, code);
        }
    }

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TotpService totp;
    private final CryptoService crypto;
    private final CarteService cartes;

    public AuthService(UtilisateurRepository utilisateurs, PasswordEncoder encoder, JwtService jwt,
                       TotpService totp, CryptoService crypto, CarteService cartes) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.jwt = jwt;
        this.totp = totp;
        this.crypto = crypto;
        this.cartes = cartes;
    }

    @Transactional
    public ConnexionResponse inscrire(InscriptionRequest req) {
        String email = normaliserEmail(req.email());
        if (utilisateurs.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setNomAffiche(req.nomAffiche().trim());
        u.setMotDePasse(encoder.encode(req.motDePasse()));
        u.setRole(Role.USER);
        utilisateurs.save(u);
        cartes.creerPour(u);
        return connexionReussie(u);
    }

    @Transactional
    public ConnexionResponse connecter(ConnexionRequest req) {
        Utilisateur u = utilisateurs.findByEmail(normaliserEmail(req.email()))
                .filter(x -> x.getMotDePasse() != null && encoder.matches(req.motDePasse(), x.getMotDePasse()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        verifierNonBloque(u);
        return etapeSuivante(u);
    }

    // Le compteur d'echecs doit etre enregistre meme quand le code est refuse
    @Transactional(noRollbackFor = ApiException.class)
    public ConnexionResponse validerCode(MfaRequest req) {
        Utilisateur u = jwt.lire(req.mfaToken(), JwtService.TYPE_MFA)
                .flatMap(utilisateurs::findById)
                .filter(Utilisateur::isTotpActif)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expirée, reconnectez-vous"));
        verifierNonBloque(u);
        if (u.getVerrouilleJusqua() != null && u.getVerrouilleJusqua().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop d'essais. Réessayez dans 15 minutes.");
        }
        String code = req.code().replaceAll("\\s", "");
        boolean ok = totp.verifier(crypto.dechiffrer(u.getTotpSecret()), code) || utiliserCodeSecours(u, code);
        if (!ok) {
            u.setEchecs2fa(u.getEchecs2fa() + 1);
            if (u.getEchecs2fa() >= ESSAIS_MAX) {
                u.setEchecs2fa(0);
                u.setVerrouilleJusqua(Instant.now().plus(VERROUILLAGE));
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Code incorrect");
        }
        u.setEchecs2fa(0);
        u.setVerrouilleJusqua(null);
        return connexionReussie(u);
    }

    @Transactional
    public GoogleResultat connexionGoogle(String googleId, String email, boolean emailVerifie, String nom) {
        if (googleId == null || email == null) return GoogleResultat.erreur("google");
        if (!emailVerifie) return GoogleResultat.erreur("email-non-verifie");

        Utilisateur u = utilisateurs.findByGoogleId(googleId).orElse(null);
        if (u == null) {
            String adresse = normaliserEmail(email);
            if (utilisateurs.existsByEmail(adresse)) {
                // Pas de fusion automatique : evite qu'on prenne le controle d'un compte existant
                return GoogleResultat.erreur("compte-existant");
            }
            u = new Utilisateur();
            u.setEmail(adresse);
            u.setNomAffiche(nom == null || nom.isBlank() ? adresse : nom.trim());
            u.setGoogleId(googleId);
            u.setRole(Role.USER);
            utilisateurs.save(u);
            cartes.creerPour(u);
        }
        if (!u.estActif()) return GoogleResultat.erreur("bloque");

        ConnexionResponse r = etapeSuivante(u);
        return new GoogleResultat(r.token(), r.mfaToken(), null);
    }

    /** Genere 10 codes de secours ; seules leurs empreintes sont gardees. */
    public List<String> genererCodesSecours(Utilisateur u) {
        List<String> codes = new ArrayList<>();
        List<String> empreintes = new ArrayList<>();
        var random = new java.security.SecureRandom();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 10; j++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            codes.add(sb.substring(0, 5) + "-" + sb.substring(5));
            empreintes.add(encoder.encode(sb.toString()));
        }
        u.setCodesSecours(String.join(";", empreintes));
        return codes;
    }

    private boolean utiliserCodeSecours(Utilisateur u, String code) {
        if (u.getCodesSecours() == null || u.getCodesSecours().isBlank()) return false;
        String saisi = code.replace("-", "").toUpperCase(Locale.ROOT);
        if (saisi.length() != 10) return false;
        List<String> empreintes = new ArrayList<>(Arrays.asList(u.getCodesSecours().split(";")));
        for (String e : empreintes) {
            if (encoder.matches(saisi, e)) {
                empreintes.remove(e);
                u.setCodesSecours(String.join(";", empreintes));
                return true;
            }
        }
        return false;
    }

    private ConnexionResponse etapeSuivante(Utilisateur u) {
        if (u.isTotpActif()) return new ConnexionResponse(null, jwt.genererMfa(u.getId()), true);
        return connexionReussie(u);
    }

    private ConnexionResponse connexionReussie(Utilisateur u) {
        u.setDerniereConnexion(Instant.now());
        return new ConnexionResponse(jwt.genererAcces(u), null, false);
    }

    private static void verifierNonBloque(Utilisateur u) {
        if (!u.estActif()) {
            throw ApiException.interdit("Ce compte est bloqué. Contactez l'administrateur.");
        }
    }

    static String normaliserEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
