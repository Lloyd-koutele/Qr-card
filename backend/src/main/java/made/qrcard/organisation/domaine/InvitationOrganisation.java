package made.qrcard.organisation.domaine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitation_organisation")
@Getter
@Setter
public class InvitationOrganisation
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String email;

    private String nom;
    private String poste;

    /** SHA-256 du jeton envoye par email : le jeton lui-meme n'est jamais conserve. */
    @Column(name = "jeton_empreinte", nullable = false, unique = true)
    private String jetonEmpreinte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInvitation statut = StatutInvitation.EN_ATTENTE;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "envoye_le", nullable = false)
    private Instant envoyeLe;

    public boolean estValable(Instant maintenant)
    {
        return statut == StatutInvitation.EN_ATTENTE && expireLe.isAfter(maintenant);
    }
}
