package made.qrcard.carnet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import made.qrcard.carte.dto.CarteDto.CarteResponse;

import java.time.Instant;
import java.util.UUID;

/** Objets echanges avec le client : carnet. */
public final class CarnetDto
{

    private CarnetDto()
    {
    }

    public record EnregistrerRequest(@NotBlank String jeton, @Size(max = 120) String note)
    {
    }

    public record NoteRequest(@Size(max = 120) String note)
    {
    }

    /** carte vaut null si elle n'est plus disponible (compte bloque ou lien regenere). */
    public record CarnetItem(UUID id, String note, Instant enregistreeLe, CarteResponse carte)
    {
    }
}
