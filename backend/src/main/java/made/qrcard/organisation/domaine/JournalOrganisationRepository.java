package made.qrcard.organisation.domaine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalOrganisationRepository extends JpaRepository<JournalOrganisation, UUID>
{

    Page<JournalOrganisation> findByOrganisationIdOrderByCreatedAtDesc(UUID organisationId, Pageable page);
}
