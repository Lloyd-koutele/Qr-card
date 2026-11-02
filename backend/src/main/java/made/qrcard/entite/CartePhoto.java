package made.qrcard.entite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "carte_photo")
@Getter
@Setter
public class CartePhoto {

    @Id
    @Column(name = "carte_id")
    private UUID carteId;

    @Column(nullable = false)
    private byte[] donnees;

    @Column(nullable = false)
    private String type;
}
