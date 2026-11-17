package made.qrcard.compte.service;

import made.qrcard.commun.securite.CryptoService;
import made.qrcard.compte.domaine.InscriptionAttenteRepository;
import made.qrcard.compte.domaine.StatutCompte;
import made.qrcard.compte.domaine.Utilisateur;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.securite.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnexionGoogleTest
{

    private static final String EMAIL = "awa@exemple.sn";

    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final InscriptionAttenteRepository attentes = mock(InscriptionAttenteRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final ApplicationEventPublisher evenements = mock(ApplicationEventPublisher.class);
    private AuthService auth;

    @BeforeEach
    void preparer()
    {
        auth = new AuthService(utilisateurs, attentes, mock(PasswordEncoder.class), jwt, mock(TotpService.class),
                mock(CryptoService.class), evenements);
        when(utilisateurs.findByGoogleId(any())).thenReturn(Optional.empty());
        when(utilisateurs.findByEmail(any())).thenReturn(Optional.empty());
        when(jwt.genererAcces(any())).thenReturn("jeton");
        when(jwt.genererMfa(any())).thenReturn("jeton-2fa");
    }

    private Utilisateur compteExistant()
    {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());
        u.setEmail(EMAIL);
        when(utilisateurs.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void compteExistantParEmail_connecteEtLieLeCompteGoogle()
    {
        Utilisateur u = compteExistant();

        var r = auth.connexionGoogle("g1", " Awa@Exemple.sn ", true, "Awa");

        assertEquals("jeton", r.token());
        assertNull(r.erreur());
        assertEquals("g1", u.getGoogleId());
        verify(evenements, never()).publishEvent(any(Object.class));
    }

    @Test
    void compteExistantAvec2fa_demandeLeCode()
    {
        compteExistant().setTotpActif(true);

        var r = auth.connexionGoogle("g1", EMAIL, true, "Awa");

        assertNull(r.token());
        assertEquals("jeton-2fa", r.mfaToken());
    }

    @Test
    void compteBloque_estRefuse()
    {
        compteExistant().setStatut(StatutCompte.BLOQUE);

        var r = auth.connexionGoogle("g1", EMAIL, true, "Awa");

        assertEquals("bloque", r.erreur());
        assertNull(r.token());
    }

    @Test
    void emailGoogleNonVerifie_neConnectePasAuCompteExistant()
    {
        Utilisateur u = compteExistant();

        var r = auth.connexionGoogle("g1", EMAIL, false, "Awa");

        assertEquals("email-non-verifie", r.erreur());
        assertNull(u.getGoogleId());
    }

    @Test
    void compteLieAUnAutreCompteGoogle_estRefuse()
    {
        compteExistant().setGoogleId("g2");

        var r = auth.connexionGoogle("g1", EMAIL, true, "Awa");

        assertEquals("compte-existant", r.erreur());
    }

    @Test
    void nouvelleAdresse_creeLeCompteEtAnnuleLInscriptionEnAttente()
    {
        var r = auth.connexionGoogle("g1", EMAIL, true, "Awa");

        assertNotNull(r.token());
        verify(utilisateurs).save(any());
        verify(evenements).publishEvent(any(Object.class));
        verify(attentes).deleteById(EMAIL);
    }
}
