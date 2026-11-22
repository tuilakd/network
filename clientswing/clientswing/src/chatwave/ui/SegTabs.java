package chatwave.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** Thanh tab dạng "viên thuốc": ô sáng trượt mượt sang tab được chọn, có số đếm đỏ. */
public class SegTabs extends JComponent {
    public interface Listener { void changed(int index); }

    private final String[] labels;
    private final int[] badges;
    private int selected;
    private float pos;
    private Anim.Tween tween;
    private Listener listener;
    private int hover = -1;

    public SegTabs(String... labels) {
        this.labels = labels;
        this.badges = new int[labels.length];
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0) select(i, true);
            }
            @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
        });
    }

    public void setListener(Listener l) { listener = l; }
    public int getSelected() { return selected; }
    public void setBadge(int i, int n) { if (badges[i] != n) { badges[i] = n; repaint(); } }

    private int indexAt(int x) {
        int w = getWidth() - 8;
        if (x < 4 || x > getWidth() - 4) return -1;
        return Math.min(labels.length - 1, (x - 4) * labels.length / Math.max(1, w));
    }

    public void select(int i, boolean notify) {
        if (i == selected) return;
        final float from = pos, to = i;
        selected = i;
        if (tween != null) tween.cancel();
        tween = Anim.run(260, new Anim.Update() {
            public void apply(float t) { pos = from + (to - from) * t; repaint(); }
        });
        if (notify && listener != null) listener.changed(i);
    }

    @Override public Dimension getPreferredSize() { return isPreferredSizeSet() ? super.getPreferredSize() : new Dimension(300, 40); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 40); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        int w = getWidth(), h = getHeight();
        g.setColor(Theme.surface2());
        g.fill(Gfx.rr(0, 0, w, h, h / 2f));
        float cw = (w - 8f) / labels.length;
        float ix = 4 + pos * cw;
        g.setColor(Theme.alpha(Theme.shadow(), 40));
        g.fill(Gfx.rr(ix, 5, cw, h - 8, (h - 8) / 2f));
        g.setColor(Theme.surface());
        g.fill(Gfx.rr(ix, 4, cw, h - 8, (h - 8) / 2f));
        g.setFont(Theme.font(java.awt.Font.BOLD, 13f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            float sel = Math.max(0f, 1f - Math.abs(pos - i));
            g.setColor(Theme.lerp(i == hover ? Theme.text() : Theme.muted(), Theme.accentDark(), sel));
            int tw = fm.stringWidth(labels[i]);
            float bx = 0;
            boolean badge = badges[i] > 0;
            float total = tw + (badge ? 26 : 0);
            float x = 4 + i * cw + (cw - total) / 2f;
            g.drawString(labels[i], x, (h - fm.getHeight()) / 2f + fm.getAscent());
            if (badge) {
                String n = badges[i] > 99 ? "99+" : String.valueOf(badges[i]);
                g.setFont(Theme.font(java.awt.Font.BOLD, 11f));
                FontMetrics f2 = g.getFontMetrics();
                int bw = Math.max(18, f2.stringWidth(n) + 10);
                g.setColor(Theme.danger());
                g.fill(Gfx.rr(x + tw + 6, h / 2f - 9, bw, 18, 9));
                g.setColor(java.awt.Color.WHITE);
                g.drawString(n, x + tw + 6 + (bw - f2.stringWidth(n)) / 2f, h / 2f - 9 + (18 - f2.getHeight()) / 2f + f2.getAscent());
                g.setFont(Theme.font(java.awt.Font.BOLD, 13f));
            }
        }
        g.dispose();
    }
}
