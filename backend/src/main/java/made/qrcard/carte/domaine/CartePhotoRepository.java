package made.qrcard.carte.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartePhotoRepository extends JpaRepository<CartePhoto, UUID>
{
}
