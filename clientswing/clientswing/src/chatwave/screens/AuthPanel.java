package chatwave.screens;

import chatwave.core.*;
import chatwave.core.Models.Account;
import chatwave.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;
import javax.swing.*;

/** Màn hình đăng nhập / đăng ký (cache nền theo pixel thật để mượt trên màn hình HiDPI). */
public class AuthPanel extends JPanel {
    private final Nav nav;
    private final Preferences prefs = Preferences.userNodeForPackage(AuthPanel.class);
    private final SegTabs tabs = new SegTabs("Đăng nhập", "Đăng ký");
    private final FField user = new FField("Tên đăng nhập hoặc email", Icons.USER, false);
    private final FField email = new FField("Email", Icons.MAIL, false);
    private final FField pass = new FField("Mật khẩu", Icons.LOCK, true);
    private final FField pass2 = new FField("Nhập lại mật khẩu", Icons.LOCK, true);
    private final FField server = new FField("Địa chỉ server (http://localhost:8080)", Icons.SETTINGS, false);
    private final FSwitch demo;
    private final FButton go;
    private final JLabel msg = new JLabel(" ");
    private final Card card = new Card(30);
    private JLabel forgot;
    private JPanel forgotRow;

    // Cache ảnh nền (kích thước theo pixel thật của màn hình)
    private BufferedImage bgCache;
    private int cachedPW = -1, cachedPH = -1;
    private double cachedScale = -1;
    private boolean cachedDark;

