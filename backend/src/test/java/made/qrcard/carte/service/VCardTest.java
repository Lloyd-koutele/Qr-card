package made.qrcard.carte.service;

import made.qrcard.carte.domaine.Carte;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class VCardTest
{

    @Test
    void echappe_les_caracteres_reserves_et_les_retours_a_la_ligne()
    {
        String v = new VCard().nom("Aïcha", "Koné").propriete("NOTE", "Ventes, achats; et\nsuivi").fin();
        assertThat(v).startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n")
                .contains("N:Koné;Aïcha;;;\r\n", "FN:Aïcha Koné\r\n", "NOTE:Ventes\\, achats\\; et\\nsuivi\r\n")
                .endsWith("END:VCARD\r\n");
    }

    @Test
    void lignes_de_75_octets_au_plus_sans_couper_un_caractere()
    {
        String note = "é".repeat(200);
        String v = new VCard().propriete("NOTE", note).fin();
        for (String ligne : v.split("\r\n"))
        {
            assertThat(ligne.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        // Deplier redonne la valeur d'origine
        assertThat(v.replace("\r\n ", "")).contains("NOTE:" + note + "\r\n");
    }

    @Test
    void etiquettes_groupees_pour_l_iphone()
    {
        String v = new VCard()
                .etiquetee("TEL;TYPE=CELL", "+221709876543", "WhatsApp")
                .etiquetee("URL", "https://linkedin.com/in/aichakone", "LinkedIn")
                .fin();
        assertThat(v).contains("item1.TEL;TYPE=CELL:+221709876543\r\n", "item1.X-ABLabel:WhatsApp\r\n",
                "item2.URL:https://linkedin.com/in/aichakone\r\n", "item2.X-ABLabel:LinkedIn\r\n");
    }

    @Test
    void nom_de_fichier_lisible_sans_accents()
    {
        Carte c = new Carte();
        c.setPrenom("Aïcha");
        c.setNom("Koné / N'Diaye");
        assertThat(VcardService.nomFichier(c)).isEqualTo("Aicha Kone NDiaye.vcf");
        assertThat(VcardService.nomFichier(new Carte())).isEqualTo("contact.vcf");
    }

    @Test
    void photo_reduite_en_jpeg() throws Exception
    {
        BufferedImage grande = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(grande, "png", png);
        byte[] vignette = PhotoVcard.vignette(png.toByteArray());
        BufferedImage lue = ImageIO.read(new ByteArrayInputStream(vignette));
        assertThat(lue.getWidth()).isEqualTo(400);
        assertThat(lue.getHeight()).isEqualTo(300);
        assertThat(Arrays.copyOf(vignette, 2)).containsExactly((byte) 0xFF, (byte) 0xD8);
        assertThat(PhotoVcard.vignette("pas une image".getBytes())).isNull();
    }
}
