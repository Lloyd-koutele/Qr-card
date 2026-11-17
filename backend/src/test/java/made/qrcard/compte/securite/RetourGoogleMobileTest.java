package made.qrcard.compte.securite;

import made.qrcard.commun.web.ApiException;
import made.qrcard.compte.dto.CompteDto.ConnexionResponse;
import made.qrcard.compte.service.AuthService.GoogleResultat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetourGoogleMobileTest
{

    private static final String VERIFICATEUR = "v".repeat(43);

    private final RetourGoogleMobile mobile = new RetourGoogleMobile();

    @Test
    void le_bon_verificateur_donne_la_session()
    {
        String code = mobile.emettre(new GoogleResultat("jwt", null, null), RetourGoogleMobile.empreinte(VERIFICATEUR));

        ConnexionResponse r = mobile.echanger(code, VERIFICATEUR);

        assertEquals("jwt", r.token());
        assertFalse(r.mfaRequis());
    }

    @Test
    void la_double_authentification_reste_demandee()
    {
        String code = mobile.emettre(new GoogleResultat(null, "mfa", null), RetourGoogleMobile.empreinte(VERIFICATEUR));

        ConnexionResponse r = mobile.echanger(code, VERIFICATEUR);

        assertNull(r.token());
        assertTrue(r.mfaRequis());
    }

    /** Une application qui intercepte le lien n'a pas le verificateur : le code est perdu. */
    @Test
    void un_code_intercepte_est_inutilisable()
    {
        String code = mobile.emettre(new GoogleResultat("jwt", null, null), RetourGoogleMobile.empreinte(VERIFICATEUR));

        assertThrows(ApiException.class, () -> mobile.echanger(code, "x".repeat(43)));
        assertThrows(ApiException.class, () -> mobile.echanger(code, VERIFICATEUR));
    }
}
