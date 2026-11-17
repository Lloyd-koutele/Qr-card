package made.qrcard.compte.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Objets echanges avec le client : authentification et profil. */
public final class CompteDto
{

    private CompteDto()
    {
    }

    public record InscriptionRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 255) String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String motDePasse,
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 120) String nomAffiche)
    {
    }

    /** L'inscription attend la saisie du code recu par email. */
    public record InscriptionAttenteResponse(String jeton, String email, long renvoiDansSecondes)
    {
    }

    public record VerificationEmailRequest(@NotBlank String jeton, @NotBlank(message = "Saisissez le code") String code)
    {
    }

    public record RenvoiCodeRequest(@NotBlank String jeton)
    {
    }

    public record MotDePasseOublieRequest(@NotBlank(message = "Saisissez votre email") @Email(message = "Email invalide") String email)
    {
    }

    public record ReinitialisationRequest(
            @NotBlank String email,
            @NotBlank(message = "Saisissez le code") String code,
            @NotBlank @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String nouveau)
    {
    }

    public record ConnexionRequest(@NotBlank String email, @NotBlank String motDePasse)
    {
    }

    public record MfaRequest(@NotBlank String mfaToken, @NotBlank(message = "Saisissez le code") String code)
    {
    }

    /** Soit token (connecte), soit mfaToken (code 2FA attendu). */
    public record ConnexionResponse(String token, String mfaToken, boolean mfaRequis)
    {
    }

    public record ConfigResponse(boolean googleActif)
    {
    }

    public record ProfilResponse(UUID id, String email, String nomAffiche, String role,
                                 boolean totpActif, boolean aMotDePasse, boolean googleLie,
                                 boolean changerMotDePasse, boolean securiteRequise)
    {
    }

    public record MotDePasseRequest(String ancien,
                                    @NotBlank @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String nouveau)
    {
    }

    public record TotpInitResponse(String secret, String uri)
    {
    }

    public record CodeRequest(@NotBlank(message = "Saisissez le code") String code)
    {
    }

    public record CodesSecoursResponse(List<String> codes)
    {
    }

    public record GoogleEchangeRequest(@NotBlank @Size(max = 100) String code,
                                       @NotBlank @Size(min = 43, max = 128) String verificateur)
    {
    }
}
