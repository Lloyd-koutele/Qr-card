package com.qrcard.entite;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;


public enum TypeContact {
    TELEPHONE, WHATSAPP, TELEGRAM, INSTAGRAM, FACEBOOK, TIKTOK, LINKEDIN, SNAPCHAT, X, EMAIL, SITE_WEB;

    private static final Pattern NUMERO = Pattern.compile("^\\+?[0-9]{6,15}$");
    private static final Pattern PSEUDO = Pattern.compile("^[A-Za-z0-9._-]{1,60}$");
    private static final Pattern EMAIL_VALIDE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public String normaliser(String brut) {
        String v = brut == null ? "" : brut.trim();
        switch (this) {
            case TELEPHONE, WHATSAPP -> {
                v = v.replaceAll("[\\s.\\-()]", "");
                if (v.startsWith("00")) v = "+" + v.substring(2);
                if (!NUMERO.matcher(v).matches()) {
                    throw new IllegalArgumentException("Numéro invalide : utilisez le format international, par exemple +2250700000000");
                }
                if (this == WHATSAPP && !v.startsWith("+")) {
                    throw new IllegalArgumentException("Le numéro WhatsApp doit commencer par l'indicatif du pays, par exemple +225");
                }
                return v;
            }
            case EMAIL -> {
                v = v.toLowerCase(Locale.ROOT);
                if (v.length() > 255 || !EMAIL_VALIDE.matcher(v).matches()) {
                    throw new IllegalArgumentException("Adresse email invalide");
                }
                return v;
            }
            case SITE_WEB -> {
                if (!v.matches("(?i)^https?://.*")) v = "https://" + v;
                try {
                    URI uri = URI.create(v);
                    if (uri.getHost() == null || !uri.getHost().contains(".") || v.length() > 255) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Adresse de site web invalide");
                }
                return v;
            }
            default -> {
                // Pseudo : on accepte aussi un lien colle, dont on garde le dernier segment
                v = v.replaceAll("[?#].*$", "").replaceAll("/+$", "");
                if (v.contains("/")) v = v.substring(v.lastIndexOf('/') + 1);
                if (v.startsWith("@")) v = v.substring(1);
                if (!PSEUDO.matcher(v).matches()) {
                    throw new IllegalArgumentException("Nom d'utilisateur invalide pour " + libelle());
                }
                return v;
            }
        }
    }

    public String lien(String v) {
        return switch (this) {
            case TELEPHONE -> "tel:" + v;
            case WHATSAPP -> "https://wa.me/" + v.replace("+", "");
            case TELEGRAM -> "https://t.me/" + v;
            case INSTAGRAM -> "https://instagram.com/" + v;
            case FACEBOOK -> "https://facebook.com/" + v;
            case TIKTOK -> "https://www.tiktok.com/@" + v;
            case LINKEDIN -> "https://www.linkedin.com/in/" + v;
            case SNAPCHAT -> "https://www.snapchat.com/add/" + v;
            case X -> "https://x.com/" + v;
            case EMAIL -> "mailto:" + v;
            case SITE_WEB -> v;
        };
    }

    public String libelle() {
        return switch (this) {
            case TELEPHONE -> "Téléphone";
            case WHATSAPP -> "WhatsApp";
            case TELEGRAM -> "Telegram";
            case INSTAGRAM -> "Instagram";
            case FACEBOOK -> "Facebook";
            case TIKTOK -> "TikTok";
            case LINKEDIN -> "LinkedIn";
            case SNAPCHAT -> "Snapchat";
            case X -> "X";
            case EMAIL -> "Email";
            case SITE_WEB -> "Site web";
        };
    }
}
