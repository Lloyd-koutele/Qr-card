package made.qrcard.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

/** Codes a 6 chiffres renouveles toutes les 30 s (Google Authenticator, Authy...). */
@Service
public class TotpService {

    private final DefaultSecretGenerator generateur = new DefaultSecretGenerator();
    private final DefaultCodeVerifier verificateur =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String nouveauSecret() {
        return generateur.generate();
    }

    public String uri(String email, String secret) {
        return new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("MadeQrCard")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();
    }

    public boolean verifier(String secret, String code) {
        return code != null && code.matches("\\d{6}") && verificateur.isValidCode(secret, code);
    }
}
