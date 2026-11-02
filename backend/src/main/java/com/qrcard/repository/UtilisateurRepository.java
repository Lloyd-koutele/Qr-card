package com.qrcard.repository;

import com.qrcard.entite.StatutCompte;
import com.qrcard.entite.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    long countByStatut(StatutCompte statut);

    Page<Utilisateur> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
