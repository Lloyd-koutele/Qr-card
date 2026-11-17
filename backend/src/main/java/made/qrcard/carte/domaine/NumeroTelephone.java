package made.qrcard.carte.domaine;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Numero de telephone ou WhatsApp, toujours au format international (+221771234567).
 * Le nombre de chiffres et les prefixes sont verifies selon le pays de l'indicatif.
 */
final class NumeroTelephone
{
    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private NumeroTelephone()
    {
    }

    static String normaliser(String brut)
    {
        String v = brut.replaceAll("[\\s.\\-()]", "");
        if (v.startsWith("00"))
        {
            v = "+" + v.substring(2);
        }
        if (!v.matches("^\\+[0-9]{4,17}$"))
        {
            throw new IllegalArgumentException("Numéro invalide : choisissez le pays puis saisissez le numéro, par exemple +221 77 123 45 67");
        }

        PhoneNumber numero;
        try
        {
            numero = UTIL.parse(v, null);
        }
        catch (NumberParseException e)
        {
            throw new IllegalArgumentException("Indicatif de pays inconnu : " + v);
        }

        String region = UTIL.getRegionCodeForCountryCode(numero.getCountryCode());
        String pays = "(" + nomPays(region) + ", +" + numero.getCountryCode() + ")";
        ValidationResult possible = UTIL.isPossibleNumberWithReason(numero);
        switch (possible)
        {
            case INVALID_COUNTRY_CODE ->
                throw new IllegalArgumentException("Indicatif de pays inconnu : +" + numero.getCountryCode());
            case TOO_SHORT, TOO_LONG, INVALID_LENGTH ->
                throw new IllegalArgumentException("Le numéro doit compter " + longueurs(region) + " chiffres après l'indicatif " + pays);
            default ->
            {
            }
        }
        if (!UTIL.isValidNumber(numero))
        {
            throw new IllegalArgumentException("Ce numéro n'existe pas " + pays + " : vérifiez les premiers chiffres");
        }
        return UTIL.format(numero, PhoneNumberFormat.E164);
    }

    private static String nomPays(String region)
    {
        String nom = Locale.of("", region).getDisplayCountry(Locale.FRENCH);
        return nom.isBlank() || nom.equals(region) ? region : nom;
    }

    /** Longueurs possibles du numero national, par exemple "9" ou "8 ou 9". */
    private static String longueurs(String region)
    {
        int indicatif = UTIL.getCountryCodeForRegion(region);
        Set<Integer> l = new TreeSet<>();
        for (int n = 2; n <= 17; n++)
        {
            PhoneNumber essai = new PhoneNumber().setCountryCode(indicatif).setNationalNumber((long) Math.pow(10, n - 1));
            if (UTIL.isPossibleNumberWithReason(essai) == ValidationResult.IS_POSSIBLE)
            {
                l.add(n);
            }
        }
        if (l.isEmpty())
        {
            return "un nombre précis de";
        }
        var liste = l.stream().map(String::valueOf).toList();
        if (liste.size() == 1)
        {
            return liste.get(0);
        }
        return String.join(", ", liste.subList(0, liste.size() - 1)) + " ou " + liste.get(liste.size() - 1);
    }
}
