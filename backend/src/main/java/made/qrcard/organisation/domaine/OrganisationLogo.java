package made.qrcard.organisation.domaine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organisation_logo")
@Getter
@Setter
public class OrganisationLogo
{

    @Id
    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(nullable = false)
    private byte[] donnees;

    @Column(nullable = false)
    private String type;
}
