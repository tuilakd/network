package chatwave.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Popup menu tự vẽ (cửa sổ bo góc 10px, đổ bóng nhẹ, padding 10px 16px, rộng tối thiểu 190px, Esc/click ngoài để đóng). */
public final class PopupMenu {
    public static final class Item {
        public final Icons icon;
        public final String label;
        public final Color color;
        public final Runnable action;
        public Item(Icons icon, String label, Color color, Runnable action) {
            this.icon = icon; this.label = label; this.color = color; this.action = action;
        }
        public Item(Icons icon, String label, Runnable action) {
            this(icon, label, null, action);
        }
    }

    private PopupMenu() {}

    public static void show(Component anchor, int x, int y, List<Item> items) {
        if (items == null || items.isEmpty()) return;
        Window owner = SwingUtilities.getWindowAncestor(anchor);
        final JWindow win = new JWindow(owner);
        win.setBackground(new Color(0, 0, 0, 0));

        final List<Item> list = new ArrayList<Item>(items);
        final int[] hover = {-1};
        final int rowH = 40;
        final int pad = 6;
        final int w = 210;
        final int h = list.size() * rowH + pad * 2 + 8;

        JComponent panel = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(w + 8, h); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                int bw = getWidth() - 8, bh = getHeight() - 8;
                Gfx.shadow(g, 4, 4, bw, bh, 10, 10, 4);
                g.setColor(Theme.surface());
                g.fill(Gfx.rr(4, 4, bw, bh, 10));
                g.setColor(Theme.border());
                g.draw(Gfx.rr(4, 4, bw, bh, 10));

                g.setFont(Theme.font(Font.PLAIN, 13.5f));
                FontMetrics fm = g.getFontMetrics();
                for (int i = 0; i < list.size(); i++) {
                    Item it = list.get(i);
                    int ry = 4 + pad + i * rowH;
                    if (i == hover[0]) {
                        g.setColor(Theme.accentSoft());
                        g.fill(Gfx.rr(8, ry, bw - 8, rowH, 8));
                    }
                    Color fg = it.color != null ? it.color : Theme.text();
                    if (it.icon != null) {
                        it.icon.paint(g, 20, ry + (rowH - 18) / 2f, 18, fg);
                    }
                    g.setColor(fg);
                    int tx = it.icon != null ? 20 + 18 + 10 : 20;
                    g.drawString(it.label, tx, ry + (rowH - fm.getHeight()) / 2f + fm.getAscent());
                }
                g.dispose();
            }
        };

        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = (e.getY() - 4 - pad) / rowH;
                if (e.getY() < 4 + pad || idx < 0 || idx >= list.size()) idx = -1;
                if (idx != hover[0]) { hover[0] = idx; panel.repaint(); }
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hover[0] = -1; panel.repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                int idx = (e.getY() - 4 - pad) / rowH;
                win.dispose();
                if (idx >= 0 && idx < list.size() && list.get(idx).action != null) {
                    list.get(idx).action.run();
                }
            }
        });

        final AWTEventListener[] outside = {null};
        outside[0] = new AWTEventListener() {
            public void eventDispatched(AWTEvent ev) {
                if (ev instanceof MouseEvent && ev.getID() == MouseEvent.MOUSE_PRESSED) {
                    MouseEvent me = (MouseEvent) ev;
                    Component src = me.getComponent();
                    if (src != null && !SwingUtilities.isDescendingFrom(src, win)) {
                        win.dispose();
                    }
                } else if (ev instanceof KeyEvent && ev.getID() == KeyEvent.KEY_PRESSED) {
                    if (((KeyEvent) ev).getKeyCode() == KeyEvent.VK_ESCAPE) {
                        win.dispose();
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outside[0], AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        win.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(outside[0]);
            }
        });

        win.setContentPane(panel);
        win.pack();
        Point loc = anchor.getLocationOnScreen();
        win.setLocation(loc.x + x, loc.y + y);
        win.setVisible(true);
    }
}
