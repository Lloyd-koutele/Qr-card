package made.qrcard.carte.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Vignette JPEG pour la vCard : un carnet de contacts n'affiche qu'une petite image,
 * et une photo de 2 Mo alourdirait le fichier au point que certaines applications le refusent.
 */
final class PhotoVcard
{

    private static final int COTE_MAX = 400;
    private static final float QUALITE = 0.85f;

    private PhotoVcard()
    {
    }

    /** Vide si l'image ne peut pas etre lue (WebP par exemple) : la vCard part alors sans photo. */
    static byte[] vignette(byte[] donnees)
    {
        try
        {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(donnees));
            if (source == null) return null;
            double echelle = Math.min(1.0, (double) COTE_MAX / Math.max(source.getWidth(), source.getHeight()));
            int largeur = Math.max(1, (int) Math.round(source.getWidth() * echelle));
            int hauteur = Math.max(1, (int) Math.round(source.getHeight() * echelle));
            // Fond blanc : le JPEG n'a pas de transparence
            BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, largeur, hauteur);
            g.drawImage(source, 0, 0, largeur, hauteur, null);
            g.dispose();
            return jpeg(image);
        }
        catch (IOException | RuntimeException e)
        {
            return null;
        }
    }

    private static byte[] jpeg(BufferedImage image) throws IOException
    {
        ImageWriter ecrivain = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ImageOutputStream flux = ImageIO.createImageOutputStream(sortie))
        {
            ecrivain.setOutput(flux);
            ImageWriteParam parametres = ecrivain.getDefaultWriteParam();
            parametres.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametres.setCompressionQuality(QUALITE);
            ecrivain.write(null, new IIOImage(image, null, null), parametres);
        }
        finally
        {
            ecrivain.dispose();
        }
        return sortie.toByteArray();
    }
}
