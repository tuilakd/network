package chatwave.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

/** Popup chọn emoji màu (Twemoji PNG 24x24, lưới 8 cột 36x36, bo góc 12px, tooltip bo góc, mục Gần đây). */
public final class EmojiPicker {
    public interface OnPick { void picked(String unicode); }

    private static final int MAX_RECENT = 16;
    private static final String PREF_KEY = "recent_emoji_hexes";
    private static final Preferences PREFS = Preferences.userNodeForPackage(EmojiPicker.class);
    private static final List<String> RECENT_HEX = loadRecent();

    private EmojiPicker() {}

    private static List<String> loadRecent() {
        List<String> list = new ArrayList<String>();
        String raw = PREFS.get(PREF_KEY, "");
        if (!raw.isEmpty()) {
            for (String s : raw.split(",")) {
                if (!s.isEmpty() && EmojiRegistry.hasHex(s) && !list.contains(s)) list.add(s);
            }
        }
        if (list.isEmpty()) {
            Collections.addAll(list, "1f600", "1f602", "2764", "1f44d", "1f64f", "1f389", "1f525", "1f60e");
        }
        return list;
    }

    private static void addRecent(String hex) {
        RECENT_HEX.remove(hex);
        RECENT_HEX.add(0, hex);
        while (RECENT_HEX.size() > MAX_RECENT) RECENT_HEX.remove(RECENT_HEX.size() - 1);
        PREFS.put(PREF_KEY, String.join(",", RECENT_HEX));
    }

