package chatwave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Ô nhập liệu bo góc: gạch chân màu nhấn lan ra khi chọn, chữ gợi ý mờ, nút hiện/ẩn mật khẩu. */
public class FField extends JPanel {
    private final JTextField input;
    private String placeholder;
    private final Icons icon;
    private final boolean password;
    private boolean reveal;
    private final Anim.Fader focus = new Anim.Fader(this);
    private final Anim.Fader err = new Anim.Fader(this);
    private int shake;

    public FField(String placeholder, Icons icon, boolean password) {
        super(new BorderLayout());
        this.placeholder = placeholder;
        this.icon = icon;
        this.password = password;
        setOpaque(false);
        input = password ? new JPasswordField() : new JTextField();
        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(Theme.font(java.awt.Font.PLAIN, 14.5f));
        add(input, BorderLayout.CENTER);
        setBorder(new EmptyBorder(0, icon != null ? 44 : 16, 2, password ? 46 : 14));
        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focus.to(1f, 220); repaint(); }
            @Override public void focusLost(FocusEvent e) { focus.to(0f, 220); repaint(); }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        if (password) {
            setCursor(Cursor.getDefaultCursor());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getX() > getWidth() - 46) {
                        reveal = !reveal;
                        ((JPasswordField) input).setEchoChar(reveal ? (char) 0 : '•');
                        repaint();
                    }
                }
            });
            ((JPasswordField) input).setEchoChar('•');
        }
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { input.requestFocusInWindow(); }
        });
    }

    public JTextField getInput() { return input; }
    public void setPlaceholder(String p) { placeholder = p; repaint(); }
    public String getText() { return password ? new String(((JPasswordField) input).getPassword()) : input.getText(); }
    public void setText(String s) { input.setText(s); }
    public void addActionListener(ActionListener l) { input.addActionListener(l); }
    @Override public void requestFocus() { input.requestFocusInWindow(); }

    /** Hiệu ứng lắc + viền đỏ khi nhập sai. */
    public void flagError() {
        err.v = 1f;
        err.to(0f, 1600);
        Anim.run(380, new Anim.Update() {
            public void apply(float t) { shake = (int) (Math.sin(t * Math.PI * 6) * 6 * (1 - t)); repaint(); }
        });
    }

    @Override public Dimension getPreferredSize() { return new Dimension(260, 46); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 46); }

    @Override protected void paintComponent(Graphics g0) {
        input.setForeground(Theme.text());
        input.setCaretColor(Theme.accent());
        input.setSelectionColor(Theme.alpha(Theme.accent(), 90));
        input.setSelectedTextColor(Theme.text());
        Graphics2D g = Gfx.g2(g0);
        int w = getWidth(), h = getHeight();
        g.translate(shake, 0);
        g.setColor(Theme.surface2());
        g.fill(Gfx.rr(0, 0, w, h, 10));
        g.setColor(Theme.lerp(Theme.border(), Theme.danger(), err.v));
        g.draw(Gfx.rr(0.5f, 0.5f, w - 1, h - 1, 10));
        // gạch chân màu nhấn lan từ giữa ra
        if (focus.v > 0.01f) {
            float bw = (w - 20) * focus.v;
            g.setColor(Theme.accent());
            g.fill(Gfx.rr((w - bw) / 2f, h - 3, bw, 2.5f, 1.2f));
        }
        if (icon != null) icon.paint(g, 14, (h - 18) / 2f, 18, Theme.lerp(Theme.muted(), Theme.accent(), focus.v));
        if (input.getText().isEmpty()) {
            g.setFont(Theme.font(java.awt.Font.PLAIN, 14.5f));
            g.setColor(Theme.muted());
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(placeholder, icon != null ? 44 : 16, (h - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (password) Icons.EYE.paint(g, w - 36, (h - 18) / 2f, 18, reveal ? Theme.accent() : Theme.muted());
        g.dispose();
    }
}
