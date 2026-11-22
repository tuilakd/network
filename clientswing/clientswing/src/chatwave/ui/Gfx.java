package chatwave.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/** Vài hàm vẽ dùng chung. */
public final class Gfx {
    private Gfx() {}

    public static Graphics2D g2(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    public static RoundRectangle2D rr(float x, float y, float w, float h, float r) {
        return new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2);
    }

    /** Bóng đổ mềm quanh hình chữ nhật bo góc. */
    public static void shadow(Graphics2D g, float x, float y, float w, float h, float r, int spread, int dy) {
        for (int i = spread; i >= 1; i--) {
            float a = (float) (spread - i + 1) / spread;
            Color c = Theme.alpha(Theme.shadow(), (int) (Theme.shadow().getAlpha() * a * 0.16f));
            g.setColor(c);
            g.fill(rr(x - i, y - i + dy, w + i * 2, h + i * 2, r + i));
        }
    }

    public static void text(Graphics2D g, String s, float x, float y, Color c) {
        g.setColor(c);
        g.drawString(s, x, y);
    }

    public static String ellipsize(String s, java.awt.FontMetrics fm, int w) {
        if (fm.stringWidth(s) <= w) return s;
        String dots = "…";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n) + dots) > w) n--;
        return s.substring(0, Math.max(0, n)) + dots;
    }

    public static void clip(Graphics2D g, Shape s) { g.clip(s); }
}
