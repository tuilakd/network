package chatwave.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Thanh cuộn mỏng, bo tròn, chỉ đậm lên khi rê chuột (như Windows 11). */
public final class ModernScroll {
    private ModernScroll() {}

    public static JScrollPane wrap(java.awt.Component c) {
        JScrollPane sp = new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        JScrollBar vsb = new JScrollBar(JScrollBar.VERTICAL) {
            @Override public void updateUI() {
                setUI(new Ui());
            }
        };
        vsb.setUnitIncrement(24);
        vsb.setPreferredSize(new Dimension(10, 10));
        vsb.setOpaque(false);
        sp.setVerticalScrollBar(vsb);
        return sp;
    }

    private static final class Ui extends BasicScrollBarUI {
        private boolean over;

        @Override protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { over = true; scrollbar.repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { over = false; scrollbar.repaint(); }
            });
        }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = Gfx.g2(g0);
            int w = over || isDragging ? 7 : 4;
            g.setColor(Theme.alpha(Theme.muted(), over || isDragging ? 170 : 90));
            g.fill(Gfx.rr(r.x + (r.width - w) / 2f - 1, r.y + 2, w, r.height - 4, w / 2f));
            g.dispose();
        }
        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(10, 10); }
    }
}