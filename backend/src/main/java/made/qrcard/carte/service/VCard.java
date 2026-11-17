package made.qrcard.carte.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Ecriture d'une vCard 3.0 (RFC 2426) : echappement, lignes de 75 octets au plus,
 * et etiquettes libres (« WhatsApp », « LinkedIn ») au format compris par l'iPhone.
 * Android ignore ces etiquettes et garde la valeur.
 */
final class VCard
{

    private static final int LONGUEUR_LIGNE = 75;

    private final StringBuilder texte = new StringBuilder();
    private int groupes;

    VCard()
    {
        ligne("BEGIN:VCARD");
        ligne("VERSION:3.0");
    }

    VCard nom(String prenom, String nom)
    {
        String p = valeur(prenom), n = valeur(nom);
        ligne("N:" + n + ";" + p + ";;;");
        ligne("FN:" + (p + " " + n).trim());
        return this;
    }

    VCard propriete(String nom, String valeur)
    {
        if (valeur != null && !valeur.isBlank()) ligne(nom + ":" + valeur(valeur));
        return this;
    }

    /** Valeur deja sure (numero E.164, adresse email, lien) : pas d'echappement des virgules. */
    VCard brute(String nom, String valeur)
    {
        if (valeur != null && !valeur.isBlank()) ligne(nom + ":" + valeur);
        return this;
    }

    /** Propriete accompagnee d'une etiquette affichee telle quelle sur iPhone. */
    VCard etiquetee(String nom, String valeur, String etiquette)
    {
        if (valeur == null || valeur.isBlank()) return this;
        String groupe = "item" + ++groupes + ".";
        ligne(groupe + nom + ":" + valeur);
        ligne(groupe + "X-ABLabel:" + valeur(etiquette));
        return this;
    }

    VCard adresse(String adresse)
    {
        if (adresse != null && !adresse.isBlank()) ligne("ADR;TYPE=WORK:;;" + valeur(adresse) + ";;;;");
        return this;
    }

    VCard photo(byte[] jpeg)
    {
        if (jpeg != null && jpeg.length > 0)
        {
            ligne("PHOTO;ENCODING=b;TYPE=JPEG:" + Base64.getEncoder().encodeToString(jpeg));
        }
        return this;
    }

    String fin()
    {
        ligne("END:VCARD");
        return texte.toString();
    }

    private static String valeur(String s)
    {
        if (s == null) return "";
        return s.trim()
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n");
    }

    /** Coupe en lignes de 75 octets au plus, sans couper un caractere UTF-8 ; la suite commence par une espace. */
    private void ligne(String contenu)
    {
        int octets = 0;
        int limite = LONGUEUR_LIGNE;
        for (int i = 0; i < contenu.length(); )
        {
            int point = contenu.codePointAt(i);
            int taille = new String(Character.toChars(point)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + taille > limite)
            {
                texte.append("\r\n ");
                octets = 0;
                // L'espace de continuation compte dans la ligne suivante
                limite = LONGUEUR_LIGNE - 1;
            }
            texte.appendCodePoint(point);
            octets += taille;
            i += Character.charCount(point);
        }
        texte.append("\r\n");
    }
}
