package made.qrcard.service;

import made.qrcard.dto.Dto.*;
import made.qrcard.entite.AuditAdmin;
import made.qrcard.entite.Role;
import made.qrcard.entite.StatutCompte;
import made.qrcard.entite.Utilisateur;
import made.qrcard.repository.AuditAdminRepository;
import made.qrcard.repository.CarteRepository;
import made.qrcard.repository.UtilisateurRepository;
import made.qrcard.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * L'admin ne voit que des metadonnees de compte (email, statut, dates) et peut
 * bloquer ou debloquer. Il ne lit ni ne modifie jamais le contenu des cartes.
 */
@Service
public class AdminService {

    private static final int TAILLE_PAGE = 20;

    private final UtilisateurRepository utilisateurs;
    private final CarteRepository cartes;
    private final AuditAdminRepository audit;

    public AdminService(UtilisateurRepository utilisateurs, CarteRepository cartes, AuditAdminRepository audit) {
        this.utilisateurs = utilisateurs;
        this.cartes = cartes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUtilisateur> lister(UUID adminId, String recherche, int page) {
        verifierSecurite(adminId);
        Page<Utilisateur> p = utilisateurs.findByEmailContainingIgnoreCase(recherche == null ? "" : recherche.trim(),
                PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(p.map(AdminService::versDto).getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(UUID adminId) {
        verifierSecurite(adminId);
        return new StatsResponse(utilisateurs.count(), utilisateurs.countByStatut(StatutCompte.ACTIF),
                utilisateurs.countByStatut(StatutCompte.BLOQUE), cartes.count());
    }

    @Transactional
    public AdminUtilisateur bloquer(UUID adminId, UUID cibleId, BlocageRequest req) {
        Utilisateur admin = verifierSecurite(adminId);
        Utilisateur cible = charger(cibleId);
        if (cible.getId().equals(adminId)) throw ApiException.invalide("Vous ne pouvez pas bloquer votre propre compte");
        if (cible.getRole() == Role.SUPER_ADMIN) throw ApiException.interdit("Un administrateur général ne peut pas être bloqué");
        cible.setStatut(StatutCompte.BLOQUE);
        cible.setMotifBlocage(req.motif().trim());
        tracer(admin, "BLOQUER", cible, req.motif().trim());
        return versDto(cible);
    }

    @Transactional
    public AdminUtilisateur debloquer(UUID adminId, UUID cibleId) {
        Utilisateur admin = verifierSecurite(adminId);
        Utilisateur cible = charger(cibleId);
        cible.setStatut(StatutCompte.ACTIF);
        cible.setMotifBlocage(null);
        tracer(admin, "DEBLOQUER", cible, null);
        return versDto(cible);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditItem> journal(UUID adminId, int page) {
        verifierSecurite(adminId);
        var p = audit.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), TAILLE_PAGE))
                .map(a -> new AuditItem(a.getCreatedAt(), a.getAdminEmail(), a.getAction(), a.getCibleEmail(), a.getMotif()));
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    /** Un admin doit avoir change son mot de passe initial et active la 2FA. */
    private Utilisateur verifierSecurite(UUID adminId) {
        Utilisateur admin = charger(adminId);
        if (!admin.isTotpActif() || admin.isChangerMotDePasse()) {
            throw ApiException.interdit("Changez votre mot de passe et activez la double authentification avant d'administrer");
        }
        return admin;
    }

    private Utilisateur charger(UUID id) {
        return utilisateurs.findById(id).orElseThrow(() -> ApiException.introuvable("Compte introuvable"));
    }

    private void tracer(Utilisateur admin, String action, Utilisateur cible, String motif) {
        AuditAdmin a = new AuditAdmin();
        a.setAdminId(admin.getId());
        a.setAdminEmail(admin.getEmail());
        a.setAction(action);
        a.setCibleId(cible.getId());
        a.setCibleEmail(cible.getEmail());
        a.setMotif(motif);
        audit.save(a);
    }

    private static AdminUtilisateur versDto(Utilisateur u) {
        String methode = u.getGoogleId() != null && u.getMotDePasse() != null ? "Google et mot de passe"
                : u.getGoogleId() != null ? "Google" : "Mot de passe";
        return new AdminUtilisateur(u.getId(), u.getEmail(), u.getRole().name(), u.getStatut().name(), u.getMotifBlocage(),
                methode, u.isTotpActif(), u.getCreatedAt(), u.getDerniereConnexion());
    }
}
