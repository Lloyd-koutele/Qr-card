package made.qrcard.carnet.service;

import made.qrcard.carnet.domaine.CarteEnregistree;
import made.qrcard.carnet.domaine.CarteEnregistreeRepository;
import made.qrcard.carnet.dto.CarnetDto.*;
import made.qrcard.carte.domaine.Carte;
import made.qrcard.carte.dto.CarteDto.*;
import made.qrcard.carte.service.CarteService;
import made.qrcard.commun.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Cartes des autres gardees par un utilisateur connecte, toujours en lecture seule. */
@Service
public class CarnetService
{

    private final CarteEnregistreeRepository enregistrements;
    private final CarteService cartes;

    public CarnetService(CarteEnregistreeRepository enregistrements, CarteService cartes)
    {
        this.enregistrements = enregistrements;
        this.cartes = cartes;
    }

    @Transactional(readOnly = true)
    public List<CarnetItem> lister(UUID utilisateurId)
    {
        return enregistrements.findByUtilisateurIdOrderByCreatedAtDesc(utilisateurId).stream()
                .map(e -> versDto(e, utilisateurId))
                .toList();
    }

    @Transactional
    public CarnetItem enregistrer(UUID utilisateurId, EnregistrerRequest req)
    {
        Carte carte = cartes.carteDisponible(req.jeton());
        if (carte.getUtilisateur().getId().equals(utilisateurId))
        {
            throw ApiException.invalide("C'est votre propre carte");
        }
        CarteEnregistree e = enregistrements.findByUtilisateurIdAndCarte_Id(utilisateurId, carte.getId())
                .orElseGet(() ->
                {
                    CarteEnregistree n = new CarteEnregistree();
                    n.setUtilisateurId(utilisateurId);
                    n.setCarte(carte);
                    return enregistrements.save(n);
                });
        if (req.note() != null) e.setNote(req.note().isBlank() ? null : req.note().trim());
        return versDto(e, utilisateurId);
    }

    @Transactional
    public CarnetItem modifierNote(UUID utilisateurId, UUID id, NoteRequest req)
    {
        CarteEnregistree e = charger(utilisateurId, id);
        e.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        return versDto(e, utilisateurId);
    }

    @Transactional
    public void supprimer(UUID utilisateurId, UUID id)
    {
        enregistrements.delete(charger(utilisateurId, id));
    }

    private CarteEnregistree charger(UUID utilisateurId, UUID id)
    {
        return enregistrements.findById(id)
                .filter(e -> e.getUtilisateurId().equals(utilisateurId))
                .orElseThrow(() -> ApiException.introuvable("Carte introuvable dans votre carnet"));
    }

    private CarnetItem versDto(CarteEnregistree e, UUID utilisateurId)
    {
        Carte c = e.getCarte();
        CarteResponse carte = cartes.estDisponible(c) ? cartes.versDto(c, utilisateurId) : null;
        return new CarnetItem(e.getId(), e.getNote(), e.getCreatedAt(), carte);
    }
}
