package chatwave.screens;

import chatwave.core.*;
import chatwave.core.Models.*;
import chatwave.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Trang quản trị: thống kê, quản lý người dùng, nhật ký request và lỗi. */
public class AdminPanel extends Bg {
    private final Nav nav;
    private final SegTabs tabs = new SegTabs("Người dùng", "Nhật ký request", "Nhật ký lỗi");
    private final DataTable users = new DataTable(new String[]{"ID", "Tên đăng nhập", "Email", "Quyền"}, new int[]{DataTable.MUTED, DataTable.TEXT, DataTable.MUTED, DataTable.STATUS}, new int[]{60, 220, 280, 100});
    private final DataTable reqLogs = new DataTable(new String[]{"Thời gian", "Method", "URL", "Người dùng", "IP", "Mã", "ms"}, new int[]{DataTable.MUTED, DataTable.METHOD, DataTable.TEXT, DataTable.TEXT, DataTable.MUTED, DataTable.STATUS, DataTable.MUTED}, new int[]{140, 90, 300, 130, 120, 70, 60});
    private final DataTable errLogs = new DataTable(new String[]{"Thời gian", "Mã", "Loại lỗi", "Thông báo", "Đường dẫn", "Người dùng"}, new int[]{DataTable.MUTED, DataTable.STATUS, DataTable.TEXT, DataTable.TEXT, DataTable.MUTED, DataTable.TEXT}, new int[]{140, 70, 160, 300, 200, 110});
    private final CardLayout cards = new CardLayout();
    private final JPanel tableHost = new JPanel(cards);
    private final FButton roleBtn, delBtn, refreshBtn;
    private final Stat sUsers = new Stat("Người dùng", Icons.USER), sReq = new Stat("Request gần đây", Icons.SHIELD), sErr = new Stat("Lỗi gần đây", Icons.INFO);
    private List<AdminUser> userData = new ArrayList<AdminUser>();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM HH:mm:ss");

