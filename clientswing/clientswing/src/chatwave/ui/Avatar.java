package chatwave.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.geom.Ellipse2D;

/** Vẽ ảnh đại diện hình tròn từ chữ cái đầu của tên. */
public final class Avatar {
    private Avatar() {}

    public static String initials(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) return "?";
        String[] p = n.split("[\\s_\\.]+");
        String s = p[0].substring(0, 1);
        if (p.length > 1) s += p[p.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    public static void paint(Graphics2D g0, String name, float x, float y, float size, boolean online, boolean group) {
        Graphics2D g = (Graphics2D) g0.create();
        Color c = Theme.avatarColor(name == null ? "" : name);
        g.setPaint(new GradientPaint(x, y, c.brighter(), x + size, y + size, c.darker()));
        g.fill(new Ellipse2D.Float(x, y, size, size));
        g.setColor(Color.WHITE);
        if (group) {
            Icons.GROUP.paint(g, x + size * 0.24f, y + size * 0.24f, size * 0.52f, Color.WHITE);
        } else {
            g.setFont(Theme.font(java.awt.Font.BOLD, size * 0.38f));
            FontMetrics fm = g.getFontMetrics();
            String s = initials(name);
            g.drawString(s, x + (size - fm.stringWidth(s)) / 2f, y + (size - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (online) {
            float d = Math.max(8, size * 0.24f);
            g.setColor(Theme.surface());
            g.fill(new Ellipse2D.Float(x + size - d - 1, y + size - d - 1, d + 3, d + 3));
            g.setColor(Theme.ok());
            g.fill(new Ellipse2D.Float(x + size - d, y + size - d, d, d));
        }
        g.dispose();
    }
}
