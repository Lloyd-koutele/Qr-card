package made.qrcard.compte.domaine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface InscriptionAttenteRepository extends JpaRepository<InscriptionAttente, String>
{

    Optional<InscriptionAttente> findByJeton(String jeton);

    @Transactional
    @Modifying
    @Query("delete from InscriptionAttente i where i.expireLe < :date")
    void supprimerExpireesAvant(Instant date);
}
