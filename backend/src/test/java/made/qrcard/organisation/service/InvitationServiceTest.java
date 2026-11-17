package made.qrcard.organisation.service;

import made.qrcard.carte.service.CarteService;
import made.qrcard.commun.config.AppProperties;
import made.qrcard.commun.email.EnvoiEmail;
import made.qrcard.compte.domaine.UtilisateurRepository;
import made.qrcard.compte.service.AuthService;
import made.qrcard.compte.service.InscriptionService;
import made.qrcard.organisation.domaine.*;
import made.qrcard.organisation.dto.OrganisationDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest
{

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ORGANISATION = UUID.randomUUID();

    @Mock InvitationOrganisationRepository invitations;
    @Mock MembreOrganisationRepository membres;
    @Mock UtilisateurRepository utilisateurs;
    @Mock OrganisationService organisations;
    @Mock CarteService cartes;
    @Mock InscriptionService inscriptions;
    @Mock AuthService auth;
    @Mock EnvoiEmail emails;
    @Mock AppProperties props;

    InvitationService service;

    @BeforeEach
    void preparer()
    {
        service = new InvitationService(invitations, membres, utilisateurs, organisations, cartes, inscriptions, auth, emails,
                new SyncTaskExecutor(), props);
        MembreOrganisation admin = new MembreOrganisation();
        admin.setOrganisationId(ORGANISATION);
        admin.setEmail("admin@organisation.sn");
        lenient().when(organisations.adminActif(ADMIN, ORGANISATION)).thenReturn(admin);
        Organisation e = new Organisation();
        e.setNom("Sonatel");
        lenient().when(organisations.charger(ORGANISATION)).thenReturn(e);
        lenient().when(props.frontendUrl()).thenReturn("https://madeqrcard.sn");
    }

    private static InvitationsRequest une(String email)
    {
        return new InvitationsRequest(List.of(new InvitationSaisie(email, null, null)));
    }

    /** La garantie principale : l'envoi ne regarde jamais si un compte existe. */
    @Test
    void inviter_ne_consulte_jamais_les_comptes()
    {
        InvitationsResultat r = service.inviter(ADMIN, ORGANISATION, une("Personne@Exemple.sn"));

        assertEquals(1, r.envoyees());
        assertTrue(r.ignorees().isEmpty());
        verifyNoInteractions(utilisateurs, inscriptions, cartes);
        verify(emails).envoyer(eq("personne@exemple.sn"), anyString(), contains("/invitation/"));
    }

    @Test
    void le_jeton_du_lien_n_est_pas_conserve()
    {
        service.inviter(ADMIN, ORGANISATION, une("personne@exemple.sn"));

        ArgumentCaptor<String> texte = ArgumentCaptor.forClass(String.class);
        verify(emails).envoyer(anyString(), anyString(), texte.capture());
        String jeton = texte.getValue().split("/invitation/")[1].split("\\s")[0];
        ArgumentCaptor<InvitationOrganisation> i = ArgumentCaptor.forClass(InvitationOrganisation.class);
        verify(invitations).save(i.capture());
        assertNotEquals(jeton, i.getValue().getJetonEmpreinte());
        assertEquals(InvitationService.empreinte(jeton), i.getValue().getJetonEmpreinte());
    }

    @Test
    void une_adresse_n_est_pas_relancee_avant_24_heures()
    {
        InvitationOrganisation deja = new InvitationOrganisation();
        deja.setEnvoyeLe(Instant.now().minus(23, ChronoUnit.HOURS));
        when(invitations.findByOrganisationIdAndEmail(ORGANISATION, "personne@exemple.sn")).thenReturn(Optional.of(deja));

        InvitationsResultat r = service.inviter(ADMIN, ORGANISATION, une("personne@exemple.sn"));

        assertEquals(0, r.envoyees());
        assertEquals(1, r.ignorees().size());
        verifyNoInteractions(emails);
    }

    @Test
    void le_quota_journalier_arrete_les_envois()
    {
        when(invitations.countByOrganisationIdAndEnvoyeLeAfter(eq(ORGANISATION), any())).thenReturn(100L);

        InvitationsResultat r = service.inviter(ADMIN, ORGANISATION, une("personne@exemple.sn"));

        assertEquals(0, r.envoyees());
        verifyNoInteractions(emails);
    }

    @Test
    void refuser_une_invitation_expiree_est_impossible()
    {
        InvitationOrganisation expiree = new InvitationOrganisation();
        expiree.setExpireLe(Instant.now().minus(1, ChronoUnit.DAYS));
        when(invitations.findByJetonEmpreinte(anyString())).thenReturn(Optional.of(expiree));

        assertThrows(made.qrcard.commun.web.ApiException.class, () -> service.refuser("jeton"));
        assertEquals(StatutInvitation.EN_ATTENTE, expiree.getStatut());
    }

    @Test
    void accepter_quand_on_est_deja_membre_de_cette_organisation_est_refuse()
    {
        InvitationOrganisation i = new InvitationOrganisation();
        i.setOrganisationId(ORGANISATION);
        i.setEmail("pro@organisation.sn");
        i.setExpireLe(Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.findByJetonEmpreinte(anyString())).thenReturn(Optional.of(i));
        Organisation active = new Organisation();
        when(organisations.charger(ORGANISATION)).thenReturn(active);
        UUID compte = UUID.randomUUID();
        when(utilisateurs.findById(compte)).thenReturn(Optional.of(new made.qrcard.compte.domaine.Utilisateur()));
        when(membres.existsByOrganisationIdAndUtilisateurId(eq(ORGANISATION), any())).thenReturn(true);

        assertThrows(made.qrcard.commun.web.ApiException.class, () -> service.accepter(compte, "jeton"));
        assertEquals(StatutInvitation.EN_ATTENTE, i.getStatut());
        verifyNoInteractions(cartes);
    }

    @Test
    void accepter_en_etant_deja_dans_une_autre_organisation_est_possible()
    {
        InvitationOrganisation i = new InvitationOrganisation();
        i.setOrganisationId(ORGANISATION);
        i.setEmail("pro@organisation.sn");
        i.setExpireLe(Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.findByJetonEmpreinte(anyString())).thenReturn(Optional.of(i));
        when(organisations.charger(ORGANISATION)).thenReturn(new Organisation());
        UUID compte = UUID.randomUUID();
        made.qrcard.compte.domaine.Utilisateur u = new made.qrcard.compte.domaine.Utilisateur();
        when(utilisateurs.findById(compte)).thenReturn(Optional.of(u));
        // Membre d'autres organisations, mais pas de celle-ci
        when(membres.existsByOrganisationIdAndUtilisateurId(eq(ORGANISATION), any())).thenReturn(false);

        service.accepter(compte, "jeton");

        assertEquals(StatutInvitation.ACCEPTEE, i.getStatut());
        verify(organisations).verifierPlace(any());
        verify(cartes).creerProfessionnelle(eq(u), eq(ORGANISATION), any(), any(), eq("pro@organisation.sn"));
    }
}
