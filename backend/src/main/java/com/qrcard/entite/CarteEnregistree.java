package com.qrcard.entite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Carte d'un autre utilisateur gardee dans le carnet, en lecture seule. */
@Entity
@Table(name = "carte_enregistree")
@Getter
@Setter
public class CarteEnregistree {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carte_id", nullable = false)
    private Carte carte;

    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
