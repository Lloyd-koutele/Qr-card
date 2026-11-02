package made.qrcard.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return erreur(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argument(IllegalArgumentException e) {
        return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Requête invalide");
        return erreur(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> illisible(HttpMessageNotReadableException e) {
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tropGros(MaxUploadSizeExceededException e) {
        return erreur(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop lourd (2 Mo maximum)");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> absent(NoResourceFoundException e) {
        return erreur(HttpStatus.NOT_FOUND, "Ressource introuvable");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> autre(Exception e) {
        log.error("Erreur inattendue", e);
        return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue");
    }

    private static ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message == null ? "" : message));
    }
}
