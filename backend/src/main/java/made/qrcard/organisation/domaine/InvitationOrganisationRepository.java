package made.qrcard.organisation.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationOrganisationRepository extends JpaRepository<InvitationOrganisation, UUID>
{

    Optional<InvitationOrganisation> findByJetonEmpreinte(String jetonEmpreinte);

    Optional<InvitationOrganisation> findByOrganisationIdAndEmail(UUID organisationId, String email);

    List<InvitationOrganisation> findByOrganisationIdAndStatutNotOrderByEnvoyeLeDesc(UUID organisationId, StatutInvitation statut);

    long countByOrganisationIdAndEnvoyeLeAfter(UUID organisationId, Instant depuis);
}
