package chatwave.screens;

import chatwave.core.*;
import chatwave.core.Models.*;
import chatwave.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Màn hình chính của người dùng: thanh bên (bạn bè / nhóm / lời mời) + khung
 * chat.
 */
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
    private String myAvatarUrl = "";

    public MainPanel(Nav nav) {
        super(Kind.BG, new BorderLayout());
        this.nav = nav;
        chat = new ChatView(nav, new Runnable() {
            public void run() {
                refresh();
            }
        });

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
            public void insertUpdate(DocumentEvent e) {
                rebuild(false);
            }

            public void removeUpdate(DocumentEvent e) {
                rebuild(false);
            }

            public void changedUpdate(DocumentEvent e) {
                rebuild(false);
            }
        });
        tabs.setListener(new SegTabs.Listener() {
            public void changed(int i) {
                rebuild(true);
            }
        });
        list.setListener(new ItemList.Listener() {
            public void clicked(ItemList.Row r) {
                if (r.id.startsWith("u:")) {
                    list.setSelectedId(r.id);
                    chat.openDm(r.id.substring(2));
                } else if (r.id.startsWith("r:") && r.tag instanceof Room) {
                    Room rm = (Room) r.tag;
                    for (Room m : mine)
                        if (m.id == rm.id) {
                            list.setSelectedId(r.id);
                            chat.openRoom(rm);
                            return;
                        }
                }
            }
        });
        poll = new javax.swing.Timer(3000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
                chat.poll(false);
            }
        });
    }

    public void start() {
        refresh();
        poll.start();
    }

    public void stop() {
        poll.stop();
    }

    /* ---------- dữ liệu ---------- */
    private void refresh() {
        if (loading)
            return;
        loading = true;
        Async.run(new java.util.concurrent.Callable<Object[]>() {
            public Object[] call() throws Exception {
                Backend b = Session.backend;
                return new Object[] { b.friends(), b.pendingRequests(), b.myRooms(), b.allRooms() };
            }
        }, new Async.Ok<Object[]>() {
            @SuppressWarnings("unchecked")
            public void accept(Object[] d) {
                loading = false;
                friends = (List<Friend>) d[0];
                reqs = (List<Req>) d[1];
                mine = (List<Room>) d[2];
                all = (List<Room>) d[3];
                rebuild(false);
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                loading = false;
            }
        });
    }

    private static String time(long t) {
        if (t <= 0)
            return "";
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        b.setTimeInMillis(t);
        boolean today = a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        return new SimpleDateFormat(today ? "HH:mm" : "dd/MM").format(new Date(t));
    }

    private void rebuild(boolean animate) {
        int tab = tabs.getSelected();
        String q = search.getText().trim().toLowerCase();
        int unread = 0;
        for (Friend f : friends)
            unread += f.unread;
        tabs.setBadge(0, unread);
        tabs.setBadge(2, reqs.size());
        List<ItemList.Row> rows = new ArrayList<ItemList.Row>();
        if (tab == 0) {
            List<Friend> sorted = new ArrayList<Friend>(friends);
            Collections.sort(sorted, new Comparator<Friend>() {
                public int compare(Friend a, Friend b) {
                    return Long.compare(b.lastAt, a.lastAt);
                }
            });
            boolean exact = false;
            for (Friend f : sorted) {
                if (f.username.equalsIgnoreCase(q))
                    exact = true;
                if (!q.isEmpty() && !f.username.toLowerCase().contains(q))
                    continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "u:" + f.username;
                r.title = f.username;
                r.sub = f.lastMessage == null || f.lastMessage.isEmpty() ? "Hãy gửi lời chào"
                        : (Session.me().equals(f.lastSender) ? "Bạn: " : "") + f.lastMessage;
                r.time = time(f.lastAt);
                r.badge = f.unread;
                rows.add(r);
            }
            if (!q.isEmpty() && !exact && !q.equalsIgnoreCase(Session.me())) {
                rows.add(ItemList.Row.header("Thêm bạn mới"));
                final String name = search.getText().trim();
                ItemList.Row r = new ItemList.Row();
                r.id = "add:" + name;
                r.title = name;
                r.sub = "Gửi lời mời kết bạn";
                r.btns = new ItemList.Btn[] { new ItemList.Btn("Kết bạn", FButton.Kind.PRIMARY, new Runnable() {
                    public void run() {
                        addFriend(name);
                    }
                }) };
                rows.add(r);
            }
            list.setEmptyText(
                    q.isEmpty() ? "Chưa có bạn bè.\nGõ tên đăng nhập vào ô tìm kiếm để kết bạn." : "Không tìm thấy");
            list.setEmptyButton(null);
        } else if (tab == 1) {
            Set<Long> mineIds = new HashSet<Long>();
            for (Room r : mine)
                mineIds.add(r.id);
            boolean any = false;
            for (Room rm : mine) {
                if (!q.isEmpty() && !rm.name.toLowerCase().contains(q))
                    continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "r:" + rm.id;
                r.title = rm.name;
                r.sub = "Nhóm của bạn";
                r.group = true;
                r.tag = rm;
                rows.add(r);
                any = true;
            }
            List<ItemList.Row> other = new ArrayList<ItemList.Row>();
            for (final Room rm : all) {
                if (mineIds.contains(rm.id))
                    continue;
                if (!q.isEmpty() && !rm.name.toLowerCase().contains(q))
                    continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "x:" + rm.id;
                r.title = rm.name;
                r.sub = "Tạo bởi " + rm.createdBy;
                r.group = true;
                r.btns = new ItemList.Btn[] { new ItemList.Btn("Tham gia", FButton.Kind.SOFT, new Runnable() {
                    public void run() {
                        join(rm);
                    }
                }) };
                other.add(r);
            }
            if (!other.isEmpty()) {
                rows.add(ItemList.Row.header("Khám phá"));
                rows.addAll(other);
            }
            list.setEmptyText(q.isEmpty() ? "Chưa có nhóm nào.\nTạo nhóm mới để trò chuyện cùng bạn bè."
                    : "Không tìm thấy nhóm nào");
            list.setEmptyButton(q.isEmpty() ? new ItemList.Btn("Tạo nhóm", FButton.Kind.PRIMARY, new Runnable() {
                public void run() {
                    createRoom();
                }
            }) : null);
        } else {
            for (final Req rq : reqs) {
                if (!q.isEmpty() && !rq.requester.toLowerCase().contains(q))
                    continue;
                ItemList.Row r = new ItemList.Row();
                r.id = "q:" + rq.id;
                r.title = rq.requester;
                r.sub = "Muốn kết bạn với bạn";
                r.time = time(rq.at);
                r.btns = new ItemList.Btn[] {
                        new ItemList.Btn("Đồng ý", FButton.Kind.PRIMARY, new Runnable() {
                            public void run() {
                                answer(rq, true);
                            }
                        }),
                        new ItemList.Btn("Từ chối", FButton.Kind.SUBTLE, new Runnable() {
                            public void run() {
                                answer(rq, false);
                            }
                        })
                };
                rows.add(r);
            }
            list.setEmptyText("Không có lời mời nào");
            list.setEmptyButton(null);
        }
        boolean anim = animate || firstFill || lastTab != tab;
        firstFill = false;
        lastTab = tab;
        list.setRows(rows, anim);
        if (chat.currentDm() != null)
            list.setSelectedId("u:" + chat.currentDm());
    }

    private void addFriend(final String name) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                Session.backend.sendRequest(name);
                return true;
            }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                nav.overlay().toast("Đã gửi lời mời tới " + name, false);
                search.setText("");
                refresh();
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                nav.overlay().toast(e.getMessage(), true);
            }
        });
    }

    private void answer(final Req rq, final boolean ok) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                if (ok)
                    Session.backend.accept(rq.id);
                else
                    Session.backend.decline(rq.id);
                return true;
            }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                reqs.remove(rq);
                nav.overlay().toast(ok ? "Đã kết bạn với " + rq.requester : "Đã từ chối", false);
                refresh();
                rebuild(false);
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                nav.overlay().toast(e.getMessage(), true);
            }
        });
    }

    private void join(final Room rm) {
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                Session.backend.joinRoom(rm.id);
                return true;
            }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                nav.overlay().toast("Đã tham gia " + rm.name, false);
                refresh();
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                nav.overlay().toast(e.getMessage(), true);
            }
        });
    }

    private void createRoom() {
        final Overlay ov = nav.overlay();
        final Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "close");
        c.getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ov.close();
            }
        });

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        headRow.setMaximumSize(new Dimension(360, 32));
        headRow.setAlignmentX(0f);
        Dialogs.Label title = new Dialogs.Label("Tạo nhóm mới", Theme.font(Font.BOLD, 18f), false, false, 300);
        headRow.add(title, BorderLayout.WEST);
        headRow.add(FButton.icon(Icons.CLOSE, "Đóng", new Runnable() {
            public void run() {
                ov.close();
            }
        }), BorderLayout.EAST);
        c.add(headRow);
        c.add(Box.createVerticalStrut(12));

        final FField nameField = new FField("Tên nhóm", Icons.GROUP, false);
        nameField.setPreferredSize(new Dimension(360, 44));
        nameField.setMaximumSize(new Dimension(360, 44));
        nameField.setAlignmentX(0f);
        c.add(nameField);

        final JLabel nameErr = new JLabel(" ");
        nameErr.setFont(Theme.font(Font.PLAIN, 11.5f));
        nameErr.setForeground(Theme.danger());
        nameErr.setAlignmentX(0f);
        c.add(Box.createVerticalStrut(4));
        c.add(nameErr);
        c.add(Box.createVerticalStrut(8));

        final Set<String> selectedSet = new LinkedHashSet<String>();
        final JLabel countLbl = new JLabel("Chọn thành viên (đã chọn 0)");
        countLbl.setFont(Theme.font(Font.BOLD, 13f));
        countLbl.setForeground(Theme.muted());
        countLbl.setAlignmentX(0f);
        c.add(countLbl);
        c.add(Box.createVerticalStrut(6));

        final FField filterField = new FField("Lọc bạn bè theo tên...", Icons.SEARCH, false);
        filterField.setPreferredSize(new Dimension(360, 40));
        filterField.setMaximumSize(new Dimension(360, 40));
        filterField.setAlignmentX(0f);
        c.add(filterField);
        c.add(Box.createVerticalStrut(8));

        final JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane sp = ModernScroll.wrap(listPanel);
        sp.setPreferredSize(new Dimension(360, Math.min(240, Math.max(80, friends.size() * 48))));
        sp.setMaximumSize(new Dimension(360, 240));
        sp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));
        sp.setAlignmentX(0f);
        c.add(sp);

        final JLabel memberErr = new JLabel(" ");
        memberErr.setFont(Theme.font(Font.PLAIN, 11.5f));
        memberErr.setForeground(Theme.danger());
        memberErr.setAlignmentX(0f);
        c.add(Box.createVerticalStrut(4));
        c.add(memberErr);
        c.add(Box.createVerticalStrut(12));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.setMaximumSize(new Dimension(360, 44));
        FButton cancel = new FButton("Hủy", FButton.Kind.GHOST, new Runnable() {
            public void run() {
                ov.close();
            }
        });
        final FButton create = new FButton("Tạo nhóm", Icons.CHECK, FButton.Kind.PRIMARY, null);
        create.setEnabled(false);

        final Runnable updateState = new Runnable() {
            public void run() {
                String n = nameField.getText().trim();
                countLbl.setText("Chọn thành viên (đã chọn " + selectedSet.size() + ")");
                boolean okName = n.length() >= 2;
                boolean okMembers = !selectedSet.isEmpty();
                nameErr.setText(n.isEmpty() || okName ? " " : "Tên nhóm tối thiểu 2 ký tự");
                memberErr.setText(okMembers || friends.isEmpty() ? " " : "Chọn ít nhất 1 thành viên");
                create.setEnabled(okName && okMembers);
            }
        };

        final Runnable rebuildRows = new Runnable() {
            public void run() {
                listPanel.removeAll();
                String q = filterField.getText().trim().toLowerCase();
                int shown = 0;
                for (final Friend f : friends) {
                    if (!q.isEmpty() && !f.username.toLowerCase().contains(q))
                        continue;
                    shown++;
                    final boolean[] hov = { false };
                    JPanel row = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g0) {
                            Graphics2D g = Gfx.g2(g0);
                            if (hov[0]) {
                                g.setColor(Theme.hover());
                                g.fillRect(0, 0, getWidth(), getHeight());
                            }
                            Avatar.paint(g, f.username, 10, 8, 32, false, false);
                            g.setFont(Theme.font(Font.PLAIN, 14f));
                            g.setColor(Theme.text());
                            FontMetrics fm = g.getFontMetrics();
                            g.drawString(Gfx.ellipsize(f.username, fm, getWidth() - 90), 52,
                                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                            // Custom checkbox (vuông bo góc 4px, tick teal khi chọn)
                            int cx = getWidth() - 30, cy = (getHeight() - 18) / 2;
                            boolean sel = selectedSet.contains(f.username);
                            if (sel) {
                                g.setColor(Theme.accent());
                                g.fill(Gfx.rr(cx, cy, 18, 18, 4));
                                g.setColor(Theme.onAccent());
                                Icons.CHECK.paint(g, cx + 2, cy + 2, 14, Theme.onAccent());
                            } else {
                                g.setColor(Theme.border());
                                g.draw(Gfx.rr(cx, cy, 18, 18, 4));
                            }
                            g.dispose();
                        }
                    };
                    row.setOpaque(false);
                    row.setPreferredSize(new Dimension(340, 48));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                    row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    row.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            hov[0] = true;
                            row.repaint();
                        }

                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            hov[0] = false;
                            row.repaint();
                        }

                        @Override
                        public void mousePressed(java.awt.event.MouseEvent e) {
                            if (selectedSet.contains(f.username))
                                selectedSet.remove(f.username);
                            else
                                selectedSet.add(f.username);
                            row.repaint();
                            updateState.run();
                        }
                    });
                    listPanel.add(row);
                }
                if (shown == 0) {
                    JLabel empty = new JLabel(
                            friends.isEmpty() ? "Bạn chưa có bạn bè nào" : "Không tìm thấy bạn bè phù hợp",
                            SwingConstants.CENTER);
                    empty.setFont(Theme.font(Font.PLAIN, 13f));
                    empty.setForeground(Theme.muted());
                    empty.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
                    empty.setAlignmentX(0.5f);
                    listPanel.add(empty);
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        };

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateState.run();
            }

            public void removeUpdate(DocumentEvent e) {
                updateState.run();
            }

            public void changedUpdate(DocumentEvent e) {
                updateState.run();
            }
        };
        nameField.getInput().getDocument().addDocumentListener(dl);
        filterField.getInput().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                rebuildRows.run();
            }

            public void removeUpdate(DocumentEvent e) {
                rebuildRows.run();
            }

            public void changedUpdate(DocumentEvent e) {
                rebuildRows.run();
            }
        });

        rebuildRows.run();
        create.setAction(new Runnable() {
            public void run() {
                if (!create.isEnabled())
                    return;
                final String roomName = nameField.getText().trim();
                final List<String> selected = new ArrayList<String>(selectedSet);
                create.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Room>() {
                    public Room call() throws Exception {
                        return Session.backend.createRoom(roomName, selected);
                    }
                }, new Async.Ok<Room>() {
                    public void accept(Room r) {
                        create.setLoading(false);
                        ov.close();
                        mine.add(r);
                        all.add(r);
                        tabs.select(1, false);
                        rebuild(true);
                        list.setSelectedId("r:" + r.id);
                        chat.openRoom(r);
                        refresh();
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        create.setLoading(false);
                        nameErr.setText(e.getMessage());
                    }
                });
            }
        });
        btns.add(cancel);
        btns.add(create);
        c.add(btns);
        ov.show(c);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nameField.requestFocus();
            }
        });
    }

    private void showAddFriendDialog() {
        final Overlay ov = nav.overlay();
        final Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "close");
        c.getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ov.close();
            }
        });

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        headRow.setMaximumSize(new Dimension(360, 32));
        headRow.setAlignmentX(0f);
        headRow.add(new Dialogs.Label("Thêm bạn", Theme.font(Font.BOLD, 18f), false, false, 300), BorderLayout.WEST);
        headRow.add(FButton.icon(Icons.CLOSE, "Đóng", new Runnable() {
            public void run() {
                ov.close();
            }
        }), BorderLayout.EAST);
        c.add(headRow);
        c.add(Box.createVerticalStrut(12));

        final FField queryField = new FField("Nhập tên người dùng", Icons.SEARCH, false);
        queryField.setPreferredSize(new Dimension(360, 44));
        queryField.setMaximumSize(new Dimension(360, 44));
        queryField.setAlignmentX(0f);
        c.add(queryField);
        c.add(Box.createVerticalStrut(12));

        final JPanel resultBox = new JPanel(new BorderLayout());
        resultBox.setOpaque(false);
        resultBox.setPreferredSize(new Dimension(360, 84));
        resultBox.setMaximumSize(new Dimension(360, 120));
        resultBox.setAlignmentX(0f);
        c.add(resultBox);

        final Set<String> friendUsernames = new HashSet<String>();
        for (Friend f : friends)
            friendUsernames.add(f.username.toLowerCase());

        final Runnable showStatus = new Runnable() {
            public void run() {
            }
        };
        final javax.swing.Timer[] debounce = { null };
        final String[] lastQuery = { "" };

        final java.util.function.Consumer<String> renderEmpty = new java.util.function.Consumer<String>() {
            public void accept(String msg) {
                resultBox.removeAll();
                JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
                lbl.setFont(Theme.font(Font.PLAIN, 13.5f));
                lbl.setForeground(Theme.muted());
                resultBox.add(lbl, BorderLayout.CENTER);
                resultBox.revalidate();
                resultBox.repaint();
            }
        };

        renderEmpty.accept("Nhập tên người dùng để tìm");

        final Runnable doSearch = new Runnable() {
            public void run() {
                final String q = queryField.getText().trim();
                lastQuery[0] = q;
                if (q.isEmpty()) {
                    renderEmpty.accept("Nhập tên người dùng để tìm");
                    return;
                }
                renderEmpty.accept("Đang tìm...");
                Async.run(new java.util.concurrent.Callable<Profile>() {
                    public Profile call() throws Exception {
                        return Session.backend.profile(q);
                    }
                }, new Async.Ok<Profile>() {
                    public void accept(final Profile p) {
                        if (!q.equals(lastQuery[0]))
                            return;
                        resultBox.removeAll();
                        JPanel row = new JPanel(new BorderLayout(10, 0));
                        row.setOpaque(false);
                        row.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Theme.border(), 1),
                                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

                        JComponent av = new JComponent() {
                            @Override
                            protected void paintComponent(Graphics g0) {
                                Graphics2D g = Gfx.g2(g0);
                                Avatar.paint(g, p.username, p.avatarUrl, 0, 2, 36, false, false, null);
                                g.dispose();
                            }
                        };
                        av.setPreferredSize(new Dimension(38, 40));

                        JPanel info = new JPanel();
                        info.setOpaque(false);
                        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                        JLabel disp = new JLabel(p.display());
                        disp.setFont(Theme.font(Font.BOLD, 14f));
                        disp.setForeground(Theme.text());
                        JLabel uname = new JLabel("@" + p.username);
                        uname.setFont(Theme.font(Font.PLAIN, 12f));
                        uname.setForeground(Theme.muted());
                        info.add(disp);
                        info.add(uname);

                        boolean isMe = p.username.equalsIgnoreCase(Session.me());
                        boolean isFriend = friendUsernames.contains(p.username.toLowerCase());
                        final FButton actionBtn = new FButton(isMe ? "Bạn" : isFriend ? "Bạn bè" : "Kết bạn",
                                isMe || isFriend ? FButton.Kind.SUBTLE : FButton.Kind.PRIMARY, null);
                        actionBtn.setPreferredSize(new Dimension(96, 34));
                        if (isMe || isFriend) {
                            actionBtn.setEnabled(false);
                        } else {
                            actionBtn.setAction(new Runnable() {
                                public void run() {
                                    actionBtn.setLoading(true);
                                    Async.run(new java.util.concurrent.Callable<Boolean>() {
                                        public Boolean call() throws Exception {
                                            Session.backend.sendRequest(p.username);
                                            return true;
                                        }
                                    }, new Async.Ok<Boolean>() {
                                        public void accept(Boolean b) {
                                            actionBtn.setLoading(false);
                                            actionBtn.setText("Đã gửi");
                                            actionBtn.setEnabled(false);
                                            ov.toast("Đã gửi lời mời tới " + p.username, false);
                                        }
                                    }, new Async.Fail() {
                                        public void accept(Exception e) {
                                            actionBtn.setLoading(false);
                                            ov.toast(e.getMessage(), true);
                                        }
                                    });
                                }
                            });
                        }

                        row.add(av, BorderLayout.WEST);
                        row.add(info, BorderLayout.CENTER);
                        row.add(actionBtn, BorderLayout.EAST);
                        resultBox.add(row, BorderLayout.CENTER);
                        resultBox.revalidate();
                        resultBox.repaint();
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        if (!q.equals(lastQuery[0]))
                            return;
                        renderEmpty.accept("Không tìm thấy người dùng nào");
                    }
                });
            }
        };

        queryField.getInput().getDocument().addDocumentListener(new DocumentListener() {
            private void trigger() {
                if (debounce[0] != null)
                    debounce[0].stop();
                debounce[0] = new javax.swing.Timer(300, new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        doSearch.run();
                    }
                });
                debounce[0].setRepeats(false);
                debounce[0].start();
            }

            public void insertUpdate(DocumentEvent e) {
                trigger();
            }

            public void removeUpdate(DocumentEvent e) {
                trigger();
            }

            public void changedUpdate(DocumentEvent e) {
                trigger();
            }
        });

        ov.show(c);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                queryField.requestFocus();
            }
        });
    }

    private void logout() {
        Dialogs.confirm(nav.overlay(), "Đăng xuất", "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", true,
                new Runnable() {
                    public void run() {
                        try {
                            Session.backend.logout();
                        } catch (Exception ignored) {
                        }
                        nav.showAuth();
                    }
                });
    }

    private void profile() {
        final Overlay ov = nav.overlay();
        Async.run(new java.util.concurrent.Callable<Profile>() {
            public Profile call() throws Exception {
                return Session.backend.profile(Session.me());
            }
        }, new Async.Ok<Profile>() {
            public void accept(Profile p) {
                showProfile(p);
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                ov.toast(e.getMessage(), true);
            }
        });
    }

    private void showProfile(Profile p) {
        final Overlay ov = nav.overlay();
        final Card c = new Card(26);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        final String me = Session.me();
        final String[] pendingAvatarUrl = { p.avatarUrl == null ? "" : p.avatarUrl };
        final BufferedImage[] localPreview = { null };
        myAvatarUrl = pendingAvatarUrl[0];
        head.repaint();

        final JComponent av = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                if (localPreview[0] != null) {
                    Avatar.paintImage(g, localPreview[0], me, (getWidth() - 84) / 2f, 0, 84, false, false);
                } else {
                    Avatar.paint(g, me, pendingAvatarUrl[0], (getWidth() - 84) / 2f, 0, 84, false, false,
                            new Runnable() {
                                public void run() {
                                    repaint();
                                }
                            });
                }
                g.dispose();
            }
        };
        av.setPreferredSize(new Dimension(340, 84));
        av.setMaximumSize(new Dimension(340, 84));
        av.setAlignmentX(0f);
        c.add(av);
        c.add(Box.createVerticalStrut(8));

        final FButton changeAv = new FButton("Đổi ảnh đại diện", FButton.Kind.SUBTLE, null);
        changeAv.setPreferredSize(new Dimension(160, 32));
        changeAv.setMaximumSize(new Dimension(160, 32));
        changeAv.setAlignmentX(0f);
        JPanel avBtnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avBtnWrap.setOpaque(false);
        avBtnWrap.setMaximumSize(new Dimension(340, 34));
        avBtnWrap.setAlignmentX(0f);
        avBtnWrap.add(changeAv);
        c.add(avBtnWrap);
        final JLabel avErr = new JLabel(" ");
        avErr.setFont(Theme.font(Font.PLAIN, 11.5f));
        avErr.setForeground(Theme.danger());
        avErr.setAlignmentX(0f);
        JPanel avErrWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avErrWrap.setOpaque(false);
        avErrWrap.setMaximumSize(new Dimension(340, 18));
        avErrWrap.setAlignmentX(0f);
        avErrWrap.add(avErr);
        c.add(avErrWrap);

        changeAv.setAction(new Runnable() {
            public void run() {
                avErr.setText(" ");
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainPanel.class);
                String lastDir = prefs.get("avatar_last_dir", "");
                Window win = SwingUtilities.getWindowAncestor(MainPanel.this);
                FileDialog fd = (win instanceof Frame)
                        ? new FileDialog((Frame) win, "Chọn ảnh đại diện", FileDialog.LOAD)
                        : new FileDialog((Dialog) win, "Chọn ảnh đại diện", FileDialog.LOAD);
                if (!lastDir.isEmpty())
                    fd.setDirectory(lastDir);
                fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.webp");
                fd.setVisible(true);
                if (fd.getFile() == null)
                    return;
                if (fd.getDirectory() != null)
                    prefs.put("avatar_last_dir", fd.getDirectory());
                final File f = new File(fd.getDirectory(), fd.getFile());
                if (f.length() > 20L * 1024 * 1024) {
                    avErr.setText("Ảnh quá lớn (tối đa 20MB)");
                    return;
                }
                changeAv.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Upload>() {
                    public Upload call() throws Exception {
                        BufferedImage img = ImageIO.read(f);
                        if (img != null)
                            localPreview[0] = img;
                        return Session.backend.upload(f);
                    }
                }, new Async.Ok<Upload>() {
                    public void accept(Upload u) {
                        changeAv.setLoading(false);
                        pendingAvatarUrl[0] = u.url;
                        av.repaint();
                        ov.toast("Đã tải ảnh lên, bấm Lưu để áp dụng", false);
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        changeAv.setLoading(false);
                        avErr.setText(e.getMessage());
                    }
                });
            }
        });

        c.add(Box.createVerticalStrut(8));
        Dialogs.Label un = new Dialogs.Label("@" + me, Theme.font(Font.BOLD, 14f), true, true, 340);
        un.setAlignmentX(0f);
        c.add(un);
        c.add(Box.createVerticalStrut(12));

        FField emailField = new FField("Email", Icons.INFO, false);
        emailField.setText(p.email == null ? "" : p.email);
        emailField.getInput().setEditable(false);
        emailField.getInput().setFocusable(false);
        emailField.getInput().setForeground(Theme.muted());
        emailField.setPreferredSize(new Dimension(340, 46));
        emailField.setMaximumSize(new Dimension(340, 46));
        emailField.setAlignmentX(0f);
        c.add(emailField);
        c.add(Box.createVerticalStrut(10));

        final FField name = new FField("Họ và tên", Icons.USER, false);
        final FField bio = new FField("Giới thiệu (tối đa 100 ký tự)", Icons.INFO, false);
        name.setText(p.fullName == null ? "" : p.fullName);
        String initBio = p.bio == null ? "" : p.bio;
        if (initBio.length() > 100)
            initBio = initBio.substring(0, 100);
        ((AbstractDocument) bio.getInput().getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String str, AttributeSet a) throws BadLocationException {
                if (str == null)
                    return;
                int avail = 100 - fb.getDocument().getLength();
                if (avail > 0)
                    super.insertString(fb, off, str.length() > avail ? str.substring(0, avail) : str, a);
            }

            @Override
            public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a)
                    throws BadLocationException {
                if (str == null) {
                    super.replace(fb, off, len, null, a);
                    return;
                }
                int avail = 100 - (fb.getDocument().getLength() - len);
                if (avail > 0)
                    super.replace(fb, off, len, str.length() > avail ? str.substring(0, avail) : str, a);
            }
        });
        bio.setText(initBio);

        name.setPreferredSize(new Dimension(340, 46));
        name.setMaximumSize(new Dimension(340, 46));
        name.setAlignmentX(0f);
        c.add(name);
        c.add(Box.createVerticalStrut(10));
        bio.setPreferredSize(new Dimension(340, 46));
        bio.setMaximumSize(new Dimension(340, 46));
        bio.setAlignmentX(0f);
        c.add(bio);

        final JLabel bioCount = new JLabel(initBio.length() + "/100");
        bioCount.setFont(Theme.font(Font.PLAIN, 11.5f));
        bioCount.setForeground(Theme.muted());
        bioCount.setAlignmentX(0f);
        bio.getInput().getDocument().addDocumentListener(new DocumentListener() {
            private void upd() {
                bioCount.setText(bio.getText().length() + "/100");
            }

            public void insertUpdate(DocumentEvent e) {
                upd();
            }

            public void removeUpdate(DocumentEvent e) {
                upd();
            }

            public void changedUpdate(DocumentEvent e) {
                upd();
            }
        });
        c.add(Box.createVerticalStrut(4));
        c.add(bioCount);
        c.add(Box.createVerticalStrut(10));

        FButton changePass = new FButton("Đổi mật khẩu", Icons.LOCK, FButton.Kind.SUBTLE, new Runnable() {
            public void run() {
                showChangePassword(ov);
            }
        });
        changePass.setPreferredSize(new Dimension(340, 38));
        changePass.setMaximumSize(new Dimension(340, 38));
        changePass.setAlignmentX(0f);
        c.add(changePass);
        c.add(Box.createVerticalStrut(14));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.setMaximumSize(new Dimension(340, 44));
        FButton cancel = new FButton("Hủy", FButton.Kind.GHOST, new Runnable() {
            public void run() {
                ov.close();
            }
        });
        final FButton save = new FButton("Lưu", Icons.CHECK, FButton.Kind.PRIMARY, null);
        save.setAction(new Runnable() {
            public void run() {
                save.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Profile>() {
                    public Profile call() throws Exception {
                        return Session.backend.updateMyProfile(name.getText().trim(), bio.getText().trim(),
                                pendingAvatarUrl[0]);
                    }
                }, new Async.Ok<Profile>() {
                    public void accept(Profile np) {
                        save.setLoading(false);
                        myAvatarUrl = np.avatarUrl == null ? "" : np.avatarUrl;
                        head.repaint();
                        ov.close();
                        ov.toast("Đã lưu hồ sơ", false);
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        save.setLoading(false);
                        ov.toast(e.getMessage(), true);
                    }
                });
            }
        });
        btns.add(cancel);
        btns.add(save);
        c.add(btns);
        ov.show(c);
    }

    private void showChangePassword(final Overlay ov) {
        Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        Dialogs.Label title = new Dialogs.Label("Đổi mật khẩu", Theme.font(Font.BOLD, 18f), false, false, 340);
        title.setAlignmentX(0f);
        c.add(title);
        c.add(Box.createVerticalStrut(14));

        final FField oldF = new FField("Mật khẩu hiện tại", Icons.LOCK, true);
        final FField newF = new FField("Mật khẩu mới (tối thiểu 6 ký tự)", Icons.LOCK, true);
        final FField cfmF = new FField("Nhập lại mật khẩu mới", Icons.LOCK, true);
        for (FField f : new FField[] { oldF, newF, cfmF }) {
            f.setPreferredSize(new Dimension(340, 46));
            f.setMaximumSize(new Dimension(340, 46));
            f.setAlignmentX(0f);
            c.add(f);
            c.add(Box.createVerticalStrut(10));
        }
        c.add(Box.createVerticalStrut(6));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.setMaximumSize(new Dimension(340, 44));
        FButton cancel = new FButton("Hủy", FButton.Kind.GHOST, new Runnable() {
            public void run() {
                ov.close();
            }
        });
        final FButton ok = new FButton("Cập nhật", Icons.CHECK, FButton.Kind.PRIMARY, null);
        ok.setAction(new Runnable() {
            public void run() {
                final String op = oldF.getText();
                final String np = newF.getText();
                String cp = cfmF.getText();
                if (op.isEmpty()) {
                    oldF.flagError();
                    return;
                }
                if (np.length() < 6) {
                    newF.flagError();
                    ov.toast("Mật khẩu mới tối thiểu 6 ký tự", true);
                    return;
                }
                if (!np.equals(cp)) {
                    cfmF.flagError();
                    ov.toast("Mật khẩu nhập lại không khớp", true);
                    return;
                }
                ok.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Boolean>() {
                    public Boolean call() throws Exception {
                        Session.backend.changePassword(op, np);
                        return true;
                    }
                }, new Async.Ok<Boolean>() {
                    public void accept(Boolean b) {
                        ok.setLoading(false);
                        ov.close();
                        ov.toast("Đổi mật khẩu thành công", false);
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        ok.setLoading(false);
                        ov.toast(e.getMessage(), true);
                    }
                });
            }
        });
        btns.add(cancel);
        btns.add(ok);
        c.add(btns);
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
            theme = FButton.icon(Theme.dark ? Icons.SUN : Icons.MOON, "Sáng / tối", new Runnable() {
                public void run() {
                    nav.toggleTheme();
                    theme.setIcon(Theme.dark ? Icons.SUN : Icons.MOON);
                }
            });
            add = FButton.icon(Icons.PLUS, "Thêm mới", new Runnable() {
                public void run() {
                    showAddMenu();
                }
            });
            out = FButton.icon(Icons.LOGOUT, "Đăng xuất", new Runnable() {
                public void run() {
                    logout();
                }
            });
            add(theme);
            add(add);
            add(out);
            setCursor(Cursor.getDefaultCursor());
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (e.getX() < 180)
                        profile();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    overAvatar = false;
                    repaint();
                }
            });
            addMouseMotionListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    boolean o = e.getX() < 180;
                    if (o != overAvatar) {
                        overAvatar = o;
                        setCursor(Cursor.getPredefinedCursor(o ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
        }

        private void showAddMenu() {
            List<chatwave.ui.PopupMenu.Item> items = new ArrayList<chatwave.ui.PopupMenu.Item>();
            items.add(new chatwave.ui.PopupMenu.Item(Icons.USER_PLUS, "Thêm bạn", new Runnable() {
                public void run() {
                    showAddFriendDialog();
                }
            }));
            items.add(new chatwave.ui.PopupMenu.Item(Icons.USERS_PLUS, "Tạo nhóm", new Runnable() {
                public void run() {
                    createRoom();
                }
            }));
            chatwave.ui.PopupMenu.show(add, 0, add.getHeight() + 4, items);
        }

        @Override
        public void doLayout() {
            int y = (getHeight() - 38) / 2;
            out.setBounds(getWidth() - 14 - 38, y, 38, 38);
            add.setBounds(getWidth() - 14 - 38 * 2 - 4, y, 38, 38);
            theme.setBounds(getWidth() - 14 - 38 * 3 - 8, y, 38, 38);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.g2(g0);
            if (overAvatar) {
                g.setColor(Theme.hover());
                g.fill(Gfx.rr(8, 8, 180, getHeight() - 16, 14));
            }
            Avatar.paint(g, Session.me(), myAvatarUrl, 16, 17, 40, true, false, new Runnable() {
                public void run() {
                    repaint();
                }
            });
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
