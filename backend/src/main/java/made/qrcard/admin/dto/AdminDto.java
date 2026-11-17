package made.qrcard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Objets echanges avec le client : administration. */
public final class AdminDto
{

    private AdminDto()
    {
    }

    public record AdminUtilisateur(UUID id, String email, String role, String statut, String motifBlocage,
                                   String methode, boolean totpActif, Instant creeLe, Instant derniereConnexion)
    {
    }

    /** Metadonnees seulement : ni les membres ni leurs cartes. */
    public record AdminOrganisation(UUID id, String nom, String type, String typePrecision, String statut, String motifBlocage, long membres, Instant creeLe)
    {
    }

    public record BlocageRequest(@NotBlank(message = "Le motif est obligatoire") @Size(max = 500) String motif)
    {
    }

    public record StatsResponse(long total, long actifs, long bloques, long cartes)
    {
    }

    public record AuditItem(Instant date, String adminEmail, String action, String cibleEmail, String motif)
    {
    }
}
