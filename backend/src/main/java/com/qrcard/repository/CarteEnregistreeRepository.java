package com.qrcard.repository;

import com.qrcard.entite.CarteEnregistree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarteEnregistreeRepository extends JpaRepository<CarteEnregistree, UUID> {

    List<CarteEnregistree> findByUtilisateurIdOrderByCreatedAtDesc(UUID utilisateurId);

    Optional<CarteEnregistree> findByUtilisateurIdAndCarte_Id(UUID utilisateurId, UUID carteId);

    boolean existsByUtilisateurIdAndCarte_Id(UUID utilisateurId, UUID carteId);

    long countByCarte_Id(UUID carteId);

    @Modifying
    @Query("DELETE FROM CarteEnregistree e WHERE e.carte.id = :carteId")
    void supprimerPourCarte(UUID carteId);
}
