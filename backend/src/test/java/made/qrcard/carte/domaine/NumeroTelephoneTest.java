package made.qrcard.carte.domaine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroTelephoneTest
{
    @Test
    void numero_valide_mis_au_format_international()
    {
        assertThat(TypeContact.TELEPHONE.normaliser("+221 77 123 45 67")).isEqualTo("+221771234567");
        assertThat(TypeContact.WHATSAPP.normaliser("00225 07 07 07 07 07")).isEqualTo("+2250707070707");
        assertThat(TypeContact.TELEPHONE.normaliser("+33 6 12 34 56 78")).isEqualTo("+33612345678");
    }

    @Test
    void longueur_verifiee_selon_le_pays()
    {
        assertThatThrownBy(() -> TypeContact.TELEPHONE.normaliser("+22177123456"))
            .hasMessageContaining("(Sénégal, +221)")
            .hasMessageContaining("9 chiffres");
        assertThatThrownBy(() -> TypeContact.WHATSAPP.normaliser("+2250707070"))
            .hasMessageContaining(", +225)");
    }

    @Test
    void indicatif_obligatoire_et_connu()
    {
        assertThatThrownBy(() -> TypeContact.TELEPHONE.normaliser("771234567"))
            .hasMessageContaining("choisissez le pays");
        assertThatThrownBy(() -> TypeContact.TELEPHONE.normaliser("+999123456789"))
            .hasMessageContaining("Indicatif de pays inconnu");
    }

    @Test
    void prefixe_inexistant_refuse()
    {
        assertThatThrownBy(() -> TypeContact.TELEPHONE.normaliser("+221111234567"))
            .hasMessageContaining("n'existe pas");
    }
}
