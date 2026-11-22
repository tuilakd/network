package chatwave.ui;

import chatwave.core.Models.Msg;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

/** Khung hiển thị tin nhắn kiểu Fluent: bong bóng bo góc, gom nhóm, ngày, ảnh, "Đã xem", hiệu ứng trượt khi có tin mới. */
public class MessageList extends JComponent implements Scrollable {
    public interface ImageListener { void open(Msg m); }
    public interface MediaListener { void open(Msg m); }
    public interface RecallListener { void recall(Msg m); }

    private static final class Item {
        Msg m; String day; boolean mine, groupStart, groupEnd, showName, showAvatar, lastMine;
        List<String> lines = new ArrayList<String>();
        int x, y, w, h, bubbleW, bubbleH, imgW, imgH, textH, metaH; long born; String time;
    }

    private final List<Item> items = new ArrayList<Item>();
    private final Set<Long> known = new HashSet<Long>();
    private List<Msg> data = new ArrayList<Msg>();
    private String me = "";
    private boolean room;
    private int total = 200;
    private ImageListener imageListener;
    private MediaListener mediaListener;
    private RecallListener recallListener;
    private Item hoverMsg;
    private boolean overMore;
    private long playingId = -1;
    private float playProg;
    private int laidWidth = -1;
    private boolean first = true;
    private Item hoverImage;
    private String emptyTitle = "Chưa có tin nhắn nào", emptySub = "Hãy gửi lời chào đầu tiên 👋";

    private static final String RECALLED = "Tin nhắn đã được thu hồi";
    private static final int PAD = 22, PX = 14, PY = 9, AV = 32, MAXB = 520;
    private final Font textFont = Theme.font(Font.PLAIN, 14.5f);
    private final Font metaFont = Theme.font(Font.PLAIN, 11.5f);
    private final Font nameFont = Theme.font(Font.BOLD, 12f);

