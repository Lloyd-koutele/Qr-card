package made.qrcard.repository;

import made.qrcard.entite.AuditAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditAdminRepository extends JpaRepository<AuditAdmin, UUID> {

    Page<AuditAdmin> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
