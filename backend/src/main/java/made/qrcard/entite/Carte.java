package made.qrcard.entite;

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
public class Carte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false, unique = true)
    private String jeton;

    @Column(nullable = false, unique = true)
    private String numero;

    private String nom;
    private String prenom;
    private String profession;
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme = Theme.ROUGE;

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
