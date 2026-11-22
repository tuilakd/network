package chatwave.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/** Logo ChatWave: ô vuông bo tròn gradient với bong bóng chat. */
public class BrandLogo extends JComponent {
    private final int size;
    public BrandLogo(int size) { this.size = size; setOpaque(false); }
    @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    public static void paintLogo(Graphics2D g, float x, float y, float s) {
        Color a = new Color(0x4DC3BA), b = new Color(0x2A8F9E);
        g.setPaint(new GradientPaint(x, y, a, x + s, y + s, b));
        g.fill(Gfx.rr(x, y, s, s, s * 0.3f));
        Icons.CHAT.paint(g, x + s * 0.2f, y + s * 0.2f, s * 0.6f, Color.WHITE);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        paintLogo(g, 0, 0, size);
        g.dispose();
    }

    public static BufferedImage image(int s) {
        BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Gfx.g2(im.getGraphics());
        paintLogo(g, 0, 0, s);
        g.dispose();
        return im;
    }
}
