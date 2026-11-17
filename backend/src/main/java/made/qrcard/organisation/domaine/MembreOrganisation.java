package made.qrcard.organisation.domaine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Lien entre un compte et une organisation. Le compte, lui, reste a la personne. */
@Entity
@Table(name = "membre_organisation")
@Getter
@Setter
public class MembreOrganisation
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "utilisateur_id", nullable = false, unique = true)
    private UUID utilisateurId;

    /** L'adresse invitee, la seule que voit l'organisation : jamais celle du compte. */
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleOrganisation role = RoleOrganisation.MEMBRE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean estAdmin()
    {
        return role == RoleOrganisation.ADMIN;
    }
}
