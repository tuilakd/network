package chatwave.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** Công tắc bật/tắt có núm trượt mượt. */
public class FSwitch extends JComponent {
    public interface Listener { void changed(boolean on); }
    private boolean on;
    private float pos;
    private Anim.Tween tw;
    private Listener listener;

    public FSwitch(boolean initial) {
        on = initial;
        pos = initial ? 1f : 0f;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { setOn(!on, true); } });
    }

    public boolean isOn() { return on; }
    public void setListener(Listener l) { listener = l; }

    public void setOn(boolean v, boolean notify) {
        if (on == v) return;
        on = v;
        final float from = pos, to = v ? 1f : 0f;
        if (tw != null) tw.cancel();
        tw = Anim.run(200, new Anim.Update() { public void apply(float t) { pos = from + (to - from) * t; repaint(); } });
        if (notify && listener != null) listener.changed(v);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(46, 26); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.g2(g0);
        int w = getWidth(), h = getHeight();
        g.setColor(Theme.lerp(Theme.border(), Theme.accent(), pos));
        g.fill(Gfx.rr(0, 0, w, h, h / 2f));
        float d = h - 8;
        float x = 4 + pos * (w - d - 8);
        g.setColor(Theme.lerp(Theme.muted(), Theme.onAccent(), pos));
        g.fillOval(Math.round(x), 4, Math.round(d), Math.round(d));
        g.dispose();
    }
}
