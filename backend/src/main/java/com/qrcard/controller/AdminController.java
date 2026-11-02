package com.qrcard.controller;

import com.qrcard.dto.Dto.*;
import com.qrcard.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.qrcard.securite.SecuriteUtils.idCourant;

/** Reserve au role SUPER_ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return admin.stats(idCourant());
    }

    @GetMapping("/utilisateurs")
    public PageResponse<AdminUtilisateur> utilisateurs(@RequestParam(defaultValue = "") String q,
                                                       @RequestParam(defaultValue = "0") int page) {
        return admin.lister(idCourant(), q, page);
    }

    @PostMapping("/utilisateurs/{id}/bloquer")
    public AdminUtilisateur bloquer(@PathVariable UUID id, @Valid @RequestBody BlocageRequest req) {
        return admin.bloquer(idCourant(), id, req);
    }

    @PostMapping("/utilisateurs/{id}/debloquer")
    public AdminUtilisateur debloquer(@PathVariable UUID id) {
        return admin.debloquer(idCourant(), id);
    }

    @GetMapping("/journal")
    public PageResponse<AuditItem> journal(@RequestParam(defaultValue = "0") int page) {
        return admin.journal(idCourant(), page);
    }
}
