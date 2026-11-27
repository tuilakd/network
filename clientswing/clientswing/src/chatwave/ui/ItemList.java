package chatwave.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Scrollable;

/** Danh sách dạng thẻ (bạn bè, lời mời, phòng...) tự vẽ: rê chuột sáng lên, mục đang chọn có thanh nhấn, hiện lần lượt khi đổi tab. */
public class ItemList extends JComponent implements Scrollable {
    public static class Btn {
        public final String label; public final FButton.Kind kind; public final Runnable action;
        Rectangle r = new Rectangle();
        public Btn(String label, FButton.Kind kind, Runnable action) { this.label = label; this.kind = kind; this.action = action; }
    }

    public static class Row {
        public String id = "", title = "", sub = "", time = "";
        public int badge;
        public boolean group, online, header;
        public Btn[] btns;
        public Object tag;
        public static Row header(String t) { Row r = new Row(); r.header = true; r.title = t; return r; }
    }

    public interface Listener { void clicked(Row row); }

    private List<Row> rows = new ArrayList<Row>();
    private int hover = -1;
    private int hoverBtn = -1;
    private String selectedId;
    private Listener listener;
    private String empty = "";
    private Btn emptyBtn;
    private boolean hoverEmptyBtn;
    private float intro = 1f;
    private static final int ROW_H = 66, ROW_H_BTN = 74, HEAD_H = 34;

