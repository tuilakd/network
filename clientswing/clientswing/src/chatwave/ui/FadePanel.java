package chatwave.ui;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/** Panel có thể mờ dần / hiện dần (alpha) và trượt nhẹ khi xuất hiện. */
public class FadePanel extends JPanel {
    private float alpha = 1f;
    private int offsetY = 0;

    public FadePanel(LayoutManager lm) { super(lm); setOpaque(false); }
    public FadePanel() { this(null); }

    public void setAlpha(float a) { alpha = Math.max(0f, Math.min(1f, a)); repaint(); }
    public float getAlpha() { return alpha; }
    public void setOffsetY(int y) { offsetY = y; repaint(); }

    /** Hiện dần + trượt lên. */
    public void playIn(int ms, final int rise) {
        Anim.run(ms, new Anim.Update() {
            public void apply(float t) { alpha = t; offsetY = (int) ((1 - t) * rise); repaint(); }
        });
    }

    @Override public void paint(Graphics g) {
        if (alpha >= 0.999f && offsetY == 0) { super.paint(g); return; }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.translate(0, offsetY);
        super.paint(g2);
        g2.dispose();
    }
}
