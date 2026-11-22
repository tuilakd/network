package chatwave.screens;

import chatwave.core.*;
import chatwave.core.Models.*;
import chatwave.ui.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Màn hình chính của người dùng: thanh bên (bạn bè / nhóm / lời mời) + khung chat. */
public class MainPanel extends Bg {
    private final Nav nav;
    private final ChatView chat;
    private final ItemList list = new ItemList();
    private final SegTabs tabs = new SegTabs("Tin nhắn", "Nhóm", "Lời mời");
    private final FField search = new FField("Tìm kiếm", Icons.SEARCH, false);
    private final javax.swing.Timer poll;
    private final SideHeader head = new SideHeader();

    private List<Friend> friends = new ArrayList<Friend>();
    private List<Req> reqs = new ArrayList<Req>();
    private List<Room> mine = new ArrayList<Room>(), all = new ArrayList<Room>();
    private boolean loading, firstFill = true;
    private int lastTab = -1;

    public MainPanel(Nav nav) {
        super(Kind.BG, new BorderLayout());
        this.nav = nav;
        chat = new ChatView(nav, new Runnable() { public void run() { refresh(); } });

        Bg side = new Bg(Kind.SIDE, new BorderLayout());
        side.setPreferredSize(new Dimension(340, 10));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        head.setAlignmentX(0f);
        top.add(head);
        JPanel sp = new JPanel(new BorderLayout());
        sp.setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        search.setPreferredSize(new Dimension(100, 44));
        sp.add(search, BorderLayout.CENTER);
        sp.setAlignmentX(0f);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        top.add(sp);
        JPanel tp = new JPanel(new BorderLayout());
        tp.setOpaque(false);
        tp.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        tabs.setPreferredSize(new Dimension(100, 38));
        tp.add(tabs, BorderLayout.CENTER);
        tp.setAlignmentX(0f);
        tp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        top.add(tp);
        side.add(top, BorderLayout.NORTH);
        JScrollPane sc = ModernScroll.wrap(list);
        side.add(sc, BorderLayout.CENTER);
        add(side, BorderLayout.WEST);
        add(chat, BorderLayout.CENTER);

        search.getInput().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuild(false); }
            public void removeUpdate(DocumentEvent e) { rebuild(false); }
            public void changedUpdate(DocumentEvent e) { rebuild(false); }
        });
        tabs.setListener(new SegTabs.Listener() { public void changed(int i) { rebuild(true); } });
        list.setListener(new ItemList.Listener() {
            public void clicked(ItemList.Row r) {
                if (r.id.startsWith("u:")) { list.setSelectedId(r.id); chat.openDm(r.id.substring(2)); }
                else if (r.id.startsWith("r:") && r.tag instanceof Room) {
                    Room rm = (Room) r.tag;
                    for (Room m : mine) if (m.id == rm.id) { list.setSelectedId(r.id); chat.openRoom(rm); return; }
                }
            }
        });
        poll = new javax.swing.Timer(3000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { refresh(); chat.poll(false); }
        });
    }

    public void start() { refresh(); poll.start(); }
    public void stop() { poll.stop(); }

    /* ---------- dữ liệu ---------- */
    private void refresh() {
        if (loading) return;
        loading = true;
        Async.run(new java.util.concurrent.Callable<Object[]>() {
            public Object[] call() throws Exception {
                Backend b = Session.backend;
                return new Object[]{b.friends(), b.pendingRequests(), b.myRooms(), b.allRooms()};
            }
        }, new Async.Ok<Object[]>() {
            @SuppressWarnings("unchecked") public void accept(Object[] d) {
                loading = false;
                friends = (List<Friend>) d[0]; reqs = (List<Req>) d[1]; mine = (List<Room>) d[2]; all = (List<Room>) d[3];
                rebuild(false);
            }
        }, new Async.Fail() { public void accept(Exception e) { loading = false; } });
    }

    private static String time(long t) {
        if (t <= 0) return "";
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        b.setTimeInMillis(t);
        boolean today = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        return new SimpleDateFormat(today ? "HH:mm" : "dd/MM").format(new Date(t));
    }

    private void rebuild(boolean animate) {
        int tab = tabs.getSelected();
        String q = search.getText().trim().toLowerCase();
        int unread = 0;
        for (Friend f : friends) unread += f.unread;
        tabs.setBadge(0, unread);
        tabs.setBadge(2, reqs.size());
        List<ItemList.Row> rows = new ArrayList<ItemList.Row>();
        if (tab == 0) {
            List<Friend> sorted = new ArrayList<Friend>(friends);
            Collections.sort(sorted, new Comparator<Friend>() { public int compare(Friend a, Friend b) { return Long.compare(b.lastAt, a.lastAt); } });
            boolean exact = false;
            for (Friend f : sorted) {
                if (f.username.equalsIgnoreCase(q)) exact = true;
                if (!q.isEmpty() && !f.username.toLowerCase().contains(q)) continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "u:" + f.username; r.title = f.username;
                r.sub = f.lastMessage == null || f.lastMessage.isEmpty() ? "Hãy gửi lời chào" : (Session.me().equals(f.lastSender) ? "Bạn: " : "") + f.lastMessage;
                r.time = time(f.lastAt); r.badge = f.unread;
                rows.add(r);
            }
            if (!q.isEmpty() && !exact && !q.equalsIgnoreCase(Session.me())) {
                rows.add(ItemList.Row.header("Thêm bạn mới"));
                final String name = search.getText().trim();
                ItemList.Row r = new ItemList.Row();
                r.id = "add:" + name; r.title = name; r.sub = "Gửi lời mời kết bạn";
                r.btns = new ItemList.Btn[]{ new ItemList.Btn("Kết bạn", FButton.Kind.PRIMARY, new Runnable() { public void run() { addFriend(name); } }) };
                rows.add(r);
            }
            list.setEmptyText(q.isEmpty() ? "Chưa có bạn bè.\nGõ tên đăng nhập vào ô tìm kiếm để kết bạn." : "Không tìm thấy");
        } else if (tab == 1) {
            Set<Long> mineIds = new HashSet<Long>();
            for (Room r : mine) mineIds.add(r.id);
            boolean any = false;
            for (Room rm : mine) {
                if (!q.isEmpty() && !rm.name.toLowerCase().contains(q)) continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "r:" + rm.id; r.title = rm.name; r.sub = "Nhóm của bạn"; r.group = true; r.tag = rm;
                rows.add(r); any = true;
            }
            List<ItemList.Row> other = new ArrayList<ItemList.Row>();
            for (final Room rm : all) {
                if (mineIds.contains(rm.id)) continue;
                if (!q.isEmpty() && !rm.name.toLowerCase().contains(q)) continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "x:" + rm.id; r.title = rm.name; r.sub = "Tạo bởi " + rm.createdBy; r.group = true;
                r.btns = new ItemList.Btn[]{ new ItemList.Btn("Tham gia", FButton.Kind.SOFT, new Runnable() { public void run() { join(rm); } }) };
                other.add(r);
            }
            if (!other.isEmpty()) { rows.add(ItemList.Row.header("Khám phá")); rows.addAll(other); }
            list.setEmptyText("Chưa có nhóm nào.\nBấm nút + phía trên để tạo nhóm mới.");
        } else {
            for (final Req rq : reqs) {
                if (!q.isEmpty() && !rq.requester.toLowerCase().contains(q)) continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "q:" + rq.id; r.title = rq.requester; r.sub = "Muốn kết bạn với bạn"; r.time = time(rq.at);
                r.btns = new ItemList.Btn[]{
                    new ItemList.Btn("Đồng ý", FButton.Kind.PRIMARY, new Runnable() { public void run() { answer(rq, true); } }),
                    new ItemList.Btn("Từ chối", FButton.Kind.SUBTLE, new Runnable() { public void run() { answer(rq, false); } })
                };
                rows.add(r);
            }
            list.setEmptyText("Không có lời mời nào");
        }
        boolean anim = animate || firstFill || lastTab != tab;
        firstFill = false; lastTab = tab;
        list.setRows(rows, anim);
        if (chat.currentDm() != null) list.setSelectedId("u:" + chat.currentDm());
    }

    private void addFriend(final String name) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception { Session.backend.sendRequest(name); return true; }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) { nav.overlay().toast("Đã gửi lời mời tới " + name, false); search.setText(""); refresh(); }
        }, new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
    }

    private void answer(final Req rq, final boolean ok) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception { if (ok) Session.backend.accept(rq.id); else Session.backend.decline(rq.id); return true; }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                reqs.remove(rq);
                nav.overlay().toast(ok ? "Đã kết bạn với " + rq.requester : "Đã từ chối", false);
                refresh(); rebuild(false);
            }
        }, new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
    }

    private void join(final Room rm) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception { Session.backend.joinRoom(rm.id); return true; }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) { nav.overlay().toast("Đã tham gia " + rm.name, false); refresh(); }
        }, new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
    }

    private void createRoom() {
        Dialogs.prompt(nav.overlay(), "Tạo nhóm mới", "Tên nhóm", "Tạo nhóm", new Dialogs.TextOk() {
            public void accept(final String name) {
                Async.run(new java.util.concurrent.Callable<Room>() {
                    public Room call() throws Exception { return Session.backend.createRoom(name); }
                }, new Async.Ok<Room>() {
                    public void accept(Room r) {
                        mine.add(r); all.add(r);
                        tabs.select(1, false);
                        rebuild(true);
                        list.setSelectedId("r:" + r.id);
                        chat.openRoom(r);
                        refresh();
                    }
                }, new Async.Fail() { public void accept(Exception e) { nav.overlay().toast(e.getMessage(), true); } });
            }
        });
    }

    private void logout() {
        Dialogs.confirm(nav.overlay(), "Đăng xuất", "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", true, new Runnable() {
            public void run() {
                try { Session.backend.logout(); } catch (Exception ignored) { }
                nav.showAuth();
            }
        });
    }

    private void profile() {
        final Overlay ov = nav.overlay();
        Async.run(new java.util.concurrent.Callable<Profile>() {
            public Profile call() throws Exception { return Session.backend.profile(Session.me()); }
        }, new Async.Ok<Profile>() {
            public void accept(Profile p) { showProfile(p); }
        }, new Async.Fail() { public void accept(Exception e) { ov.toast(e.getMessage(), true); } });
    }

    private void showProfile(Profile p) {
        final Overlay ov = nav.overlay();
        Card c = new Card(26);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        final String me = Session.me();
        JComponent av = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                Avatar.paint(g, me, (getWidth() - 84) / 2f, 0, 84, false, false);
                g.dispose();
            }
        };
        av.setPreferredSize(new Dimension(340, 84)); av.setMaximumSize(new Dimension(340, 84)); av.setAlignmentX(0f);
        c.add(av);
        c.add(Box.createVerticalStrut(10));
        Dialogs.Label un = new Dialogs.Label("@" + me + (p.email == null || p.email.isEmpty() ? "" : "  ·  " + p.email), Theme.font(Font.PLAIN, 13.5f), true, true, 340);
        un.setAlignmentX(0f);
        c.add(un);
        c.add(Box.createVerticalStrut(16));
        final FField name = new FField("Họ và tên", Icons.USER, false);
        final FField bio = new FField("Giới thiệu bản thân", Icons.INFO, false);
        name.setText(p.fullName == null ? "" : p.fullName);
        bio.setText(p.bio == null ? "" : p.bio);
        for (FField f : new FField[]{name, bio}) { f.setPreferredSize(new Dimension(340, 46)); f.setMaximumSize(new Dimension(340, 46)); f.setAlignmentX(0f); c.add(f); c.add(Box.createVerticalStrut(10)); }
        c.add(Box.createVerticalStrut(8));
        final FButton save = new FButton("Lưu thay đổi", Icons.CHECK, FButton.Kind.PRIMARY, null);
        save.setAction(new Runnable() {
            public void run() {
                save.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Profile>() {
                    public Profile call() throws Exception { return Session.backend.updateMyProfile(name.getText().trim(), bio.getText().trim()); }
                }, new Async.Ok<Profile>() {
                    public void accept(Profile np) { save.setLoading(false); ov.close(); ov.toast("Đã lưu hồ sơ", false); }
                }, new Async.Fail() { public void accept(Exception e) { save.setLoading(false); ov.toast(e.getMessage(), true); } });
            }
        });
        save.setPreferredSize(new Dimension(340, 44)); save.setMaximumSize(new Dimension(340, 44)); save.setAlignmentX(0f);
        c.add(save);
        ov.show(c);
    }

    /* ---------- tiêu đề thanh bên ---------- */
    private final class SideHeader extends JComponent {
        private final FButton theme, add, out;
        private boolean overAvatar;
        SideHeader() {
            setLayout(null);
            setPreferredSize(new Dimension(340, 74));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
            theme = FButton.icon(Theme.dark ? Icons.SUN : Icons.MOON, "Sáng / tối", new Runnable() { public void run() { nav.toggleTheme(); theme.setIcon(Theme.dark ? Icons.SUN : Icons.MOON); } });
            add = FButton.icon(Icons.PLUS, "Tạo nhóm", new Runnable() { public void run() { createRoom(); } });
            out = FButton.icon(Icons.LOGOUT, "Đăng xuất", new Runnable() { public void run() { logout(); } });
            add(theme); add(add); add(out);
            setCursor(Cursor.getDefaultCursor());
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mousePressed(java.awt.event.MouseEvent e) { if (e.getX() < 180) profile(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { overAvatar = false; repaint(); }
            });
            addMouseMotionListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                    boolean o = e.getX() < 180;
                    if (o != overAvatar) { overAvatar = o; setCursor(Cursor.getPredefinedCursor(o ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); repaint(); }
                }
            });
        }
        @Override public void doLayout() {
            int y = (getHeight() - 38) / 2;
            out.setBounds(getWidth() - 14 - 38, y, 38, 38);
            add.setBounds(getWidth() - 14 - 38 * 2 - 4, y, 38, 38);
            theme.setBounds(getWidth() - 14 - 38 * 3 - 8, y, 38, 38);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.g2(g0);
            if (overAvatar) { g.setColor(Theme.hover()); g.fill(Gfx.rr(8, 8, 180, getHeight() - 16, 14)); }
            Avatar.paint(g, Session.me(), 16, 17, 40, true, false);
            g.setFont(Theme.font(Font.BOLD, 15f));
            g.setColor(Theme.text());
            g.drawString(Gfx.ellipsize(Session.me(), g.getFontMetrics(), 110), 66, 35);
            g.setFont(Theme.font(Font.PLAIN, 12f));
            g.setColor(Theme.muted());
            g.drawString("Hồ sơ của bạn", 66, 53);
            g.dispose();
        }
    }
}
