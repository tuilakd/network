package chatwave.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Hộp thoại dùng chung (xác nhận, nhập tên, xem ảnh) hiện giữa màn hình qua Overlay. */
public final class Dialogs {
    private Dialogs() {}

    public interface TextOk { void accept(String text); }

    /** Nhãn chữ tự vẽ, tự xuống dòng theo chiều rộng. */
    public static final class Label extends JComponent {
        private final String text; private final java.awt.Font font; private final boolean muted; private final boolean center; private final int width;
        public Label(String text, java.awt.Font font, boolean muted, boolean center, int width) {
            this.text = text; this.font = font; this.muted = muted; this.center = center; this.width = width;
            setOpaque(false);
        }
        private java.util.List<String> lines() {
            java.util.List<String> out = new java.util.ArrayList<String>();
            java.awt.FontMetrics fm = getFontMetrics(font);
            StringBuilder line = new StringBuilder();
            for (String w : text.split(" ")) {
                String t = line.length() == 0 ? w : line + " " + w;
                if (fm.stringWidth(t) > width && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(w); }
                else line = new StringBuilder(t);
            }
            out.add(line.toString());
            return out;
        }
        @Override public Dimension getPreferredSize() {
            java.awt.FontMetrics fm = getFontMetrics(font);
            return new Dimension(width, lines().size() * (fm.getHeight() + 2));
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.g2(g0);
            g.setFont(font);
            g.setColor(muted ? Theme.muted() : Theme.text());
            java.awt.FontMetrics fm = g.getFontMetrics();
            int y = fm.getAscent();
            for (String l : lines()) {
                g.drawString(l, center ? (getWidth() - fm.stringWidth(l)) / 2 : 0, y);
                y += fm.getHeight() + 2;
            }
            g.dispose();
        }
    }

    private static JPanel buttons(FButton... b) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        for (FButton x : b) p.add(x);
        p.setAlignmentX(0f);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return p;
    }

    public static void confirm(final Overlay ov, String title, String message, String okText, boolean danger, final Runnable onOk) {
        Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        Label t = new Label(title, Theme.font(java.awt.Font.BOLD, 18f), false, false, 340);
        Label m = new Label(message, Theme.font(java.awt.Font.PLAIN, 14f), true, false, 340);
        t.setAlignmentX(0f); m.setAlignmentX(0f);
        c.add(t);
        c.add(javax.swing.Box.createVerticalStrut(8));
        c.add(m);
        c.add(javax.swing.Box.createVerticalStrut(22));
        FButton no = new FButton("Ở lại", FButton.Kind.GHOST, new Runnable() { public void run() { ov.close(); } });
        FButton yes = new FButton(okText, danger ? FButton.Kind.DANGER : FButton.Kind.PRIMARY, new Runnable() {
            public void run() { ov.close(); if (onOk != null) onOk.run(); }
        });
        c.add(buttons(no, yes));
        ov.show(c);
    }

    public static void prompt(final Overlay ov, String title, String hint, String okText, final TextOk ok) {
        Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        Label t = new Label(title, Theme.font(java.awt.Font.BOLD, 18f), false, false, 340);
        t.setAlignmentX(0f);
        final FField f = new FField(hint, null, false);
        f.setAlignmentX(0f);
        c.add(t);
        c.add(javax.swing.Box.createVerticalStrut(14));
        c.add(f);
        c.add(javax.swing.Box.createVerticalStrut(20));
        final Runnable go = new Runnable() {
            public void run() {
                String s = f.getText().trim();
                if (s.isEmpty()) { f.flagError(); return; }
                ov.close();
                ok.accept(s);
            }
        };
        f.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { go.run(); } });
        c.add(buttons(new FButton("Hủy", FButton.Kind.GHOST, new Runnable() { public void run() { ov.close(); } }),
                new FButton(okText, FButton.Kind.PRIMARY, go)));
        ov.show(c);
        javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { f.requestFocus(); } });
    }

    /** Xem ảnh lớn; bấm đâu cũng đóng. */
    public static void image(final Overlay ov, final BufferedImage img) {
        if (img == null) return;
        int maxW = Math.max(300, ov.getWidth() - 140), maxH = Math.max(200, ov.getHeight() - 140);
        final BufferedImage fit = ImageCache.fit(img, maxW, maxH);
        JComponent v = new JComponent() {
            { setOpaque(false); setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)); }
            @Override public Dimension getPreferredSize() { return new Dimension(fit.getWidth() + 28, fit.getHeight() + 28); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                Gfx.shadow(g, 14, 14, fit.getWidth(), fit.getHeight(), 14, 14, 6);
                g.setClip(Gfx.rr(14, 14, fit.getWidth(), fit.getHeight(), 14));
                g.drawImage(fit, 14, 14, null);
                g.dispose();
            }
        };
        v.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { ov.close(); } });
        ov.show(v);
    }
}
