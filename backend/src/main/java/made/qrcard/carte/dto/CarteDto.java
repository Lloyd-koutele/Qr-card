package made.qrcard.carte.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import made.qrcard.carte.domaine.TypeContact;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Objets echanges avec le client : cartes. */
public final class CarteDto
{

    private CarteDto()
    {
    }

    public record ContactInput(@NotNull TypeContact type, @NotBlank @Size(max = 255) String valeur)
    {
    }

    public record CarteRequest(
            @Size(max = 80) String nom,
            @Size(max = 80) String prenom,
            @Size(max = 120) String profession,
            @Size(max = 300) String bio,
            @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "Couleur invalide") String couleur,
            @Size(max = 20, message = "20 contacts au maximum") List<@Valid ContactInput> contacts)
    {
    }

    public record ContactDto(TypeContact type, String libelle, String valeur, String lien)
    {
    }

    /** Ce que l'organisation impose sur une carte professionnelle : le titulaire ne peut pas le changer. */
    public record MarqueDto(UUID id, String nom, String couleur, String adresse, String logo)
    {
    }

    public record CarteResponse(String jeton, String numero, String nom, String prenom, String profession, String bio,
                                String couleur, Long photoVersion, List<ContactDto> contacts, Instant creeLe,
                                boolean proprietaire, boolean enregistree, boolean connecte, Long enregistrements,
                                MarqueDto organisation)
    {
    }
}
