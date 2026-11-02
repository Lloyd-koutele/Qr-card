package made.qrcard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/** Connexion Google active seulement si GOOGLE_CLIENT_ID et GOOGLE_CLIENT_SECRET sont renseignes. */
@Configuration
@ConditionalOnExpression("'${app.google-client-id:}' != '' and '${app.google-client-secret:}' != ''")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AppProperties props) {
        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(props.googleClientId())
                        .clientSecret(props.googleClientSecret())
                        .scope("openid", "email", "profile")
                        .build());
    }
}
