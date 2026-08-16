import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Crop a source image (anime illustration) into a rounded launcher icon (192x192). */
public class CropIcon {
    public static void main(String[] a) throws Exception {
        int s = 192;
        String in = a.length > 0 ? a[0] : "src_icon.jpg";
        BufferedImage src = ImageIO.read(new File(in));
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        // crop a square biased toward the top (face/head)
        int sy = 0, sx = (w - side) / 2;
        BufferedImage sq = src.getSubimage(sx, sy, side, side);

        BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(sq, 0, 0, s, s, null);
        g.dispose();
        ImageIO.write(out, "png", new File("ic_launcher.png"));
        System.out.println("icon written, src=" + w + "x" + h + " crop=" + side + "x" + side);
    }
}