package com.qrcard.repository;

import com.qrcard.entite.CartePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartePhotoRepository extends JpaRepository<CartePhoto, UUID> {
}
