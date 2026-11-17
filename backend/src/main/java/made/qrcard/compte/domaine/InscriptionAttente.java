package made.qrcard.compte.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Demande d'inscription dont l'email n'est pas encore verifie. Le mot de passe et le code sont hashes. */
@Entity
@Table(name = "inscription_attente")
@Getter
@Setter
public class InscriptionAttente
{

    @Id
    private String email;

    /** Lie la verification au navigateur qui a fait la demande. */
    @Column(nullable = false, unique = true)
    private String jeton;

    @Column(name = "nom_affiche", nullable = false)
    private String nomAffiche;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int essais;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "envoye_le", nullable = false)
    private Instant envoyeLe;

    /** Donnees d'un autre module a remettre a la creation du compte (JSON), par exemple une organisation. */
    private String complement;
}
