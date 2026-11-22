package chatwave.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Lớp phủ trên cùng cửa sổ (glass pane): hộp thoại bật lên giữa màn hình trên nền mờ tối, và thông báo toast.
 */
public class Overlay extends JComponent {
    private JComponent card;
    private float open;          // 0..1 tiến độ mở
    private boolean dismissOnScrim = true;
    private Runnable onClose;
    private Anim.Tween tween;

    private static final class Toast {
        String text; boolean error; float t;
    }
    private final List<Toast> toasts = new ArrayList<Toast>();

    public Overlay() {
        setOpaque(false);
        setLayout(null);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (card != null && dismissOnScrim && !card.getBounds().contains(e.getPoint())) close();
            }
        });
    }

    public void install(JRootPane root) {
        root.setGlassPane(this);
        setVisible(true);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ovClose");
        root.getActionMap().put("ovClose", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (card != null && dismissOnScrim) close(); }
        });
    }

    /** Chỉ nhận chuột khi đang có hộp thoại; nếu không thì để chuột xuyên qua tới giao diện bên dưới. */
    @Override public boolean contains(int x, int y) { return card != null; }

    public boolean isOpen() { return card != null; }

    public void show(JComponent c) { show(c, true, null); }

    public void show(JComponent c, boolean dismissOnScrim, Runnable onClose) {
        if (card != null) removeNow();
        this.card = c;
        this.dismissOnScrim = dismissOnScrim;
        this.onClose = onClose;
        add(c);
        Dimension d = c.getPreferredSize();
        c.setSize(d);
        layoutCard();
        if (tween != null) tween.cancel();
        tween = Anim.run(260, new Anim.Update() {
            public void apply(float t) { open = t; repaint(); }
        });
        repaint();
    }

    private void layoutCard() {
        if (card == null) return;
        Dimension d = card.getPreferredSize();
        int w = Math.min(d.width, Math.max(240, getWidth() - 40));
        int h = Math.min(d.height, Math.max(200, getHeight() - 40));
        card.setBounds((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
        card.validate();
    }

    @Override public void doLayout() { layoutCard(); }

    private void removeNow() {
        if (card != null) { remove(card); card = null; }
        Runnable r = onClose;
        onClose = null;
        if (r != null) r.run();
    }

    public void close() {
        if (card == null) return;
        if (tween != null) tween.cancel();
        final float from = open;
        tween = Anim.run(160, new Anim.Update() {
            public void apply(float t) { open = from * (1 - t); repaint(); }
        }, new Runnable() { public void run() { removeNow(); open = 0; repaint(); } });
    }

    // ---------- toast ----------
    public void toast(String text, boolean error) {
        final Toast t = new Toast();
        t.text = text;
        t.error = error;
        toasts.add(t);
        if (toasts.size() > 3) toasts.remove(0);
        Anim.Tween tw = Anim.run(3600, new Anim.Update() {
            public void apply(float p) { t.t = p; repaint(); }
        }, new Runnable() { public void run() { toasts.remove(t); repaint(); } });
        // easeOut làm p tăng nhanh đầu; dùng thời gian thực để hết hạn đúng: chấp nhận gần đúng
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        if (card != null && open > 0) {
            g.setColor(Theme.alpha(Theme.scrim(), (int) (Theme.scrim().getAlpha() * open)));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // toast
        g.setFont(Theme.font(java.awt.Font.BOLD, 13.5f));
        FontMetrics fm = g.getFontMetrics();
        int y = getHeight() - 28;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            float in = Math.min(1f, t.t / 0.07f);
            float out = t.t > 0.9f ? (1f - t.t) / 0.1f : 1f;
            float a = Math.min(in, out);
            int tw = fm.stringWidth(t.text) + 60, th = 44;
            int x = (getWidth() - tw) / 2;
            int yy = y - th + (int) ((1 - in) * 18);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, a)));
            Gfx.shadow(g, x, yy, tw, th, 22, 10, 4);
            g.setColor(Theme.dark ? Theme.surface2() : new Color(0x22323A));
            g.fill(Gfx.rr(x, yy, tw, th, 22));
            Color dot = t.error ? Theme.danger() : Theme.ok();
            g.setColor(dot);
            g.fillOval(x + 16, yy + th / 2 - 5, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString(t.text, x + 38, yy + (th - fm.getHeight()) / 2 + fm.getAscent());
            g.setComposite(AlphaComposite.SrcOver);
            y -= th + 10;
        }
        g.dispose();
    }

    @Override protected void paintChildren(Graphics g0) {
        if (card == null) return;
        Graphics2D g = (Graphics2D) g0.create();
        float e = open;
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, e))));
        double s = 0.94 + 0.06 * e;
        double cx = card.getX() + card.getWidth() / 2.0, cy = card.getY() + card.getHeight() / 2.0;
        g.translate(cx, cy + (1 - e) * 14);
        g.scale(s, s);
        g.translate(-cx, -cy);
        super.paintChildren(g);
        g.dispose();
    }
}
