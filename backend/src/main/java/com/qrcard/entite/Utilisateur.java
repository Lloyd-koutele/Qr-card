package com.qrcard.entite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "nom_affiche", nullable = false)
    private String nomAffiche;

    /** Vide pour un compte cree avec Google. */
    @Column(name = "mot_de_passe")
    private String motDePasse;

    @Column(name = "google_id")
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCompte statut = StatutCompte.ACTIF;

    @Column(name = "motif_blocage")
    private String motifBlocage;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_affichage", nullable = false)
    private ModeAffichage modeAffichage = ModeAffichage.SYSTEME;

    /** Secret TOTP chiffre (AES-GCM). */
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_actif", nullable = false)
    private boolean totpActif;

    /** Empreintes BCrypt des codes de secours, separees par ";". */
    @Column(name = "codes_secours")
    private String codesSecours;

    @Column(name = "echecs_2fa", nullable = false)
    private int echecs2fa;

    @Column(name = "verrouille_jusqua")
    private Instant verrouilleJusqua;

    @Column(name = "changer_mot_de_passe", nullable = false)
    private boolean changerMotDePasse;

    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(name = "uo_id")
    private UUID uoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    public boolean estActif() {
        return statut == StatutCompte.ACTIF;
    }

    public boolean estAdmin() {
        return role != Role.USER;
    }
}
