package made.qrcard.organisation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import made.qrcard.compte.dto.CompteDto.InscriptionRequest;
import made.qrcard.organisation.domaine.RoleOrganisation;
import made.qrcard.organisation.domaine.TypeOrganisation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Objets echanges avec le client : organisation. Aucun ne contient l'adresse du
 * compte, la carte personnelle ou une information de connexion d'un membre.
 */
public final class OrganisationDto
{

    private OrganisationDto()
    {
    }

    public record OrganisationRequest(
            @NotBlank(message = "Le nom de l'organisation est obligatoire") @Size(max = 150) String nom,
            @NotNull(message = "Choisissez le type d'organisation") TypeOrganisation type,
            @Size(max = 60, message = "Le type précisé fait au plus 60 caractères") String typePrecision,
            @Size(max = 255) String adresse,
            @NotNull @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide") String couleur)
    {
    }

    public record OrganisationResponse(UUID id, String nom, TypeOrganisation type, String typePrecision, String adresse, String couleur, String logo,
                                     String statut, String motifBlocage, RoleOrganisation monRole, String monEmail)
    {
    }

    public record MembreItem(UUID id, String email, String nom, String poste, RoleOrganisation role,
                             Instant depuis, String carteJeton)
    {
    }

    public record RoleRequest(@NotNull RoleOrganisation role)
    {
    }

    public record InvitationSaisie(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 255) String email,
            @Size(max = 120) String nom,
            @Size(max = 120) String poste)
    {
    }

    public record InvitationsRequest(
            @NotEmpty(message = "Indiquez au moins une adresse")
            @Size(max = 50, message = "50 adresses au maximum par envoi") List<@Valid InvitationSaisie> invitations)
    {
    }

    /** Adresse non traitee : le motif ne parle que de cette organisation, jamais d'un compte existant. */
    public record InvitationIgnoree(String email, String motif)
    {
    }

    public record InvitationsResultat(int envoyees, List<InvitationIgnoree> ignorees)
    {
    }

    /** statut : EN_ATTENTE ou TERMINEE (expiree ou refusee, volontairement sans distinction). */
    public record InvitationItem(UUID id, String email, String nom, String poste, String statut,
                                 Instant envoyeeLe, Instant expireLe)
    {
    }

    public record JournalItem(Instant date, String auteur, String action, String cible)
    {
    }

    /** Ce que voit la personne qui a recu le lien. */
    public record InvitationPublique(UUID organisationId, String organisation, String couleur, String logo, String email, String nom, String poste)
    {
    }

    public record CompteInvitationRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 120) String nomAffiche,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères") String motDePasse)
    {
    }

    /** Creation publique : le compte de l'administrateur et l'organisation, crees ensemble une fois l'email verifie. */
    public record CreationPubliqueRequest(
            @NotNull @Valid OrganisationRequest organisation,
            @NotNull @Valid InscriptionRequest compte)
    {
    }
}