    public MessageList() {
        setOpaque(false);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { if (getWidth() != laidWidth) { relayout(); revalidate(); repaint(); } }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Item hm = null; boolean om = false;
                for (Item it : items) if (it.m != null && recallListener != null) {
                    if (moreBtn(it).contains(e.getPoint())) { hm = it; om = true; }
                    else if (bubbleRect(it).contains(e.getPoint())) hm = it;
                }
                if (hm != hoverMsg || om != overMore) { hoverMsg = hm; overMore = om; repaint(); }
                Item h = null;
                for (Item it : items) if (it.m != null && !it.m.recalled && (it.m.isImage() || it.m.isMedia()) && imgRect(it).contains(e.getPoint())) h = it;
                if (h != hoverImage) { hoverImage = h; repaint(); }
                setCursor(h != null || om ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
            @Override public void mouseExited(MouseEvent e) { hoverImage = null; hoverMsg = null; overMore = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (recallListener != null) {
                    for (Item it : items) if (it.m != null) {
                        boolean btn = moreBtn(it).contains(e.getPoint());
                        boolean rightClick = SwingUtilities.isRightMouseButton(e) && bubbleRect(it).contains(e.getPoint());
                        if (btn || rightClick) { recallListener.recall(it.m); return; }
                    }
                }
                if (hoverImage == null) return;
                if (hoverImage.m.isMedia()) { if (mediaListener != null) mediaListener.open(hoverImage.m); }
                else if (imageListener != null) imageListener.open(hoverImage.m);
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setImageListener(ImageListener l) { imageListener = l; }
    public void setMediaListener(MediaListener l) { mediaListener = l; }
    public void setRecallListener(RecallListener l) { recallListener = l; }
    /** Báo tin ghi âm nào đang phát (id &lt; 0 = không có) và tiến độ 0..1. */
    public void setPlaying(long id, float prog) { playingId = id; playProg = prog; repaint(); }
    public void setMe(String me) { this.me = me; }
    public void setRoom(boolean r) { room = r; }
    public void setEmptyText(String t, String s) { emptyTitle = t; emptySub = s; repaint(); }
    public List<Msg> getData() { return data; }

    /** Đổi sang cuộc trò chuyện khác: xóa sạch, không chạy hiệu ứng. */
    public void reset() {
        known.clear();
        data = new ArrayList<Msg>();
        items.clear();
        first = true;
        total = 200;
        revalidate();
        repaint();
    }

    /** @return true nếu có tin mới (chưa từng thấy) trong danh sách. */
    public boolean setMessages(List<Msg> list) {
        boolean fresh = false;
        long now = System.currentTimeMillis();
        for (Msg m : list) if (!known.contains(m.id)) { fresh = true; }
        final Set<Long> old = new HashSet<Long>(known);
        data = list;
        buildItems();
        for (Item it : items) {
            if (it.m == null) continue;
            if (!first && !old.contains(it.m.id)) it.born = now;
            known.add(it.m.id);
        }
        first = false;
        relayout();
        revalidate();
        repaint();
        if (fresh && !isEmptyList()) Anim.run(360, new Anim.Update() { public void apply(float t) { repaint(); } });
        return fresh;
    }

    private boolean isEmptyList() { return data.isEmpty(); }

    private static String dayKey(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    private static String dayLabel(long t) {
        Calendar now = Calendar.getInstance();
        if (dayKey(t).equals(dayKey(now.getTimeInMillis()))) return "Hôm nay";
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (dayKey(t).equals(dayKey(now.getTimeInMillis()))) return "Hôm qua";
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date(t));
    }

    private void buildItems() {
        items.clear();
        String lastDay = "";
        Item prev = null;
        for (Msg m : data) {
            String dk = dayKey(m.at);
            if (!dk.equals(lastDay)) {
                Item d = new Item();
                d.day = dayLabel(m.at);
                items.add(d);
                lastDay = dk;
                prev = null;
            }
            Item it = new Item();
            it.m = m;
            it.mine = m.sender.equals(me);
            it.time = new SimpleDateFormat("HH:mm").format(new Date(m.at));
            it.groupStart = prev == null || prev.mine != it.mine || !prev.m.sender.equals(m.sender) || m.at - prev.m.at > 5 * 60000L;
            if (prev != null) prev.groupEnd = it.groupStart;
            it.showName = room && !it.mine && it.groupStart;
            items.add(it);
            prev = it;
        }
        if (prev != null) prev.groupEnd = true;
        for (Item it : items) it.showAvatar = it.m != null && !it.mine && it.groupEnd;
        for (int i = items.size() - 1; i >= 0; i--) if (items.get(i).m != null && items.get(i).mine) { items.get(i).lastMine = true; break; }
    }

    private FontMetrics fm(Font f) { return getFontMetrics(f); }

    private void wrap(Item it, int maxTextW) {
        it.lines.clear();
        FontMetrics f = fm(textFont);
        String text = it.m.recalled ? RECALLED : (it.m.content == null ? "" : it.m.content);
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) { it.lines.add(""); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ", -1)) {
                String trial = line.length() == 0 ? word : line + " " + word;
                if (f.stringWidth(trial) <= maxTextW) { line = new StringBuilder(trial); continue; }
                if (line.length() > 0) { it.lines.add(line.toString()); line = new StringBuilder(); }
                String w = word;
                while (f.stringWidth(w) > maxTextW) {
                    int n = w.length();
                    while (n > 1 && f.stringWidth(w.substring(0, n)) > maxTextW) n--;
                    it.lines.add(w.substring(0, n));
                    w = w.substring(n);
                }
                line = new StringBuilder(w);
            }
            it.lines.add(line.toString());
        }
        if (it.lines.size() > 1 && it.lines.get(it.lines.size() - 1).isEmpty() && text.endsWith("\n")) it.lines.remove(it.lines.size() - 1);
    }

    private void relayout() {
        int W = Math.max(getWidth(), 300);
        laidWidth = getWidth();
        int avSpace = AV + 8;
        int maxBub = Math.min(MAXB, (int) ((W - PAD * 2 - avSpace) * 0.78));
        int y = 16;
        FontMetrics tf = fm(textFont);
        int lh = tf.getHeight() + 2;
        for (Item it : items) {
            if (it.m == null) { it.h = 34; it.y = y; y += it.h + 4; continue; }
            Msg m = it.m;
            boolean hasText = m.recalled || (m.content != null && !m.content.trim().isEmpty());
            wrap(it, maxBub - PX * 2);
            int tw = 0;
            for (String l : it.lines) tw = Math.max(tw, tf.stringWidth(l));
            if (m.recalled) tw += 10;
            it.imgW = 0; it.imgH = 0;
            if (m.isImage()) {
                BufferedImage th = ImageCache.thumb(m.attUrl, 280, 240, null);
                if (th != null) { it.imgW = th.getWidth(); it.imgH = th.getHeight(); }
                else { it.imgW = 240; it.imgH = ImageCache.failed(m.attUrl) ? 70 : 160; }
            }
            if (m.isMedia()) { it.imgW = 250; it.imgH = 62; }
            it.textH = hasText ? it.lines.size() * lh + PY * 2 - 2 : 0;
            int cw = Math.max(hasText ? tw + PX * 2 : 0, it.imgW);
            it.bubbleW = Math.max(cw, 46);
            it.bubbleH = (it.imgH > 0 ? it.imgH : 0) + it.textH;
            if (it.bubbleH == 0) it.bubbleH = 36;
            it.y = y + (it.groupStart ? 10 : 0);
            int top = it.y;
            if (it.showName) top += 16;
            it.h = it.bubbleH;
            it.x = it.mine ? W - PAD - it.bubbleW : PAD + avSpace;
            it.metaH = 0;
            if (it.groupEnd || it.lastMine) it.metaH = 18;
            y = top + it.h + it.metaH + 3;
        }
        total = y + 14;
    }

    private Rectangle bubbleRect(Item it) {
        int by = it.y + (it.showName ? 16 : 0);
        return new Rectangle(it.x, by, it.bubbleW, it.bubbleH);
    }

    /** Vùng nút 3 chấm: nằm bên trái nếu là tin của mình, nằm bên phải nếu là tin của đối phương */
    private Rectangle moreBtn(Item it) {
        Rectangle b = bubbleRect(it);
        int bx = it.mine ? (b.x - 36) : (b.x + b.width + 8);
        return new Rectangle(bx, b.y + b.height / 2 - 14, 28, 28);
    }

    private Rectangle imgRect(Item it) {
        int by = it.y + (it.showName ? 16 : 0);
        return new Rectangle(it.x, by, it.imgW, it.imgH);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(300, total); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        Rectangle clip = g0.getClipBounds() != null ? g0.getClipBounds() : new Rectangle(0, 0, getWidth(), getHeight());
        int W = getWidth();
        if (items.isEmpty()) {
            Icons.CHAT.paint(g, W / 2f - 30, getVisibleRect().y + 90, 60, Theme.alpha(Theme.accent(), 120));
            g.setFont(Theme.font(Font.BOLD, 16f));
            FontMetrics f = g.getFontMetrics();
            g.setColor(Theme.text());
            g.drawString(emptyTitle, (W - f.stringWidth(emptyTitle)) / 2, getVisibleRect().y + 188);
            g.setFont(Theme.font(Font.PLAIN, 13.5f));
            f = g.getFontMetrics();
            g.setColor(Theme.muted());
            g.drawString(emptySub, (W - f.stringWidth(emptySub)) / 2, getVisibleRect().y + 212);
            g.dispose();
            return;
        }
        long now = System.currentTimeMillis();
        for (Item it : items) {
            if (it.y + it.h + 60 < clip.y || it.y - 30 > clip.y + clip.height) continue;
            if (it.m == null) { paintDay(g, it, W); continue; }
            float p = it.born == 0 ? 1f : Math.min(1f, (now - it.born) / 340f);
            p = Anim.easeOut(p);
            Graphics2D gi = (Graphics2D) g.create();
            gi.setComposite(AlphaComposite.SrcOver.derive(p));
            gi.translate((it.mine ? 1 : -1) * (1 - p) * 26, (1 - p) * 10);
            paintMsg(gi, it, W);
            gi.dispose();
        }
        g.dispose();
    }

    private void paintDay(Graphics2D g, Item it, int W) {
        g.setFont(Theme.font(Font.BOLD, 11.5f));
        FontMetrics f = g.getFontMetrics();
        int tw = f.stringWidth(it.day) + 22;
        g.setColor(Theme.surface2());
        g.fill(Gfx.rr((W - tw) / 2f, it.y + 6, tw, 22, 11));
        g.setColor(Theme.muted());
        g.drawString(it.day, (W - tw) / 2f + 11, it.y + 6 + (22 - f.getHeight()) / 2f + f.getAscent());
    }

    private void paintMsg(Graphics2D g, Item it, int W) {
        Msg m = it.m;
        int by = it.y + (it.showName ? 16 : 0);
        if (it.showName) {
            g.setFont(nameFont);
            g.setColor(Theme.muted());
            g.drawString(m.sender, it.x + 4, it.y + 12);
        }
        if (it.showAvatar) Avatar.paint(g, m.sender, PAD, by + it.bubbleH - AV, AV, false, false);

        // Bong bóng bo tròn đều 4 góc (đã bỏ đoạn Area vẽ đuôi gây lỗi góc vuông thừa)
        float arc = Math.min(36f, Math.min(it.bubbleW, it.bubbleH));
        RoundRectangle2D shape = new RoundRectangle2D.Float(it.x, by, it.bubbleW, it.bubbleH, arc, arc);

        Color fill1 = it.mine ? Theme.accent() : Theme.surface();
        Color fill2 = it.mine ? Theme.accentDark() : Theme.surface();

        if (m.recalled) {
            Graphics2D gd = (Graphics2D) g.create();
            gd.setColor(Theme.border());
            gd.setStroke(new java.awt.BasicStroke(1.2f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f));
            gd.draw(shape);
            gd.dispose();
        } else {
            if (!it.mine) { g.setColor(Theme.shadow()); g.translate(0, 1); g.fill(shape); g.translate(0, -1); }
            if (fill1.equals(fill2)) g.setColor(fill1); else g.setPaint(new GradientPaint(it.x, by, fill1, it.x, by + it.bubbleH, fill2));
            g.fill(shape);
            if (!it.mine) { g.setColor(Theme.border()); g.draw(shape); }
        }

        // Vẽ nút 3 chấm (⋮) khi di chuột vào tin nhắn
        if (it == hoverMsg && recallListener != null) {
            Rectangle rb = moreBtn(it);
            g.setColor(overMore ? Theme.accentSoft() : Theme.surface2());
            g.fillOval(rb.x, rb.y, rb.width, rb.height);
            g.setColor(overMore ? Theme.accent() : Theme.muted());
            int cx = rb.x + rb.width / 2 - 1;
            int cy = rb.y + rb.height / 2;
            g.fillOval(cx, cy - 6, 3, 3);
            g.fillOval(cx, cy - 1, 3, 3);
            g.fillOval(cx, cy + 4, 3, 3);
        }

        // ảnh
        int ty = by;
        if (m.isImage()) {
            BufferedImage th = ImageCache.thumb(m.attUrl, 280, 240, new Runnable() { public void run() { relayout(); revalidate(); repaint(); } });
            Graphics2D gc = (Graphics2D) g.create();
            gc.clip(shape);
            if (th != null) {
                gc.drawImage(th, it.x, by, it.imgW, it.imgH, null);
                if (it == hoverImage) { gc.setColor(new Color(255, 255, 255, 36)); gc.fillRect(it.x, by, it.imgW, it.imgH); }
            } else {
                gc.setColor(Theme.surface2());
                gc.fillRect(it.x, by, it.imgW, it.imgH);
                gc.setColor(Theme.muted());
                gc.setFont(Theme.small());
                String s = ImageCache.failed(m.attUrl) ? "Không tải được ảnh" : "Đang tải ảnh…";
                gc.drawString(s, it.x + (it.imgW - gc.getFontMetrics().stringWidth(s)) / 2, by + it.imgH / 2 + 4);
            }
            gc.dispose();
            ty += it.imgH;
        }
        if (m.isMedia()) { paintMedia(g, it, by); ty += it.imgH; }

        // chữ
        if (!it.lines.isEmpty() && it.textH > 0) {
            g.setFont(m.recalled ? textFont.deriveFont(Font.ITALIC) : textFont);
            FontMetrics f = g.getFontMetrics();
            g.setColor(m.recalled ? Theme.muted() : it.mine ? Theme.onAccent() : Theme.text());
            int lh = f.getHeight() + 2;
            int y = ty + PY - 1 + f.getAscent();
            for (String l : it.lines) { g.drawString(l, it.x + PX, y); y += lh; }
        }

        // giờ + trạng thái
        if (it.metaH > 0) {
            g.setFont(metaFont);
            FontMetrics f = g.getFontMetrics();
            String meta = it.time;
            Color c = Theme.muted();
            if (it.mine && it.lastMine && !room) {
                meta = it.time + "  ·  " + (m.read ? "Đã xem" : "Đã gửi");
                if (m.read) c = Theme.accent();
            }
            g.setColor(c);
            int mw = f.stringWidth(meta);
            int mx = it.mine ? it.x + it.bubbleW - mw - 2 : it.x + 4;
            g.drawString(meta, mx, by + it.bubbleH + 14);
        }
    }

    private void paintMedia(Graphics2D g, Item it, int by) {
        Msg m = it.m;
        boolean audio = m.isAudio(), playing = audio && m.id == playingId;
        Color fg = it.mine ? Theme.onAccent() : Theme.accent();
        int cx = it.x + 14, cy = by + (it.imgH - 40) / 2;
        g.setColor(it.mine ? new Color(255, 255, 255, 56) : Theme.accentSoft());
        g.fillOval(cx, cy, 40, 40);
        (playing ? Icons.PAUSE : Icons.PLAY).paint(g, cx + 8, cy + 8, 24, fg);
        int x0 = cx + 54, x1 = it.x + it.bubbleW - 16;
        if (audio) {
            int n = Math.max(6, (x1 - x0) / 5);
            for (int i = 0; i < n; i++) {
                long hsh = (m.id * 31 + i * 17) * 2654435761L;
                int hh = 6 + (int) ((hsh >>> 8) % 20);
                boolean on = playing && (float) i / n < playProg;
                g.setColor(on ? fg : Theme.alpha(fg, it.mine ? 130 : 110));
                g.fill(Gfx.rr(x0 + i * 5, by + 24 - hh / 2f, 3, hh, 1.5f));
            }
            g.setFont(metaFont);
            g.setColor(it.mine ? Theme.alpha(Theme.onAccent(), 200) : Theme.muted());
            g.drawString(playing ? "Đang phát - bấm để dừng" : "Ghi âm", x0, by + it.imgH - 10);
        } else {
            g.setFont(Theme.font(Font.BOLD, 14f));
            g.setColor(it.mine ? Theme.onAccent() : Theme.text());
            g.drawString("Video", x0, by + it.imgH / 2 - 1);
            g.setFont(metaFont);
            g.setColor(it.mine ? Theme.alpha(Theme.onAccent(), 200) : Theme.muted());
            g.drawString("Bấm để mở bằng trình phát video", x0, by + it.imgH / 2 + 16);
        }
    }

    // ----- Scrollable -----
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle v, int o, int d) { return 32; }
    public int getScrollableBlockIncrement(Rectangle v, int o, int d) { return v.height - 60; }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}