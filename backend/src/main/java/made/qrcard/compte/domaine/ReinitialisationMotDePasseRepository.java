package made.qrcard.compte.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReinitialisationMotDePasseRepository extends JpaRepository<ReinitialisationMotDePasse, UUID>
{
}
