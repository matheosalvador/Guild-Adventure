import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/** Compare deux dossiers de PNG ; ignore le HUD texte en haut a gauche (FPS variable). */
public class Compare {
    public static void main(String[] a) throws Exception {
        File[] fa = new File(a[0]).listFiles((d, n) -> n.endsWith(".png"));
        Arrays.sort(fa);
        int diffFiles = 0, missing = 0;
        for (File f : fa) {
            File g = new File(a[1], f.getName());
            if (!g.exists()) { System.out.println("MANQUANT  " + f.getName()); missing++; continue; }
            BufferedImage x = ImageIO.read(f), y = ImageIO.read(g);
            if (x.getWidth() != y.getWidth() || x.getHeight() != y.getHeight()) { System.out.println("TAILLE    " + f.getName()); diffFiles++; continue; }
            long n = 0; int minX = 9999, minY = 9999, maxX = -1, maxY = -1;
            for (int py = 0; py < x.getHeight(); py++) {
                for (int px = 0; px < x.getWidth(); px++) {
                    if (px < 330 && py < 110) continue; // HUD (FPS...)
                    if (x.getRGB(px, py) != y.getRGB(px, py)) {
                        n++; minX = Math.min(minX, px); minY = Math.min(minY, py); maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
                    }
                }
            }
            if (n == 0) System.out.println("identique " + f.getName());
            else { diffFiles++; System.out.println("DIFFERENT " + f.getName() + " : " + n + " px, zone [" + minX + "," + minY + " -> " + maxX + "," + maxY + "]"); }
        }
        File[] fb = new File(a[1]).listFiles((d, n) -> n.endsWith(".png"));
        for (File f : fb) if (!new File(a[0], f.getName()).exists()) { System.out.println("EN PLUS   " + f.getName()); missing++; }
        System.out.println(diffFiles == 0 && missing == 0 ? "\nRESULTAT: IDENTIQUE (" + fa.length + " captures)" : "\nRESULTAT: " + diffFiles + " difference(s), " + missing + " manquant(s)/en plus");
    }
}
