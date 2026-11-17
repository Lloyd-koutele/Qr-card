package made.qrcard.carnet.service;

import made.qrcard.carnet.domaine.CarteEnregistreeRepository;
import made.qrcard.carte.service.EnregistrementsDeCarte;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EnregistrementsDuCarnet implements EnregistrementsDeCarte
{

    private final CarteEnregistreeRepository enregistrements;

    public EnregistrementsDuCarnet(CarteEnregistreeRepository enregistrements)
    {
        this.enregistrements = enregistrements;
    }

    @Override
    public boolean estEnregistree(UUID utilisateurId, UUID carteId)
    {
        return enregistrements.existsByUtilisateurIdAndCarte_Id(utilisateurId, carteId);
    }

    @Override
    public long compter(UUID carteId)
    {
        return enregistrements.countByCarte_Id(carteId);
    }

    @Override
    public void oublier(UUID carteId)
    {
        enregistrements.supprimerPourCarte(carteId);
    }
}
