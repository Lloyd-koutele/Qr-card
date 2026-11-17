package made.qrcard.organisation.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganisationLogoRepository extends JpaRepository<OrganisationLogo, UUID>
{
}
