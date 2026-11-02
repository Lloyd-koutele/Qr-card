package made.qrcard.securite;

import made.qrcard.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Chiffrement AES-GCM des secrets 2FA stockes en base. */
@Service
public class CryptoService {

    private static final int IV = 12;
    private final SecretKeySpec cle;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(AppProperties props) throws GeneralSecurityException {
        String brut = props.cleChiffrement();
        if (brut == null || brut.length() < 32) {
            throw new IllegalStateException("APP_CLE_CHIFFREMENT doit contenir au moins 32 caractères");
        }
        byte[] octets = MessageDigest.getInstance("SHA-256").digest(brut.getBytes(StandardCharsets.UTF_8));
        this.cle = new SecretKeySpec(octets, "AES");
    }

    public String chiffrer(String clair) {
        try {
            byte[] iv = new byte[IV];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(128, iv));
            byte[] chiffre = c.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV + chiffre.length).put(iv).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    public String dechiffrer(String chiffre) {
        try {
            byte[] tout = Base64.getDecoder().decode(chiffre);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(128, tout, 0, IV));
            return new String(c.doFinal(tout, IV, tout.length - IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Déchiffrement impossible", e);
        }
    }
}