    public AdminPanel(Nav nav) {
        super(Kind.BG, new BorderLayout());
        this.nav = nav;
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        Bg top = new Bg(Kind.SURFACE, null) {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = Gfx.g2(g0);
                g.setColor(Theme.border());
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
                BrandLogo.paintLogo(g, 24, 14, 40);
                g.setFont(Theme.font(Font.BOLD, 17f));
                g.setColor(Theme.text());
                g.drawString("ChatWave Admin", 76, 31);
                g.setFont(Theme.font(Font.PLAIN, 12.5f));
                g.setColor(Theme.muted());
                g.drawString("Xin chào, " + Session.me(), 76, 50);
                g.dispose();
            }
        };
        top.setPreferredSize(new Dimension(100, 68));
        final FButton theme = FButton.icon(Theme.dark ? Icons.SUN : Icons.MOON, "Sáng / tối", null);
        theme.setAction(new Runnable() { public void run() { AdminPanel.this.nav.toggleTheme(); theme.setIcon(Theme.dark ? Icons.SUN : Icons.MOON); } });
        final FButton out = new FButton("Đăng xuất", Icons.LOGOUT, FButton.Kind.SOFT, new Runnable() {
            public void run() {
                Dialogs.confirm(AdminPanel.this.nav.overlay(), "Đăng xuất", "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", true, new Runnable() {
                    public void run() { try { Session.backend.logout(); } catch (Exception ignored) { } AdminPanel.this.nav.showAuth(); }
                });
            }
        });
        top.add(theme); top.add(out);
        top.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                theme.setBounds(top.getWidth() - 24 - 130 - 8 - 40, 14, 40, 40);
                out.setBounds(top.getWidth() - 24 - 130, 14, 130, 40);
            }
        });
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(22, 28, 24, 28));

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(sUsers); stats.add(sReq); stats.add(sErr);
        stats.setPreferredSize(new Dimension(100, 96));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        tabs.setPreferredSize(new Dimension(440, 40));
        bar.add(tabs, BorderLayout.WEST);
        roleBtn = new FButton("Đổi quyền", Icons.SHIELDUSER, FButton.Kind.SOFT, new Runnable() { public void run() { changeRole(); } });
        delBtn = new FButton("Xóa", Icons.TRASH, FButton.Kind.DANGER, new Runnable() { public void run() { deleteUser(); } });
        refreshBtn = new FButton("Làm mới", Icons.SEARCH, FButton.Kind.GHOST, new Runnable() { public void run() { load(); } });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(roleBtn); right.add(delBtn); right.add(refreshBtn);
        bar.add(right, BorderLayout.EAST);

        Card tableCard = new Card(2, 16f, true);
        tableCard.setLayout(new BorderLayout());
        tableHost.setOpaque(false);
        tableHost.add(users.scroll(), "0");
        tableHost.add(reqLogs.scroll(), "1");
        tableHost.add(errLogs.scroll(), "2");
        tableCard.add(tableHost, BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout(0, 14));
        mid.setOpaque(false);
        mid.add(bar, BorderLayout.NORTH);
        mid.add(tableCard, BorderLayout.CENTER);
        center.add(stats, BorderLayout.NORTH);
        center.add(mid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        tabs.setListener(new SegTabs.Listener() {
            public void changed(int i) { cards.show(tableHost, String.valueOf(i)); roleBtn.setVisible(i == 0); delBtn.setVisible(i == 0); }
        });
        load();
    }

    private void load() {
        refreshBtn.setLoading(true);
        Async.run(new java.util.concurrent.Callable<Object[]>() {
            public Object[] call() throws Exception {
                Backend b = Session.backend;
                return new Object[]{b.adminUsers(), b.requestLogs(100), b.errorLogs(100)};
            }
        }, new Async.Ok<Object[]>() {
            @SuppressWarnings("unchecked") public void accept(Object[] d) {
                refreshBtn.setLoading(false);
                userData = (List<AdminUser>) d[0];
                List<ReqLog> rl = (List<ReqLog>) d[1];
                List<ErrLog> el = (List<ErrLog>) d[2];
                List<Object[]> r = new ArrayList<Object[]>();
                for (AdminUser u : userData) r.add(new Object[]{u.id, u.username, u.email, u.role});
                users.setRows(r);
                r = new ArrayList<Object[]>();
                for (ReqLog l : rl) r.add(new Object[]{FMT.format(new Date(l.at)), l.method, l.url, l.username == null ? "" : l.username, l.ip, String.valueOf(l.status), l.ms});
                reqLogs.setRows(r);
                r = new ArrayList<Object[]>();
                for (ErrLog l : el) r.add(new Object[]{FMT.format(new Date(l.at)), String.valueOf(l.status), l.type, l.message, l.path, l.user == null ? "" : l.user});
                errLogs.setRows(r);
                sUsers.set(userData.size());
                sReq.set(rl.size());
                sErr.set(el.size());
            }
        }, new Async.Fail() {
            public void accept(Exception e) { refreshBtn.setLoading(false); nav.overlay().toast(e.getMessage(), true); }
        });
    }

    private AdminUser selected() {
        int r = users.getSelectedRow();
        if (r < 0 || r >= userData.size()) { nav.overlay().toast("Hãy chọn một người dùng trong bảng", true); return null; }
        return userData.get(users.convertRowIndexToModel(r));
    }

    private void changeRole() {
        final AdminUser u = selected();
        if (u == null) return;
        if (u.username.equals(Session.me())) { nav.overlay().toast("Không thể đổi quyền của chính bạn", true); return; }
        final String next = "ADMIN".equalsIgnoreCase(u.role) ? "USER" : "ADMIN";
        Dialogs.confirm(nav.overlay(), "Đổi quyền", "Đổi quyền của " + u.username + " thành " + next + "?", "Đổi quyền", false, new Runnable() {
            public void run() {
                Async.run(new java.util.concurrent.Callable<Boolean>() {
                              public Boolean call() throws Exception { Session.backend.adminSetRole(u.id, next); return true; }
                          }, new Async.Ok<Boolean>() { public void accept(Boolean b) { nav.overlay().toast("Đã cập nhật quyền", false); load(); } },
                        new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
            }
        });
    }

    private void deleteUser() {
        final AdminUser u = selected();
        if (u == null) return;
        if (u.username.equals(Session.me())) { nav.overlay().toast("Không thể xóa chính bạn", true); return; }
        Dialogs.confirm(nav.overlay(), "Xóa người dùng", "Xóa tài khoản " + u.username + "? Hành động này không thể hoàn tác.", "Xóa", true, new Runnable() {
            public void run() {
                Async.run(new java.util.concurrent.Callable<Boolean>() {
                              public Boolean call() throws Exception { Session.backend.adminDeleteUser(u.id); return true; }
                          }, new Async.Ok<Boolean>() { public void accept(Boolean b) { nav.overlay().toast("Đã xóa " + u.username, false); load(); } },
                        new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
            }
        });
    }

    /**
     * Thẻ thống kê có số đếm chạy lên.
     * Phần tĩnh (bóng, nền, icon, nhãn) được cache thành ảnh; mỗi frame chỉ vẽ lại con số.
     */
    private static final class Stat extends JComponent {
        private final String label; private final Icons icon;
        private float shown; private int target;

        private BufferedImage cache;
        private int cPW = -1, cPH = -1;
        private double cScale = -1;
        private boolean cDark;

        Stat(String label, Icons icon) { this.label = label; this.icon = icon; }

        void set(int n) {
            final float from = shown; target = n;
            Anim.run(700, new Anim.Update() {
                public void apply(float t) {
                    shown = from + (target - from) * Anim.easeOut(t);
                    // chỉ vẽ lại vùng chứa con số, không vẽ lại cả thẻ + bóng
                    int h = getHeight() - 6;
                    repaint(78, Math.max(0, h / 2 - 34), Math.max(1, getWidth() - 78), 46);
                }
            });
        }

        private void buildCache(int w, int fullH, double sc) {
            int pw = (int) Math.ceil(w * sc), ph = (int) Math.ceil(fullH * sc);
            cPW = pw; cPH = ph; cScale = sc; cDark = Theme.dark;
            cache = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = Gfx.g2(cache.createGraphics());
            g.scale(sc, sc);
            int h = fullH - 6;
            Gfx.shadow(g, 0, 0, w, h, 16, 10, 4);
            g.setColor(Theme.surface());
            g.fill(Gfx.rr(0, 0, w, h, 16));
            g.setColor(Theme.accentSoft());
            g.fill(Gfx.rr(18, (h - 48) / 2f, 48, 48, 14));
            icon.paint(g, 30, (h - 24) / 2f, 24, Theme.accent());
            g.setFont(Theme.font(Font.PLAIN, 13f));
            g.setColor(Theme.muted());
            g.drawString(label, 82, h / 2 - 8);
            g.dispose();
        }

        @Override protected void paintComponent(Graphics g0) {
            int w = getWidth(), fullH = getHeight();
            if (w <= 0 || fullH <= 6) return;
            Graphics2D g = Gfx.g2(g0);
            double sc = g.getTransform().getScaleX();
            if (sc <= 0) sc = 1;
            int pw = (int) Math.ceil(w * sc), ph = (int) Math.ceil(fullH * sc);
            if (cache == null || cPW != pw || cPH != ph || cScale != sc || cDark != Theme.dark) {
                buildCache(w, fullH, sc);
            }
            g.drawImage(cache, 0, 0, w, fullH, null);

            int h = fullH - 6;
            g.setFont(Theme.font(Font.BOLD, 28f));
            g.setColor(Theme.text());
            g.drawString(String.valueOf(Math.round(shown)), 82, h / 2 + 26);
            g.dispose();
        }
    }
}