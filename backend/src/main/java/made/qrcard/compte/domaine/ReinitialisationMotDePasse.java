package made.qrcard.compte.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Demande de nouveau mot de passe en cours. Le code est hashe. */
@Entity
@Table(name = "reinitialisation_mot_de_passe")
@Getter
@Setter
public class ReinitialisationMotDePasse
{

    @Id
    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int essais;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "envoye_le", nullable = false)
    private Instant envoyeLe;
}
