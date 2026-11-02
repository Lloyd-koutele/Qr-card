package made.qrcard.controller;

import made.qrcard.dto.Dto.*;
import made.qrcard.service.CarnetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static made.qrcard.securite.SecuriteUtils.idCourant;

@RestController
@RequestMapping("/api/carnet")
public class CarnetController {

    private final CarnetService carnet;

    public CarnetController(CarnetService carnet) {
        this.carnet = carnet;
    }

    @GetMapping
    public List<CarnetItem> lister() {
        return carnet.lister(idCourant());
    }

    @PostMapping
    public CarnetItem enregistrer(@Valid @RequestBody EnregistrerRequest req) {
        return carnet.enregistrer(idCourant(), req);
    }

    @PutMapping("/{id}")
    public CarnetItem modifierNote(@PathVariable UUID id, @Valid @RequestBody NoteRequest req) {
        return carnet.modifierNote(idCourant(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        carnet.supprimer(idCourant(), id);
        return ResponseEntity.noContent().build();
    }
}
