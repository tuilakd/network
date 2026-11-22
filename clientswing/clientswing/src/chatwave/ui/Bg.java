package chatwave.ui;

import java.awt.Graphics;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/** Panel nền tự đổi màu theo Theme (sáng/tối). */
public class Bg extends JPanel {
    public enum Kind { BG, SIDE, SURFACE }
    private final Kind kind;
    public Bg(Kind k, LayoutManager lm) { super(lm); kind = k; setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        g.setColor(kind == Kind.BG ? Theme.bg() : kind == Kind.SIDE ? Theme.side() : Theme.surface());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
