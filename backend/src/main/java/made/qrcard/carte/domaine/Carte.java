package made.qrcard.carte.domaine;

import made.qrcard.compte.domaine.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carte")
@Getter
@Setter
public class Carte 
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    /** Vide pour la carte personnelle, renseigne pour une carte professionnelle. */
    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(nullable = false, unique = true)
    private String jeton;

    @Column(nullable = false, unique = true)
    private String numero;

    private String nom;
    private String prenom;
    private String profession;
    private String bio;

    /** Carte personnelle seulement, #rrggbb ; vide = rouge MadeQrCard. */
    private String couleur;

    /** Horodatage de la derniere photo, vide s'il n'y en a pas. */
    @Column(name = "photo_version")
    private Long photoVersion;

    @OneToMany(mappedBy = "carte", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<Contact> contacts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