    public static void show(Component anchor, final OnPick onPick) {
        Window owner = SwingUtilities.getWindowAncestor(anchor);
        final JWindow win = new JWindow(owner);
        win.setBackground(new Color(0, 0, 0, 0));

        final int[] activeTab = {1};
        final int[] hoverTab = {-1};
        final int[] hoverCell = {-1};
        final String[] tipText = {""};

        final JPanel root = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                int w = getWidth() - 8, h = getHeight() - 8;
                Gfx.shadow(g, 4, 4, w, h, 12, 12, 4);
                g.setColor(Theme.surface());
                g.fill(Gfx.rr(4, 4, w, h, 12));
                g.setColor(Theme.border());
                g.draw(Gfx.rr(4, 4, w, h, 12));
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.setPreferredSize(new Dimension(328, 294));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        final List<EmojiRegistry.Entry> currentEntries = new ArrayList<EmojiRegistry.Entry>();

        final JComponent grid = new JComponent() {
            @Override public Dimension getPreferredSize() {
                int rows = Math.max(1, (currentEntries.size() + 7) / 8);
                return new Dimension(292, rows * 36 + 4);
            }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                for (int i = 0; i < currentEntries.size(); i++) {
                    int col = i % 8, row = i / 8;
                    int x = col * 36 + 2, y = row * 36 + 2;
                    if (i == hoverCell[0]) {
                        g.setColor(Theme.accentSoft());
                        g.fill(Gfx.rr(x, y, 34, 34, 8));
                    }
                    EmojiRegistry.Entry e = currentEntries.get(i);
                    BufferedImage img = EmojiRegistry.image(e.hex, 24, new Runnable() {
                        public void run() { repaint(); }
                    });
                    if (img != null) {
                        g.drawImage(img, x + 5, y + 5, 24, 24, null);
                    }
                }
                g.dispose();
            }
        };
        grid.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final JComponent tabBar = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(296, 34); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                int cw = getWidth() / EmojiRegistry.TAB_HEX.length;
                for (int i = 0; i < EmojiRegistry.TAB_HEX.length; i++) {
                    int x = i * cw;
                    if (i == activeTab[0] || i == hoverTab[0]) {
                        g.setColor(i == activeTab[0] ? Theme.accentSoft() : Theme.hover());
                        g.fill(Gfx.rr(x + 2, 2, cw - 4, 30, 8));
                    }
                    BufferedImage img = EmojiRegistry.image(EmojiRegistry.TAB_HEX[i], 18, new Runnable() {
                        public void run() { repaint(); }
                    });
                    if (img != null) {
                        g.drawImage(img, x + (cw - 18) / 2, 8, 18, 18, null);
                    }
                }
                g.dispose();
            }
        };
        tabBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final JComponent footer = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(296, 20); }
            @Override protected void paintComponent(Graphics g0) {
                if (tipText[0].isEmpty()) return;
                Graphics2D g = Gfx.g2(g0);
                g.setFont(Theme.font(Font.PLAIN, 11.5f));
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(tipText[0]) + 16;
                g.setColor(Theme.surface2());
                g.fill(Gfx.rr(2, 1, tw, 18, 8));
                g.setColor(Theme.muted());
                g.drawString(tipText[0], 10, 1 + (18 - fm.getHeight()) / 2f + fm.getAscent());
                g.dispose();
            }
        };

        final Runnable reload = new Runnable() {
            public void run() {
                currentEntries.clear();
                if (activeTab[0] == 0) {
                    for (String hx : RECENT_HEX) {
                        EmojiRegistry.Entry e = EmojiRegistry.byHex(hx);
                        if (e != null) currentEntries.add(e);
                    }
                } else {
                    currentEntries.addAll(EmojiRegistry.byCategory(activeTab[0]));
                }
                hoverCell[0] = -1;
                grid.revalidate();
                grid.repaint();
                tabBar.repaint();
            }
        };

        tabBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int cw = Math.max(1, tabBar.getWidth() / EmojiRegistry.TAB_HEX.length);
                int idx = Math.min(EmojiRegistry.TAB_HEX.length - 1, Math.max(0, e.getX() / cw));
                if (idx != hoverTab[0]) {
                    hoverTab[0] = idx;
                    tipText[0] = EmojiRegistry.TAB_NAMES[idx];
                    tabBar.repaint();
                    footer.repaint();
                }
            }
        });
        tabBar.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoverTab[0] = -1; tipText[0] = ""; tabBar.repaint(); footer.repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                int cw = Math.max(1, tabBar.getWidth() / EmojiRegistry.TAB_HEX.length);
                int idx = Math.min(EmojiRegistry.TAB_HEX.length - 1, Math.max(0, e.getX() / cw));
                activeTab[0] = idx;
                reload.run();
            }
        });

        grid.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int col = (e.getX() - 2) / 36, row = (e.getY() - 2) / 36;
                int idx = (col >= 0 && col < 8 && row >= 0) ? row * 8 + col : -1;
                if (idx >= currentEntries.size()) idx = -1;
                if (idx != hoverCell[0]) {
                    hoverCell[0] = idx;
                    tipText[0] = idx >= 0 ? currentEntries.get(idx).name : "";
                    grid.repaint();
                    footer.repaint();
                }
            }
        });
        grid.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoverCell[0] = -1; tipText[0] = ""; grid.repaint(); footer.repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                int col = (e.getX() - 2) / 36, row = (e.getY() - 2) / 36;
                int idx = (col >= 0 && col < 8 && row >= 0) ? row * 8 + col : -1;
                if (idx >= 0 && idx < currentEntries.size()) {
                    EmojiRegistry.Entry en = currentEntries.get(idx);
                    addRecent(en.hex);
                    if (onPick != null) onPick.picked(en.unicode);
                    if (activeTab[0] == 0) reload.run();
                }
            }
        });

        reload.run();
        JScrollPane scroll = ModernScroll.wrap(grid);
        scroll.getViewport().setBackground(Theme.surface());
        root.add(tabBar, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        final AWTEventListener[] outside = {null};
        outside[0] = new AWTEventListener() {
            public void eventDispatched(AWTEvent ev) {
                if (ev instanceof MouseEvent && ev.getID() == MouseEvent.MOUSE_PRESSED) {
                    Component src = ((MouseEvent) ev).getComponent();
                    if (src != null && !SwingUtilities.isDescendingFrom(src, win) && src != anchor) {
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

        win.setContentPane(root);
        win.pack();
        Point loc = anchor.getLocationOnScreen();
        win.setLocation(Math.max(8, loc.x - 10), Math.max(8, loc.y - win.getHeight() - 6));
        win.setVisible(true);
    }
}
