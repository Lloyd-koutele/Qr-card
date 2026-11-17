package made.qrcard.organisation.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembreOrganisationRepository extends JpaRepository<MembreOrganisation, UUID>
{

    List<MembreOrganisation> findByUtilisateurIdOrderByCreatedAtAsc(UUID utilisateurId);

    Optional<MembreOrganisation> findByOrganisationIdAndUtilisateurId(UUID organisationId, UUID utilisateurId);

    long countByUtilisateurId(UUID utilisateurId);

    boolean existsByOrganisationIdAndUtilisateurId(UUID organisationId, UUID utilisateurId);

    boolean existsByOrganisationIdAndEmail(UUID organisationId, String email);

    List<MembreOrganisation> findByOrganisationIdOrderByCreatedAtAsc(UUID organisationId);

    long countByOrganisationId(UUID organisationId);

    long countByOrganisationIdAndRole(UUID organisationId, RoleOrganisation role);
}
