package made.qrcard.commun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String jwtSecret,
        long jwtValiditeMinutes,
        String cleChiffrement,
        String adminEmail,
        String adminMotDePasse,
        String googleClientId,
        String googleClientSecret,
        String mailExpediteur)
{

    public boolean googleActif()
    {
        return googleClientId != null && !googleClientId.isBlank()
                && googleClientSecret != null && !googleClientSecret.isBlank();
    }
}
