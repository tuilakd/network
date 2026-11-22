package chatwave.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Khung nền bo góc có bóng đổ (dùng cho thẻ đăng nhập, hộp thoại...). */
public class Card extends JPanel {
    private final float radius;
    private final boolean shadow;
    private boolean sheet;

    public Card(int pad, float radius, boolean shadow) {
        super();
        this.radius = radius;
        this.shadow = shadow;
        setOpaque(false);
        int m = shadow ? 14 : 0;
        setBorder(new EmptyBorder(pad + m, pad + m, pad + m, pad + m));
    }

    public Card(int pad) { this(pad, 18f, true); }

    /** true = nền "side" thay vì "surface" (cột bên). */
    public Card sheet(boolean s) { sheet = s; return this; }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        int m = shadow ? 14 : 0;
        float w = getWidth() - m * 2, h = getHeight() - m * 2;
        if (shadow) Gfx.shadow(g, m, m, w, h, radius, 12, 4);
        g.setColor(sheet ? Theme.side() : Theme.surface());
        g.fill(Gfx.rr(m, m, w, h, radius));
        g.setColor(Theme.border());
        g.draw(Gfx.rr(m + 0.5f, m + 0.5f, w - 1, h - 1, radius));
        g.dispose();
    }

    public Insets cardInsets() { return getInsets(); }
}
