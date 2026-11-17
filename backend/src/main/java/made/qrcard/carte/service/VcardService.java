package made.qrcard.carte.service;

import made.qrcard.carte.domaine.Carte;
import made.qrcard.carte.domaine.CartePhotoRepository;
import made.qrcard.carte.domaine.Contact;
import made.qrcard.carte.domaine.TypeContact;
import made.qrcard.carte.dto.CarteDto.MarqueDto;
import made.qrcard.commun.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

/**
 * Fiche contact (.vcf) d'une carte, ouverte sans compte : le visiteur l'ajoute
 * directement aux contacts de son telephone.
 */
@Service
public class VcardService
{

    private final CarteService cartes;
    private final CartePhotoRepository photos;
    private final AppProperties props;

    public VcardService(CarteService cartes, CartePhotoRepository photos, AppProperties props)
    {
        this.cartes = cartes;
        this.photos = photos;
        this.props = props;
    }

    public record FichierVcard(String nom, String contenu)
    {
    }

    @Transactional(readOnly = true)
    public FichierVcard vcard(String jeton)
    {
        Carte c = cartes.carteDisponible(jeton);
        Optional<MarqueDto> marque = cartes.marque(c);
        VCard v = new VCard().nom(c.getPrenom(), c.getNom());
        marque.ifPresent(m -> v.propriete("ORG", m.nom()));
        v.propriete("TITLE", c.getProfession());
        if (c.getPhotoVersion() != null)
        {
            photos.findById(c.getId()).ifPresent(p -> v.photo(PhotoVcard.vignette(p.getDonnees())));
        }

        List<Contact> contacts = c.getContacts();
        List<String> telephones = contacts.stream().filter(ct -> ct.getType() == TypeContact.TELEPHONE)
                .map(Contact::getValeur).toList();
        for (Contact ct : contacts)
        {
            switch (ct.getType())
            {
                case TELEPHONE -> v.brute("TEL;TYPE=CELL", ct.getValeur());
                // Un vrai numero, pas seulement un lien ; inutile de le repeter s'il est deja la
                case WHATSAPP ->
                {
                    if (!telephones.contains(ct.getValeur())) v.etiquetee("TEL;TYPE=CELL", ct.getValeur(), "WhatsApp");
                }
                case EMAIL -> v.brute("EMAIL;TYPE=INTERNET", ct.getValeur());
                default ->
                {
                }
            }
        }
        // La carte en ligne d'abord : la vCard ne se met pas a jour, la carte si
        v.etiquetee("URL", props.frontendUrl().replaceAll("/+$", "") + "/c/" + c.getJeton(), "MadeQrCard");
        for (Contact ct : contacts)
        {
            if (ct.getType() != TypeContact.TELEPHONE && ct.getType() != TypeContact.WHATSAPP && ct.getType() != TypeContact.EMAIL)
            {
                v.etiquetee("URL", ct.getType().lien(ct.getValeur()), ct.getType().libelle());
            }
        }
        marque.ifPresent(m -> v.adresse(m.adresse()));
        v.propriete("NOTE", c.getBio());
        return new FichierVcard(nomFichier(c), v.fin());
    }

    /** « Aicha Kone.vcf » : lisible quand le fichier est envoye par WhatsApp ou par email. */
    static String nomFichier(Carte c)
    {
        String nom = ((c.getPrenom() == null ? "" : c.getPrenom()) + " " + (c.getNom() == null ? "" : c.getNom())).trim();
        String ascii = Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ._-]", "").replaceAll("\\s+", " ").trim();
        return (ascii.isEmpty() ? "contact" : ascii) + ".vcf";
    }
}
