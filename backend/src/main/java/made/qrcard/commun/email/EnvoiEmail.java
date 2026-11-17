package made.qrcard.commun.email;

import made.qrcard.commun.config.AppProperties;
import made.qrcard.commun.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sans serveur SMTP configure (MAIL_HOST vide), le message est ecrit dans les logs : developpement uniquement. */
@Service
public class EnvoiEmail
{

    private static final Logger log = LoggerFactory.getLogger(EnvoiEmail.class);

    private final ObjectProvider<JavaMailSender> expediteur;
    private final AppProperties props;
    private final String hote;

    public EnvoiEmail(ObjectProvider<JavaMailSender> expediteur, AppProperties props,
                      @Value("${spring.mail.host:}") String hote)
    {
        this.expediteur = expediteur;
        this.props = props;
        this.hote = hote;
    }

    public void envoyer(String destinataire, String sujet, String texte)
    {
        if (hote.isBlank())
        {
            log.warn("MAIL_HOST non configuré, email non envoyé à {} :\n{}", destinataire, texte);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.mailExpediteur());
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(texte);
        try
        {
            expediteur.getObject().send(message);
        }
        catch (MailException e)
        {
            log.error("Envoi de l'email impossible : {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "L'email n'a pas pu être envoyé. Réessayez dans quelques instants.");
        }
    }
}
