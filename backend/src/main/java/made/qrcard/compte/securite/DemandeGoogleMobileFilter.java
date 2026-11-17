package made.qrcard.compte.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/** Au depart vers Google, retient le defi de l'application mobile (?defi=...) pour le retour. */
public class DemandeGoogleMobileFilter extends OncePerRequestFilter
{

    private static final Pattern DEFI = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        return !"/oauth2/authorization/google".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String defi = request.getParameter("defi");
        if (defi != null && DEFI.matcher(defi).matches())
        {
            request.getSession(true).setAttribute(RetourGoogleMobile.ATTRIBUT_DEFI, defi);
        }
        else
        {
            HttpSession session = request.getSession(false);
            if (session != null) session.removeAttribute(RetourGoogleMobile.ATTRIBUT_DEFI);
        }
        chain.doFilter(request, response);
    }
}
