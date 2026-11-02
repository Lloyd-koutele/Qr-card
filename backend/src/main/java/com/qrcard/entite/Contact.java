package com.qrcard.entite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "contact")
@Getter
@Setter
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carte_id", nullable = false)
    private Carte carte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeContact type;

    /** Valeur normalisee : numero international ou pseudo, jamais un lien libre. */
    @Column(nullable = false)
    private String valeur;

    @Column(nullable = false)
    private int ordre;
}
