package made.qrcard.carte.domaine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarteRepository extends JpaRepository<Carte, UUID>
{

    Optional<Carte> findByJeton(String jeton);

    Optional<Carte> findByUtilisateur_IdAndOrganisationIdIsNull(UUID utilisateurId);

    Optional<Carte> findByUtilisateur_IdAndOrganisationId(UUID utilisateurId, UUID organisationId);

    List<Carte> findByUtilisateur_IdAndOrganisationIdIsNotNullOrderByCreatedAtAsc(UUID utilisateurId);

    List<Carte> findByOrganisationId(UUID organisationId);

    boolean existsByJeton(String jeton);

    @Query(value = "SELECT nextval('carte_numero_seq')", nativeQuery = true)
    long prochainNumero();
}
