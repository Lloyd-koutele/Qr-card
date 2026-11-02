package made.qrcard.dto;

import made.qrcard.entite.ModeAffichage;
import made.qrcard.entite.Theme;
import made.qrcard.entite.TypeContact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Objets echanges avec le client. */
public final class Dto {

    private Dto() {
    }

    // --- Authentification ---

    public record InscriptionRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 255) String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String motDePasse,
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 120) String nomAffiche) {
    }

    public record ConnexionRequest(@NotBlank String email, @NotBlank String motDePasse) {
    }

    public record MfaRequest(@NotBlank String mfaToken, @NotBlank(message = "Saisissez le code") String code) {
    }

    /** Soit token (connecte), soit mfaToken (code 2FA attendu). */
    public record ConnexionResponse(String token, String mfaToken, boolean mfaRequis) {
    }

    public record ConfigResponse(boolean googleActif) {
    }

    // --- Profil ---

    public record ProfilResponse(UUID id, String email, String nomAffiche, String role, ModeAffichage modeAffichage,
                                 boolean totpActif, boolean aMotDePasse, boolean googleLie,
                                 boolean changerMotDePasse, boolean securiteRequise) {
    }

    public record PreferencesRequest(@NotNull ModeAffichage modeAffichage) {
    }

    public record MotDePasseRequest(String ancien,
                                    @NotBlank @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String nouveau) {
    }

    public record TotpInitResponse(String secret, String uri) {
    }

    public record CodeRequest(@NotBlank(message = "Saisissez le code") String code) {
    }

    public record CodesSecoursResponse(List<String> codes) {
    }

    // --- Cartes ---

    public record ContactInput(@NotNull TypeContact type, @NotBlank @Size(max = 255) String valeur) {
    }

    public record CarteRequest(
            @Size(max = 80) String nom,
            @Size(max = 80) String prenom,
            @Size(max = 120) String profession,
            @Size(max = 300) String bio,
            @NotNull Theme theme,
            @Size(max = 20, message = "20 contacts au maximum") List<@Valid ContactInput> contacts) {
    }

    public record ContactDto(TypeContact type, String libelle, String valeur, String lien) {
    }

    public record CarteResponse(String jeton, String numero, String nom, String prenom, String profession, String bio,
                                Theme theme, Long photoVersion, List<ContactDto> contacts, Instant creeLe,
                                boolean proprietaire, boolean enregistree, boolean connecte, Long enregistrements) {
    }

    public record EnregistrerRequest(@NotBlank String jeton, @Size(max = 120) String note) {
    }

    public record NoteRequest(@Size(max = 120) String note) {
    }

    /** carte vaut null si elle n'est plus disponible (compte bloque ou lien regenere). */
    public record CarnetItem(UUID id, String note, Instant enregistreeLe, CarteResponse carte) {
    }

    // --- Administration ---

    public record AdminUtilisateur(UUID id, String email, String role, String statut, String motifBlocage,
                                   String methode, boolean totpActif, Instant creeLe, Instant derniereConnexion) {
    }

    public record BlocageRequest(@NotBlank(message = "Le motif est obligatoire") @Size(max = 500) String motif) {
    }

    public record StatsResponse(long total, long actifs, long bloques, long cartes) {
    }

    public record AuditItem(Instant date, String adminEmail, String action, String cibleEmail, String motif) {
    }

    public record PageResponse<T>(List<T> contenu, int page, int totalPages, long total) {
    }
}
