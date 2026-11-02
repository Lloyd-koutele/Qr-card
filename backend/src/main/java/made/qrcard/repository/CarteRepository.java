package made.qrcard.repository;

import made.qrcard.entite.Carte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CarteRepository extends JpaRepository<Carte, UUID> {

    Optional<Carte> findByJeton(String jeton);

    Optional<Carte> findByUtilisateur_Id(UUID utilisateurId);

    boolean existsByJeton(String jeton);

    @Query(value = "SELECT nextval('carte_numero_seq')", nativeQuery = true)
    long prochainNumero();
}
