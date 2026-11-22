package chatwave.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;
import javax.swing.Timer;

/** Nút kiểu Fluent: bo góc, đổi màu mượt khi rê chuột, nhún khi bấm, có vòng xoay khi đang xử lý. */
public class FButton extends JComponent {
    public enum Kind { PRIMARY, SOFT, SUBTLE, GHOST, DANGER, ICON }

    private String text;
    private Icons icon;
    private final Kind kind;
    private Runnable action;
    private boolean loading, pressed;
    private final Anim.Fader hover = new Anim.Fader(this);
    private final Anim.Fader focus = new Anim.Fader(this);
    private Timer spin;
    private float angle;
    private float radius = 10f;
    private int iconSize = 18;
    private Color customFg;

    public FButton(String text, Icons icon, Kind kind, Runnable action) {
        this.text = text;
        this.icon = icon;
        this.kind = kind;
        this.action = action;
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) hover.to(1f, 140); }
            @Override public void mouseExited(MouseEvent e) { hover.to(0f, 200); pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { if (isEnabled()) { pressed = true; repaint(); } }
            @Override public void mouseReleased(MouseEvent e) {
                boolean fire = pressed && contains(e.getPoint());
                pressed = false;
                repaint();
                if (fire) fire();
            }
        };
        addMouseListener(m);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { focus.to(1f, 120); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { focus.to(0f, 160); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) fire();
            }
        });
    }

    public FButton(String text, Kind kind, Runnable action) { this(text, null, kind, action); }
    public static FButton icon(Icons ic, final String tip, Runnable action) {
        FButton b = new FButton("", ic, Kind.ICON, action);
        b.setToolTipText(tip);
        return b;
    }

    private void fire() { if (isEnabled() && !loading && action != null) action.run(); }

    public void setAction(Runnable r) { action = r; }
    public void setText(String t) { text = t; revalidate(); repaint(); }
    public void setIcon(Icons i) { icon = i; repaint(); }
    public void setRadius(float r) { radius = r; }
    public void setIconSize(int s) { iconSize = s; }
    public void setForegroundColor(Color c) { customFg = c; }

    public void setLoading(boolean on) {
        loading = on;
        if (on && spin == null) {
            spin = new Timer(16, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) { angle = (angle + 9) % 360; repaint(); }
            });
            spin.start();
        } else if (!on && spin != null) { spin.stop(); spin = null; }
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        if (kind == Kind.ICON) return new Dimension(38, 38);
        FontMetrics fm = getFontMetrics(Theme.bold());
        int w = fm.stringWidth(text == null ? "" : text) + 36 + (icon != null ? 26 : 0);
        return new Dimension(Math.max(w, 80), 40);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        int w = getWidth(), h = getHeight();
        float hv = hover.v;
        Color fill, fg;
        boolean border = false;
        switch (kind) {
            case PRIMARY: fill = Theme.lerp(Theme.accent(), Theme.accentDark(), hv); fg = Theme.onAccent(); break;
            case SOFT:    fill = Theme.lerp(Theme.accentSoft(), Theme.lerp(Theme.accentSoft(), Theme.accent(), .22f), hv); fg = Theme.accentDark(); break;
            case DANGER:  fill = Theme.lerp(Theme.alpha(Theme.danger(), 28), Theme.alpha(Theme.danger(), 60), hv); fg = Theme.danger(); break;
            case GHOST:   fill = Theme.lerp(Theme.alpha(Theme.surface(), 0), Theme.hover(), hv); fg = Theme.text(); border = true; break;
            case ICON:    fill = Theme.lerp(Theme.alpha(Theme.hover(), 0), Theme.hover(), hv); fg = Theme.lerp(Theme.muted(), Theme.text(), hv); break;
            default:      fill = Theme.lerp(Theme.alpha(Theme.hover(), 0), Theme.hover(), hv); fg = Theme.text();
        }
        if (customFg != null) fg = customFg;
        if (!isEnabled()) { fill = Theme.alpha(fill, fill.getAlpha() / 2); fg = Theme.alpha(fg, 110); }
        float s = pressed ? 0.965f : 1f;
        g.translate(w / 2.0, h / 2.0);
        g.scale(s, s);
        g.translate(-w / 2.0, -h / 2.0);
        float r = kind == Kind.ICON ? Math.min(w, h) / 2f : radius;
        g.setColor(fill);
        g.fill(Gfx.rr(0, 0, w, h, r));
        if (border) { g.setColor(Theme.border()); g.draw(Gfx.rr(0.5f, 0.5f, w - 1, h - 1, r)); }
        if (focus.v > 0.01f && isFocusOwner()) {
            g.setColor(Theme.alpha(Theme.accent(), (int) (160 * focus.v)));
            g.setStroke(new java.awt.BasicStroke(2f));
            g.draw(Gfx.rr(1, 1, w - 2, h - 2, r));
        }
        g.setFont(Theme.bold());
        FontMetrics fm = g.getFontMetrics();
        String t = text == null ? "" : text;
        int tw = fm.stringWidth(t);
        int iw = icon != null ? iconSize + (t.isEmpty() ? 0 : 8) : 0;
        float x = (w - tw - iw) / 2f;
        if (loading) {
            g.setColor(fg);
            g.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Float(w / 2f - 9, h / 2f - 9, 18, 18, -angle, 260, Arc2D.OPEN));
        } else {
            if (icon != null) { icon.paint(g, x, (h - iconSize) / 2f, iconSize, fg); x += iw; }
            if (!t.isEmpty()) { g.setColor(fg); g.drawString(t, x, (h - fm.getHeight()) / 2f + fm.getAscent()); }
        }
        g.dispose();
    }
}