    public ItemList() {
        setOpaque(false);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e); }
            @Override public void mouseExited(MouseEvent e) { hover = -1; hoverBtn = -1; setCursor(Cursor.getDefaultCursor()); repaint(); }
            @Override public void mousePressed(MouseEvent e) { click(e); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setListener(Listener l) { listener = l; }
    public void setEmptyText(String s) { empty = s; repaint(); }
    public void setEmptyButton(Btn b) { emptyBtn = b; hoverEmptyBtn = false; repaint(); }
    public void setSelectedId(String id) { selectedId = id; repaint(); }
    public String getSelectedId() { return selectedId; }

    public void setRows(List<Row> r, boolean animate) {
        rows = r;
        hover = -1;
        revalidate();
        repaint();
        if (animate && !r.isEmpty()) {
            Anim.run(420 + Math.min(r.size(), 8) * 40, new Anim.Update() {
                public void apply(float t) { intro = t; repaint(); }
            });
        } else intro = 1f;
    }

    private int rowH(Row r) { return r.header ? HEAD_H : r.btns != null ? ROW_H_BTN : ROW_H; }

    private int rowAt(int y) {
        int top = 6;
        for (int i = 0; i < rows.size(); i++) {
            int h = rowH(rows.get(i));
            if (y >= top && y < top + h) return i;
            top += h;
        }
        return -1;
    }

    private int topOf(int idx) {
        int top = 6;
        for (int i = 0; i < idx; i++) top += rowH(rows.get(i));
        return top;
    }

    private void updateHover(MouseEvent e) {
        if (rows.isEmpty()) {
            boolean heb = emptyBtn != null && emptyBtn.r.contains(e.getPoint());
            if (heb != hoverEmptyBtn) {
                hoverEmptyBtn = heb;
                setCursor(heb ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
            return;
        }
        int i = rowAt(e.getY());
        int hb = -1;
        if (i >= 0 && !rows.get(i).header) {
            Row r = rows.get(i);
            if (r.btns != null) for (int b = 0; b < r.btns.length; b++) if (r.btns[b].r.contains(e.getPoint())) hb = b;
        }
        if (i >= 0 && rows.get(i).header) i = -1;
        if (i != hover || hb != hoverBtn) {
            hover = i;
            hoverBtn = hb;
            setCursor(i >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }
    }

    private void click(MouseEvent e) {
        if (rows.isEmpty()) {
            if (emptyBtn != null && emptyBtn.r.contains(e.getPoint()) && emptyBtn.action != null) {
                emptyBtn.action.run();
            }
            return;
        }
        int i = rowAt(e.getY());
        if (i < 0) return;
        Row r = rows.get(i);
        if (r.header) return;
        if (r.btns != null) {
            for (Btn b : r.btns) if (b.r.contains(e.getPoint())) { if (b.action != null) b.action.run(); return; }
        }
        if (listener != null) listener.clicked(r);
    }

    @Override public Dimension getPreferredSize() {
        int h = 12;
        for (Row r : rows) h += rowH(r);
        return new Dimension(280, Math.max(h, 40));
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        int w = getWidth();
        if (rows.isEmpty()) {
            g.setFont(Theme.font(java.awt.Font.PLAIN, 13.5f));
            g.setColor(Theme.muted());
            FontMetrics fm = g.getFontMetrics();
            String[] lines = empty.split("\n");
            int y = 60;
            for (String l : lines) { g.drawString(l, (w - fm.stringWidth(l)) / 2, y); y += fm.getHeight() + 2; }
            if (emptyBtn != null) {
                g.setFont(Theme.font(java.awt.Font.BOLD, 13f));
                FontMetrics bfm = g.getFontMetrics();
                int bw = bfm.stringWidth(emptyBtn.label) + 32, bh = 36;
                int bx = (w - bw) / 2, by = y + 12;
                emptyBtn.r.setBounds(bx, by, bw, bh);
                g.setColor(hoverEmptyBtn ? Theme.accentDark() : Theme.accent());
                g.fill(Gfx.rr(bx, by, bw, bh, 10));
                g.setColor(Theme.onAccent());
                g.drawString(emptyBtn.label, bx + (bw - bfm.stringWidth(emptyBtn.label)) / 2f,
                        by + (bh - bfm.getHeight()) / 2f + bfm.getAscent());
            }
            g.dispose();
            return;
        }
        int top = 6;
        int n = rows.size();
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            int h = rowH(r);
            float p = Math.max(0f, Math.min(1f, intro * (n + 3) / 1.6f - i * 0.5f));
            p = Math.max(0f, Math.min(1f, p));
            g.setComposite(AlphaComposite.SrcOver.derive(intro >= 1f ? 1f : p));
            int off = intro >= 1f ? 0 : (int) ((1 - p) * 14);
            if (r.header) {
                g.setFont(Theme.font(java.awt.Font.BOLD, 11.5f));
                g.setColor(Theme.muted());
                g.drawString(r.title.toUpperCase(), 20, top + 22 + off);
            } else paintRow(g, r, i, 8, top + off, w - 16, h - 4);
            top += h;
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.dispose();
    }

    private void paintRow(Graphics2D g, Row r, int i, int x, int y, int w, int h) {
        boolean sel = r.id.equals(selectedId) && !r.id.isEmpty();
        boolean hov = i == hover;
        if (sel) {
            g.setColor(Theme.accentSoft());
            g.fill(Gfx.rr(x, y, w, h, 12));
            g.setColor(Theme.accent());
            g.fill(Gfx.rr(x, y + h / 2f - 11, 3.5f, 22, 2));
        } else if (hov) {
            g.setColor(Theme.hover());
            g.fill(Gfx.rr(x, y, w, h, 12));
        }
        float av = 44;
        Avatar.paint(g, r.title, x + 12, y + (h - av) / 2f, av, r.online, r.group);
        int tx = x + 12 + (int) av + 12;
        int right = x + w - 12;
        boolean hasBtns = r.btns != null;
        int btnW = 0;
        if (hasBtns) {
            g.setFont(Theme.font(java.awt.Font.BOLD, 12.5f));
            FontMetrics fm = g.getFontMetrics();
            int bx = right;
            for (int b = r.btns.length - 1; b >= 0; b--) {
                Btn bt = r.btns[b];
                int bw = fm.stringWidth(bt.label) + 24, bh = 30;
                bx -= bw;
                bt.r.setBounds(bx, y + (h - bh) / 2, bw, bh);
                boolean hb = i == hover && b == hoverBtn;
                java.awt.Color fill, fg;
                if (bt.kind == FButton.Kind.PRIMARY) { fill = hb ? Theme.accentDark() : Theme.accent(); fg = Theme.onAccent(); }
                else if (bt.kind == FButton.Kind.DANGER) { fill = Theme.alpha(Theme.danger(), hb ? 60 : 30); fg = Theme.danger(); }
                else { fill = hb ? Theme.border() : Theme.surface2(); fg = Theme.text(); }
                g.setColor(fill);
                g.fill(Gfx.rr(bx, bt.r.y, bw, bh, 9));
                g.setColor(fg);
                g.drawString(bt.label, bx + 12, bt.r.y + (bh - fm.getHeight()) / 2f + fm.getAscent());
                bx -= 8;
            }
            btnW = right - bx;
        }
        // thời gian + số chưa đọc
        int textRight = right - btnW - (hasBtns ? 4 : 0);
        if (!hasBtns) {
            g.setFont(Theme.font(java.awt.Font.PLAIN, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            if (!r.time.isEmpty()) {
                int tw = fm.stringWidth(r.time);
                g.setColor(r.badge > 0 ? Theme.accent() : Theme.muted());
                g.drawString(r.time, right - tw, y + 26);
                textRight = right - tw - 10;
            }
            if (r.badge > 0) {
                String b = r.badge > 99 ? "99+" : String.valueOf(r.badge);
                g.setFont(Theme.font(java.awt.Font.BOLD, 11f));
                FontMetrics f2 = g.getFontMetrics();
                int bw = Math.max(20, f2.stringWidth(b) + 12);
                g.setColor(Theme.accent());
                g.fill(Gfx.rr(right - bw, y + h - 30, bw, 20, 10));
                g.setColor(Theme.onAccent());
                g.drawString(b, right - bw + (bw - f2.stringWidth(b)) / 2f, y + h - 30 + (20 - f2.getHeight()) / 2f + f2.getAscent());
                textRight = Math.min(textRight, right - bw - 10);
            }
        }
        g.setFont(Theme.font(r.badge > 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 14.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setFont(Theme.font(java.awt.Font.BOLD, 14.5f));
        fm = g.getFontMetrics();
        g.setColor(Theme.text());
        int maxW = Math.max(30, textRight - tx);
        g.drawString(Gfx.ellipsize(r.title, fm, maxW), tx, y + (r.sub.isEmpty() ? h / 2 + fm.getAscent() / 2 - 2 : 28));
        if (!r.sub.isEmpty()) {
            g.setFont(Theme.font(r.badge > 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 13f));
            fm = g.getFontMetrics();
            g.setColor(r.badge > 0 ? Theme.text() : Theme.muted());
            int sw = (hasBtns ? textRight : right - (r.badge > 0 ? 32 : 0)) - tx;
            g.drawString(Gfx.ellipsize(r.sub, fm, Math.max(30, sw)), tx, y + 49);
        }
    }

    // ----- Scrollable -----
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle v, int o, int d) { return 28; }
    public int getScrollableBlockIncrement(Rectangle v, int o, int d) { return v.height - 40; }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
