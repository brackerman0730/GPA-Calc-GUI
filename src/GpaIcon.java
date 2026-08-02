import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Renders the current overall GPA as a square PNG for use as an app icon.
 *
 * <p>Written to {@code %USERPROFILE%\.deck\icons\gpa-live.png} — Deck's icon
 * folder, under a fixed name. Deck reads the file fresh every time it builds
 * its tile grid, so overwriting the same path is what makes the tile track the
 * GPA. A UUID name (what Deck's own importer generates) would break that: the
 * tile has to keep pointing at one stable filename.
 *
 * <p>Rendered flat — solid fills, no gradients — so it stays legible when Deck
 * scales it down to a 112px tile or a 32px list entry.
 */
public final class GpaIcon {

    /** Fixed output filename. Must not change: Deck's DB row points at it. */
    public static final String FILENAME = "gpa-live.png";

    private static final int SIZE = 512;
    private static final double MAX_GPA = 4.0;

    private static final Color BG        = new Color(0x14, 0x17, 0x1E);
    private static final Color TRACK     = new Color(0x2A, 0x2E, 0x38);
    private static final Color TEXT      = new Color(0xF2, 0xF5, 0xFA);
    private static final Color TEXT_DIM  = new Color(0x8B, 0x93, 0xA7);

    private GpaIcon() { }

    /** Destination path, inside Deck's icon folder. */
    public static Path outputPath() {
        return Paths.get(System.getProperty("user.home"), ".deck", "icons", FILENAME);
    }

    /**
     * Renders and writes the icon.
     *
     * @param gpa the overall GPA to display
     * @return the file written
     */
    public static Path write(final double gpa) throws IOException {
        final Path out = outputPath();
        Files.createDirectories(out.getParent());
        // Write to a temp file and move into place, so Deck can never catch a
        // half-written PNG if it happens to be starting up at the same moment.
        final Path tmp = out.resolveSibling(FILENAME + ".tmp");
        if (!ImageIO.write(render(gpa, SIZE), "png", tmp.toFile())) {
            throw new IOException("No PNG encoder available");
        }
        try {
            Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Some filesystems refuse an atomic replace while the target is
            // open; fall back to a plain overwrite rather than losing the update.
            Files.copy(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tmp);
        }
        return out;
    }

    /** Colour band for the value — makes the tile readable at a glance. */
    static Color accentFor(final double gpa) {
        if (gpa >= 3.5) return new Color(0x4A, 0xDE, 0x80);   // green
        if (gpa >= 3.0) return new Color(0x4F, 0xD1, 0xFF);   // blue
        if (gpa >= 2.0) return new Color(0xFB, 0xBF, 0x24);   // amber
        return new Color(0xF8, 0x71, 0x71);                   // red
    }

    static BufferedImage render(final double gpa, final int size) {
        final double s = size / (double) SIZE;
        final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(s, s);

        g.setColor(BG);
        g.fillRect(0, 0, SIZE, SIZE);

        final double cx = SIZE / 2.0, cy = SIZE / 2.0;
        final double radius = 176, stroke = 30;
        final double fraction = Math.max(0, Math.min(1, gpa / MAX_GPA));

        // Track, then the filled portion on top of it.
        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(TRACK);
        g.draw(new Arc2D.Double(cx - radius, cy - radius, radius * 2, radius * 2,
                0, 360, Arc2D.OPEN));

        if (fraction > 0) {
            g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(accentFor(gpa));
            // Start at 12 o'clock and sweep clockwise (negative extent).
            g.draw(new Arc2D.Double(cx - radius, cy - radius, radius * 2, radius * 2,
                    90, -360 * fraction, Arc2D.OPEN));
        }

        final String value = String.format(Locale.ROOT, "%.2f", gpa);
        drawCentered(g, value, boldFont(150), TEXT, cx, cy - 18);
        drawCentered(g, "GPA", boldFont(46), TEXT_DIM, cx, cy + 96);

        g.dispose();
        return img;
    }

    /**
     * Draws text centred on {@code (cx, cy)} using the glyphs' ink bounds
     * rather than font metrics — ascent/descent include padding for characters
     * that aren't present, which leaves digits visibly high in the circle.
     */
    private static void drawCentered(final Graphics2D g, final String text, final Font font,
                                     final Color color, final double cx, final double cy) {
        g.setFont(font);
        g.setColor(color);
        final GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);
        final Rectangle2D b = gv.getVisualBounds();
        g.drawString(text,
                (float) (cx - b.getWidth() / 2 - b.getX()),
                (float) (cy - b.getHeight() / 2 - b.getY()));
    }

    /** Prefers Segoe UI (present on Windows), falling back to the JDK default. */
    private static Font boldFont(final int size) {
        for (String family : new String[]{"Segoe UI Semibold", "Segoe UI", Font.SANS_SERIF}) {
            if (isAvailable(family)) return new Font(family, Font.BOLD, size);
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    private static boolean isAvailable(final String family) {
        if (Font.SANS_SERIF.equals(family)) return true;
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase(family)) return true;
        }
        return false;
    }
}
