package made.qrcard.organisation.domaine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisation")
@Getter
@Setter
public class Organisation
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    private String adresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeOrganisation type = TypeOrganisation.ENTREPRISE;

    /** Saisie libre, seulement pour le type AUTRE (par exemple « Club de sport »). */
    @Column(name = "type_precision")
    private String typePrecision;

    @Column(nullable = false)
    private String couleur;

    /** Horodatage du dernier logo, vide s'il n'y en a pas. */
    @Column(name = "logo_version")
    private Long logoVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutOrganisation statut = StatutOrganisation.ACTIVE;

    @Column(name = "motif_blocage")
    private String motifBlocage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean estActive()
    {
        return statut == StatutOrganisation.ACTIVE;
    }

    /** Adresse du logo, utilisable sans connexion puisque la carte est publique. */
    public String urlLogo()
    {
        return logoVersion == null ? null : "/api/public/organisations/" + id + "/logo?v=" + logoVersion;
    }
}