    public AuthPanel(Nav nav) {
        this.nav = nav;
        setLayout(new BorderLayout());
        setOpaque(true);
        demo = new FSwitch(prefs.getBoolean("demo", false));
        server.setText(prefs.get("server", "http://localhost:8080"));
        user.setText(prefs.get("user", ""));
        go = new FButton("Đăng nhập", Icons.CHECK, FButton.Kind.PRIMARY, new Runnable() { public void run() { submit(); } });
        go.setPreferredSize(new Dimension(340, 46));

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        BrandLogo logo = new BrandLogo(60);
        logo.setAlignmentX(0.5f);
        card.add(logo);
        card.add(Box.createVerticalStrut(14));
        card.add(centerLabel("ChatWave", Theme.font(Font.BOLD, 26f), false));
        card.add(Box.createVerticalStrut(2));
        card.add(centerLabel("Nhắn tin nhanh, gọn và đẹp", Theme.font(Font.PLAIN, 14f), true));
        card.add(Box.createVerticalStrut(22));
        tabs.setAlignmentX(0.5f);
        tabs.setPreferredSize(new Dimension(340, 40));
        tabs.setMaximumSize(new Dimension(340, 40));
        card.add(tabs);
        card.add(Box.createVerticalStrut(18));
        for (FField f : new FField[]{user, email, pass, pass2}) {
            f.setPreferredSize(new Dimension(340, 46));
            f.setMaximumSize(new Dimension(340, 46));
            f.setAlignmentX(0.5f);
            card.add(f);
            card.add(Box.createVerticalStrut(10));
        }
        email.setVisible(false); pass2.setVisible(false);
        forgot = new JLabel("Quên mật khẩu?") {
            @Override public Color getForeground() { return Theme.accent(); }
        };
        forgot.setFont(Theme.font(Font.BOLD, 13f));
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { openForgot(); }
        });
        JPanel fp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        fp.setOpaque(false);
        fp.add(forgot);
        fp.setPreferredSize(new Dimension(340, 20));
        fp.setMaximumSize(new Dimension(340, 20));
        fp.setAlignmentX(0.5f);
        forgotRow = fp;
        card.add(fp);
        card.add(Box.createVerticalStrut(4));
        msg.setFont(Theme.font(Font.PLAIN, 13f));
        msg.setAlignmentX(0.5f);
        card.add(msg);
        card.add(Box.createVerticalStrut(6));
        go.setAlignmentX(0.5f);
        go.setMaximumSize(new Dimension(340, 46));
        card.add(go);
        card.add(Box.createVerticalStrut(18));

        JPanel dm = new JPanel(new BorderLayout(10, 0));
        dm.setOpaque(false);
        dm.setMaximumSize(new Dimension(340, 30));
        dm.setPreferredSize(new Dimension(340, 30));
        dm.setAlignmentX(0.5f);
        JLabel dl = new JLabel("Chế độ demo (không cần server)");
        dl.setFont(Theme.font(Font.PLAIN, 13f));
        dl.setForeground(Theme.muted());
        dm.add(dl, BorderLayout.CENTER);
        dm.add(demo, BorderLayout.EAST);
        card.add(dm);
        card.add(Box.createVerticalStrut(10));
        server.setPreferredSize(new Dimension(340, 46));
        server.setMaximumSize(new Dimension(340, 46));
        server.setAlignmentX(0.5f);
        server.setVisible(!demo.isOn());
        card.add(server);
        demo.setListener(new FSwitch.Listener() {
            public void changed(boolean on) { server.setVisible(!on); card.revalidate(); AuthPanel.this.revalidate(); repaint(); }
        });
        tabs.setListener(new SegTabs.Listener() {
            public void changed(int i) {
                boolean reg = i == 1;
                email.setVisible(reg); pass2.setVisible(reg);
                forgotRow.setVisible(!reg);
                user.setPlaceholder(reg ? "Tên đăng nhập" : "Tên đăng nhập hoặc email");
                go.setText(reg ? "Tạo tài khoản" : "Đăng nhập");
                setMsg(" ", false);
                card.revalidate(); AuthPanel.this.revalidate(); repaint();
            }
        });
        java.awt.event.ActionListener enter = new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { submit(); } };
        user.addActionListener(enter); pass.addActionListener(enter); pass2.addActionListener(enter); email.addActionListener(enter);
        card.setMinimumSize(card.getPreferredSize());
        Page page = new Page(new GridBagLayout());
        page.add(card);
        JScrollPane sp = ModernScroll.wrap(page);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        // Viewport trong suốt + BLIT mặc định hay gây repaint thừa / giật khi đổi focus
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        add(sp, BorderLayout.CENTER);
    }

    /** Trang chứa thẻ đăng nhập: luôn rộng bằng cửa sổ, cao tối thiểu bằng nội dung (thấp hơn thì hiện thanh cuộn). */
    private static final class Page extends JPanel implements Scrollable {
        Page(LayoutManager lm) { super(lm); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height;
        }
    }

    private JLabel centerLabel(String t, Font f, final boolean muted) {
        JLabel l = new JLabel(t) {
            @Override public Color getForeground() { return muted ? Theme.muted() : Theme.text(); }
        };
        l.setFont(f);
        l.setAlignmentX(0.5f);
        return l;
    }

    private void openForgot() {
        boolean useDemo = demo.isOn();
        String url = server.getText().trim().isEmpty() ? "http://localhost:8080" : server.getText().trim();
        Backend b = useDemo ? new DemoBackend() : new RemoteBackend(url);
        ForgotDialog.show(nav.overlay(), b, useDemo, user.getText().contains("@") ? user.getText().trim() : "");
    }

    private void setMsg(String t, boolean err) {
        msg.setText(t.isEmpty() ? " " : t);
        msg.setForeground(err ? Theme.danger() : Theme.ok());
    }

    private void submit() {
        final boolean reg = tabs.getSelected() == 1;
        final String u = user.getText().trim(), p = pass.getText();
        if (u.isEmpty()) { user.flagError(); setMsg("Nhập tên đăng nhập", true); return; }
        if (p.isEmpty()) { pass.flagError(); setMsg("Nhập mật khẩu", true); return; }
        if (reg) {
            if (email.getText().trim().isEmpty() || !email.getText().contains("@")) { email.flagError(); setMsg("Email chưa hợp lệ", true); return; }
            if (!p.equals(pass2.getText())) { pass2.flagError(); setMsg("Mật khẩu nhập lại không khớp", true); return; }
        }
        final boolean useDemo = demo.isOn();
        final String url = server.getText().trim().isEmpty() ? "http://localhost:8080" : server.getText().trim();
        prefs.putBoolean("demo", useDemo); prefs.put("server", url); prefs.put("user", u);
        final Backend b = useDemo ? new DemoBackend() : new RemoteBackend(url);
        go.setLoading(true);
        setMsg(" ", false);
        Async.run(new java.util.concurrent.Callable<Account>() {
            public Account call() throws Exception {
                if (reg) b.register(u, email.getText().trim(), p);
                return b.login(u, p);
            }
        }, new Async.Ok<Account>() {
            public void accept(Account a) {
                go.setLoading(false);
                Session.backend = b; Session.account = a;
                pass.setText(""); pass2.setText("");
                if (a.isAdmin()) nav.showAdmin(); else nav.showMain();
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                go.setLoading(false);
                setMsg(e.getMessage() == null ? "Có lỗi xảy ra" : e.getMessage(), true);
                card.revalidate();
                pass.flagError();
            }
        });
    }

    public void focusFirst() {
        SwingUtilities.invokeLater(new Runnable() { public void run() { (user.getText().isEmpty() ? user : pass).requestFocus(); } });
    }

    @Override protected void paintComponent(Graphics g0) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g2 = (Graphics2D) g0;

        // Scale thật của màn hình (1.0, 1.25, 1.5, 2.0...) để cache đúng pixel vật lý
        double sc = g2.getTransform().getScaleX();
        if (sc <= 0) sc = 1;
        int pw = (int) Math.ceil(w * sc), ph = (int) Math.ceil(h * sc);

        if (bgCache == null || cachedPW != pw || cachedPH != ph || cachedScale != sc || cachedDark != Theme.dark) {
            cachedPW = pw; cachedPH = ph; cachedScale = sc; cachedDark = Theme.dark;
            bgCache = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = Gfx.g2(bgCache.createGraphics());
            g.scale(sc, sc); // vẽ theo toạ độ logic nhưng ra ảnh pixel thật
            g.setPaint(new GradientPaint(0, 0, Theme.lerp(Theme.bg(), Theme.accent(), Theme.dark ? .10f : .16f), w, h, Theme.bg()));
            g.fillRect(0, 0, w, h);
            blob(g, w * 0.22f, h * 0.26f, 280, Theme.accent(), Theme.dark ? 50 : 70);
            blob(g, w * 0.80f, h * 0.78f, 340, new Color(0x6F9BEF), Theme.dark ? 40 : 60);
            blob(g, w * 0.72f, h * 0.16f, 200, new Color(0xF2A7B8), Theme.dark ? 28 : 55);
            g.dispose();
        }
        // Vẽ 1:1, chỉ trong vùng clip cần repaint
        g2.drawImage(bgCache, 0, 0, w, h, null);
    }

    private void blob(Graphics2D g, float cx, float cy, float r, Color c, int a) {
        g.setPaint(new RadialGradientPaint(cx, cy, r, new float[]{0f, 1f}, new Color[]{Theme.alpha(c, a), Theme.alpha(c, 0)}));
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }
}