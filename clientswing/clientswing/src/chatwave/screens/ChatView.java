package chatwave.screens;

import chatwave.core.*;
import chatwave.core.Models.*;
import chatwave.ui.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Khung trò chuyện bên phải: tiêu đề, tin nhắn, ô soạn, đính kèm ảnh, ngăn
 * thông tin.
 */
public class ChatView extends Bg {
    private final Nav nav;
    private final Runnable onChanged;
    private final MessageList list = new MessageList();
    private final JScrollPane scroll;
    private final FField input = new FField("Nhập tin nhắn (hoặc Ctrl+V dán ảnh)...", null, false);
    private final FButton send, attach, emojiBtn;
    private final JPanel chip = new JPanel(new BorderLayout(8, 0));
    private final JLabel chipName = new JLabel();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final Header header = new Header();
    private final Drawer drawer;
    private final JPanel mid;
    private File attached;
    private final FButton mic;
    private Audio.Recorder recorder;
    private javax.swing.Timer recTimer;
    private final Audio.Player player = new Audio.Player();
    private long playingId = -1;
    // Giới hạn 5 tệp media trong RAM
    private final Map<String, byte[]> mediaCache = new LinkedHashMap<String, byte[]>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> e) {
            return size() > 5;
        }
    };
    private boolean sending;

    private String dmUser; // đang chat riêng với ai
    private Room room; // hoặc đang ở nhóm nào
    private int token;
    private final Set<Long> marked = new HashSet<Long>();
    private final Set<Long> hiddenIds = new HashSet<Long>(); // lưu ID tin nhắn đã xóa ở phía mình

    public ChatView(Nav nav, Runnable onChanged) {
        super(Kind.SURFACE, new BorderLayout());
        this.nav = nav;
        this.onChanged = onChanged;
        list.setMe(Session.me());

        scroll = ModernScroll.wrap(list);
        scroll.getViewport().setOpaque(false);
        list.setImageListener(new MessageList.ImageListener() {
            public void open(Msg m) {
                BufferedImage img = ImageCache.full(m.attUrl, null);
                if (img != null)
                    Dialogs.image(ChatView.this.nav.overlay(), img);
            }
        });

        // Khi bấm vào nút 3 chấm / tùy chọn trên tin nhắn -> hiện menu chọn Xóa phía
        // mình hoặc Thu hồi
        list.setRecallListener(new MessageList.RecallListener() {
            public void recall(Msg m) {
                showMsgOptions(m);
            }
        });
        list.setMediaListener(new MessageList.MediaListener() {
            public void open(Msg m) {
                openMedia(m);
            }
        });
        attach = FButton.icon(Icons.IMAGE, "Gửi ảnh, video, âm thanh", new Runnable() {
            public void run() {
                pickFile();
            }
        });
        emojiBtn = FButton.icon(Icons.SMILE, "Chèn emoji", new Runnable() {
            public void run() {
                openEmojiPicker();
            }
        });
        mic = FButton.icon(Icons.MIC, "Ghi âm", new Runnable() {
            public void run() {
                toggleRecord();
            }
        });
        input.getInput().setFont(Theme.emojiFont(14.5f));
        send = new FButton("", Icons.SEND, FButton.Kind.PRIMARY, new Runnable() {
            public void run() {
                doSend();
            }
        });
        send.setPreferredSize(new Dimension(46, 46));
        send.setRadius(23);
        input.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                doSend();
            }
        });

        // Hỗ trợ dán ảnh trực tiếp từ Clipboard bằng Ctrl + V
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 && e.getKeyCode() == KeyEvent.VK_V) {
                    if (pasteImageFromClipboard())
                        e.consume();
                }
            }
        });

        chip.setOpaque(false);
        chipName.setFont(Theme.font(Font.PLAIN, 13f));
        chip.add(chipName, BorderLayout.CENTER);
        chip.add(FButton.icon(Icons.CLOSE, "Bỏ tệp", new Runnable() {
            public void run() {
                discardAttachment();
            }
        }), BorderLayout.EAST);
        chip.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 6));
        chip.setVisible(false);

        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        composer.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 3));
        tools.setOpaque(false);
        tools.add(attach);
        tools.add(emojiBtn);
        tools.add(mic);
        composer.add(tools, BorderLayout.WEST);
        composer.add(input, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(chip, BorderLayout.NORTH);
        south.add(composer, BorderLayout.CENTER);

        JPanel chat = new JPanel(new BorderLayout());
        chat.setOpaque(false);
        chat.add(scroll, BorderLayout.CENTER);
        chat.add(south, BorderLayout.SOUTH);
        JComponent empty = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                int cx = getWidth() / 2, cy = getHeight() / 2 - 20;
                g.setColor(Theme.accentSoft());
                g.fill(new java.awt.geom.Ellipse2D.Float(cx - 54, cy - 54, 108, 108));
                Icons.CHAT.paint(g, cx - 28, cy - 28, 56, Theme.accent());
                g.setFont(Theme.font(Font.BOLD, 18f));
                g.setColor(Theme.text());
                String t = "Chọn một cuộc trò chuyện";
                g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, cy + 94);
                g.setFont(Theme.font(Font.PLAIN, 14f));
                g.setColor(Theme.muted());
                String s = "hoặc tìm bạn bè để bắt đầu nhắn tin";
                g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, cy + 120);
                g.dispose();
            }
        };
        body.setOpaque(false);
        body.add(empty, "empty");
        body.add(chat, "chat");
        cards.show(body, "empty");

        drawer = new Drawer();
        mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(header, BorderLayout.NORTH);
        mid.add(body, BorderLayout.CENTER);
        // Layout tự quản: ngăn thông tin trượt đè lên vùng chat (drawer thêm trước =
        // nằm trên cùng)
        setLayout(null);
        add(drawer);
        add(mid);
        header.setVisible(false);

        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport s) {
                return (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                        || s.isDataFlavorSupported(DataFlavor.imageFlavor)) && active();
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport s) {
                try {
                    if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> fs = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (!fs.isEmpty())
                            setAttached(fs.get(0));
                        return true;
                    } else if (s.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image img = (Image) s.getTransferable().getTransferData(DataFlavor.imageFlavor);
                        File tmp = saveTempImage(img);
                        if (tmp != null) {
                            setAttached(tmp);
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    /**
     * Các thành phần con chồng lên nhau (drawer đè lên mid) nên phải tắt tối ưu vẽ.
     */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        mid.setBounds(0, 0, Math.max(0, w - drawer.settled), h);
        drawer.setBounds(w - drawer.width, 0, Drawer.W, h);
    }

    private boolean pasteImageFromClipboard() {
        if (!active())
            return false;
        try {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (t == null)
                return false;
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    setAttached(files.get(0));
                    return true;
                }
            }
            if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                File tmp = saveTempImage(img);
                if (tmp != null) {
                    setAttached(tmp);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private File saveTempImage(Image img) {
        if (img == null)
            return null;
        try {
            int w = img.getWidth(null), h = img.getHeight(null);
            if (w <= 0 || h <= 0)
                return null;
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.drawImage(img, 0, 0, null);
            g2.dispose();
            File f = File.createTempFile("clipboard-", ".png");
            f.deleteOnExit();
            ImageIO.write(bi, "png", f);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean active() {
        return dmUser != null || room != null;
    }

    public void openDm(String user) {
        dmUser = user;
        room = null;
        resetChat();
        header.set(user, "Bạn bè", false);
        drawer.close();
    }

    public void openRoom(Room r) {
        room = r;
        dmUser = null;
        resetChat();
        header.set(r.name, "Nhóm trò chuyện", true);
        drawer.close();
    }

    public void closeChat() {
        dmUser = null;
        room = null;
        token++;
        player.stop();
        setPlayingUi(-1, 0);
        cancelRecording();
        header.setVisible(false);
        cards.show(body, "empty");
        drawer.close();
    }

    public String currentDm() {
        return dmUser;
    }

    private void resetChat() {
        token++;
        player.stop();
        setPlayingUi(-1, 0);
        cancelRecording();
        marked.clear();
        list.reset();
        list.setRoom(room != null);
        list.setEmptyText("Chưa có tin nhắn", "Hãy gửi lời chào đầu tiên");
        setAttached(null);
        input.setText("");
        header.setVisible(true);
        cards.show(body, "chat");
        poll(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                input.requestFocus();
            }
        });
    }

    /** Tải lại tin nhắn của cuộc trò chuyện hiện tại. */
    public void poll(final boolean scrollDown) {
        if (!active())
            return;
        final int tk = token;
        final String dm = dmUser;
        final Room rm = room;
        Async.run(new java.util.concurrent.Callable<List<Msg>>() {
            public List<Msg> call() throws Exception {
                return dm != null ? Session.backend.dmHistory(dm) : Session.backend.roomHistory(rm.id);
            }
        }, new Async.Ok<List<Msg>>() {
            public void accept(List<Msg> h) {
                if (tk != token)
                    return;
                // Lọc bỏ các tin nhắn người dùng đã chọn "Xóa ở phía mình"
                List<Msg> visible = new ArrayList<Msg>();
                for (Msg m : h) {
                    if (!hiddenIds.contains(m.id))
                        visible.add(m);
                }
                boolean fresh = list.setMessages(visible);
                if (fresh || scrollDown)
                    toBottom();
                if (dm != null)
                    markReads(visible, dm);
                drawer.refreshImages(visible);
            }
        }, null);
    }

    private void markReads(List<Msg> h, final String from) {
        final List<Long> todo = new ArrayList<Long>();
        for (Msg m : h)
            if (!m.read && m.sender.equals(from) && marked.add(m.id))
                todo.add(m.id);
        if (todo.isEmpty())
            return;
        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                for (Long id : todo)
                    Session.backend.markRead(id);
                return true;
            }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                if (onChanged != null)
                    onChanged.run();
            }
        }, null);
    }

    private void toBottom() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JScrollBar sb = scroll.getVerticalScrollBar();
                final int from = sb.getValue();
                final int to = sb.getMaximum() - sb.getVisibleAmount();
                if (Math.abs(to - from) > 400) {
                    sb.setValue(to);
                    return;
                }
                Anim.run(260, new Anim.Update() {
                    public void apply(float t) {
                        sb.setValue(
                                from + Math.round((sb.getMaximum() - sb.getVisibleAmount() - from) * Anim.easeOut(t)));
                    }
                });
            }
        });
    }

    /**
     * Hộp thoại chọn tệp hiện đại của hệ điều hành (Windows Explorer có xem trước
     * ảnh)
     */
    private void openEmojiPicker() {
        EmojiPicker.show(emojiBtn, new EmojiPicker.OnPick() {
            public void picked(String emoji) {
                JTextField tf = input.getInput();
                int pos = tf.getCaretPosition();
                String cur = tf.getText();
                String next = cur.substring(0, pos) + emoji + cur.substring(pos);
                tf.setText(next);
                tf.setCaretPosition(pos + emoji.length());
                tf.requestFocusInWindow();
            }
        });
    }

    private void pickFile() {
        if (!active())
            return;
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ChatView.class);
        String lastDir = prefs.get("chat_last_dir", "");
        Window win = SwingUtilities.getWindowAncestor(this);
        FileDialog fd = (win instanceof Frame)
                ? new FileDialog((Frame) win, "Chọn ảnh, video hoặc âm thanh", FileDialog.LOAD)
                : new FileDialog((Dialog) win, "Chọn ảnh, video hoặc âm thanh", FileDialog.LOAD);
        if (!lastDir.isEmpty())
            fd.setDirectory(lastDir);
        fd.setFile("*.png;*.jpg;*.jpeg;*.gif;*.webp;*.mp4;*.mov;*.avi;*.mkv;*.mp3;*.wav;*.ogg;*.m4a;*.webm");
        fd.setVisible(true);
        if (fd.getFile() != null) {
            if (fd.getDirectory() != null)
                prefs.put("chat_last_dir", fd.getDirectory());
            setAttached(new File(fd.getDirectory(), fd.getFile()));
        }
    }

    private void setAttached(File f) {
        if (f != null) {
            if (Models.kindOf(f.getName()).isEmpty()) {
                nav.overlay().toast(
                        "Chỉ gửi được ảnh (png, jpg, gif, webp), video (mp4, mov...) và âm thanh (mp3, wav, ogg, m4a)",
                        true);
                return;
            }
            if (f.length() > 100L * 1024 * 1024) {
                nav.overlay().toast("Tệp quá lớn (tối đa 100 MB) - hãy chọn video ngắn/nhỏ hơn", true);
                return;
            }
        }
        attached = f;
        chip.setVisible(f != null);
        if (f != null) {
            String k = Models.kindOf(f.getName());
            String label = k.equals("AUDIO") ? "Âm thanh" : k.equals("VIDEO") ? "Video" : "Ảnh";
            chipName.setText(label + ": " + f.getName() + "  (" + sizeText(f.length()) + ")");
        }
        chipName.setForeground(Theme.muted());
        chip.revalidate();
        revalidate();
        repaint();
    }

    private static String sizeText(long b) {
        return b >= 1024 * 1024 ? String.format("%.1f MB", b / 1048576.0) : Math.max(1, b / 1024) + " KB";
    }

    private void discardAttachment() {
        cancelRecording();
        setAttached(null);
    }

    /*
     * ---------------- Menu 3 chấm: Chuyển tiếp / Sao chép / Xóa / Thu hồi
     * ----------------
     */
    private void showMsgOptions(final Msg m) {
        List<chatwave.ui.PopupMenu.Item> items = new ArrayList<chatwave.ui.PopupMenu.Item>();
        if (!m.recalled) {
            items.add(new chatwave.ui.PopupMenu.Item(Icons.FORWARD, "Chuyển tiếp", new Runnable() {
                public void run() {
                    showForwardDialog(m);
                }
            }));
            if (m.content != null && !m.content.isEmpty()) {
                items.add(new chatwave.ui.PopupMenu.Item(Icons.COPY, "Sao chép", new Runnable() {
                    public void run() {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                                new java.awt.datatransfer.StringSelection(m.content), null);
                        nav.overlay().toast("Đã sao chép", false);
                    }
                }));
            }
        }
        items.add(new chatwave.ui.PopupMenu.Item(Icons.TRASH, "Xóa phía bạn", new Runnable() {
            public void run() {
                deleteForSelf(m);
            }
        }));
        boolean isMine = m.sender != null && m.sender.equals(Session.me());
        if (isMine && !m.recalled) {
            items.add(
                    new chatwave.ui.PopupMenu.Item(Icons.CLOSE, "Thu hồi (với tất cả)", Theme.danger(), new Runnable() {
                        public void run() {
                            askRecall(m);
                        }
                    }));
        }
        Point p = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(p, this);
        chatwave.ui.PopupMenu.show(this, p.x - 100, p.y + 8, items);
    }

    private void showForwardDialog(final Msg m) {
        final Overlay ov = nav.overlay();
        final Card c = new Card(24);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        c.getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ov.close();
            }
        });

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        headRow.setMaximumSize(new Dimension(380, 32));
        headRow.setAlignmentX(0f);
        headRow.add(new Dialogs.Label("Chuyển tiếp tin nhắn", Theme.font(Font.BOLD, 18f), false, false, 320),
                BorderLayout.WEST);
        headRow.add(FButton.icon(Icons.CLOSE, "Đóng", new Runnable() {
            public void run() {
                ov.close();
            }
        }), BorderLayout.EAST);
        c.add(headRow);
        c.add(Box.createVerticalStrut(10));

        // Xem trước tin nhắn
        String previewTxt = m.isImage()
                ? "[Hình ảnh]" + (m.content != null && !m.content.isEmpty() ? " " + m.content : "")
                : m.isAudio() ? "[Ghi âm]" : m.isVideo() ? "[Video]" : (m.content == null ? "" : m.content);
        if (previewTxt.length() > 80)
            previewTxt = previewTxt.substring(0, 77) + "...";
        JPanel prevBox = new JPanel(new BorderLayout(8, 0));
        prevBox.setBackground(Theme.surface2());
        prevBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        prevBox.setMaximumSize(new Dimension(380, 48));
        prevBox.setAlignmentX(0f);
        JLabel prevLbl = new JLabel(previewTxt);
        prevLbl.setFont(Theme.emojiFont(13f));
        prevLbl.setForeground(Theme.muted());
        prevBox.add(prevLbl, BorderLayout.CENTER);
        c.add(prevBox);
        c.add(Box.createVerticalStrut(10));

        final FField searchF = new FField("Tìm bạn bè hoặc nhóm...", Icons.SEARCH, false);
        searchF.setPreferredSize(new Dimension(380, 40));
        searchF.setMaximumSize(new Dimension(380, 40));
        searchF.setAlignmentX(0f);
        c.add(searchF);
        c.add(Box.createVerticalStrut(8));

        final JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = ModernScroll.wrap(listPanel);
        sp.setPreferredSize(new Dimension(380, 220));
        sp.setMaximumSize(new Dimension(380, 220));
        sp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));
        sp.setAlignmentX(0f);
        c.add(sp);
        c.add(Box.createVerticalStrut(14));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.setMaximumSize(new Dimension(380, 44));
        FButton cancel = new FButton("Hủy", FButton.Kind.GHOST, new Runnable() {
            public void run() {
                ov.close();
            }
        });
        final FButton sendBtn = new FButton("Gửi", Icons.SEND, FButton.Kind.PRIMARY, null);
        sendBtn.setEnabled(false);

        final List<String> selFriends = new ArrayList<String>();
        final List<Long> selRooms = new ArrayList<Long>();
        final List<Friend> allFriends = new ArrayList<Friend>();
        final List<Room> allRooms = new ArrayList<Room>();

        final Runnable updateSendBtn = new Runnable() {
            public void run() {
                int n = selFriends.size() + selRooms.size();
                sendBtn.setText(n > 0 ? "Gửi (" + n + ")" : "Gửi");
                sendBtn.setEnabled(n > 0);
            }
        };

        final Runnable rebuildRows = new Runnable() {
            public void run() {
                listPanel.removeAll();
                String q = searchF.getText().trim().toLowerCase();
                for (final Friend f : allFriends) {
                    if (!q.isEmpty() && !f.username.toLowerCase().contains(q))
                        continue;
                    listPanel.add(forwardRow(f.username, false, new Runnable() {
                        public void run() {
                            if (selFriends.contains(f.username))
                                selFriends.remove(f.username);
                            else if (selFriends.size() + selRooms.size() >= 10) {
                                ov.toast("Tối đa 10 nơi nhận", true);
                                return;
                            } else
                                selFriends.add(f.username);
                            updateSendBtn.run();
                        }
                    }, new java.util.concurrent.Callable<Boolean>() {
                        public Boolean call() {
                            return selFriends.contains(f.username);
                        }
                    }));
                }
                for (final Room rm : allRooms) {
                    if (!q.isEmpty() && !rm.name.toLowerCase().contains(q))
                        continue;
                    listPanel.add(forwardRow(rm.name, true, new Runnable() {
                        public void run() {
                            if (selRooms.contains(rm.id))
                                selRooms.remove(rm.id);
                            else if (selFriends.size() + selRooms.size() >= 10) {
                                ov.toast("Tối đa 10 nơi nhận", true);
                                return;
                            } else
                                selRooms.add(rm.id);
                            updateSendBtn.run();
                        }
                    }, new java.util.concurrent.Callable<Boolean>() {
                        public Boolean call() {
                            return selRooms.contains(rm.id);
                        }
                    }));
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        };

        searchF.getInput().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                rebuildRows.run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                rebuildRows.run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                rebuildRows.run();
            }
        });

        Async.run(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                allFriends.addAll(Session.backend.friends());
                allRooms.addAll(Session.backend.myRooms());
                return true;
            }
        }, new Async.Ok<Boolean>() {
            public void accept(Boolean b) {
                rebuildRows.run();
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                ov.toast(e.getMessage(), true);
            }
        });

        sendBtn.setAction(new Runnable() {
            public void run() {
                if (!sendBtn.isEnabled())
                    return;
                sendBtn.setLoading(true);
                final String srcType = dmUser == null ? "ROOM" : "DM";
                Async.run(new java.util.concurrent.Callable<ForwardResult>() {
                    public ForwardResult call() throws Exception {
                        return Session.backend.forwardMessage(m.id, srcType, new ArrayList<String>(selFriends),
                                new ArrayList<Long>(selRooms));
                    }
                }, new Async.Ok<ForwardResult>() {
                    public void accept(ForwardResult res) {
                        sendBtn.setLoading(false);
                        ov.close();
                        if (res.failedCount == 0) {
                            ov.toast("Đã chuyển tiếp", false);
                        } else {
                            ov.toast("Đã chuyển tiếp " + res.successCount + "/" + res.totalRequested + ". Lỗi: "
                                    + String.join(", ", res.failedDetails), true);
                        }
                        if (onChanged != null)
                            onChanged.run();
                    }
                }, new Async.Fail() {
                    public void accept(Exception e) {
                        sendBtn.setLoading(false);
                        ov.toast(e.getMessage(), true);
                    }
                });
            }
        });

        btns.add(cancel);
        btns.add(sendBtn);
        c.add(btns);
        ov.show(c);
    }

    private JPanel forwardRow(final String label, final boolean isGroup, final Runnable toggle,
            final java.util.concurrent.Callable<Boolean> isSelected) {
        final boolean[] hov = { false };
        final JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Gfx.g2(g0);
                if (hov[0]) {
                    g.setColor(Theme.hover());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                Avatar.paint(g, label, 10, 8, 32, false, isGroup);
                g.setFont(Theme.font(Font.PLAIN, 14f));
                g.setColor(Theme.text());
                FontMetrics fm = g.getFontMetrics();
                String txt = Gfx.ellipsize(label, fm, getWidth() - (isGroup ? 145 : 95));
                g.drawString(txt, 52, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                if (isGroup) {
                    g.setFont(Theme.font(Font.BOLD, 10.5f));
                    g.setColor(Theme.accentSoft());
                    g.fill(Gfx.rr(getWidth() - 88, 13, 46, 20, 6));
                    g.setColor(Theme.accent());
                    g.drawString("Nhóm", getWidth() - 80, 27);
                }
                boolean sel = false;
                try {
                    sel = isSelected.call();
                } catch (Exception ignored) {
                }
                int cx = getWidth() - 30, cy = (getHeight() - 18) / 2;
                if (sel) {
                    g.setColor(Theme.accent());
                    g.fill(Gfx.rr(cx, cy, 18, 18, 4));
                    Icons.CHECK.paint(g, cx + 2, cy + 2, 14, Theme.onAccent());
                } else {
                    g.setColor(Theme.border());
                    g.draw(Gfx.rr(cx, cy, 18, 18, 4));
                }
                g.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(360, 46));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
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
                toggle.run();
                row.repaint();
            }
        });
        return row;
    }

    private void deleteForSelf(Msg m) {
        if (m.id == playingId) {
            player.stop();
            setPlayingUi(-1, 0);
        }
        hiddenIds.add(m.id);
        nav.overlay().toast("Đã xóa tin nhắn ở phía bạn", false);
        poll(false);
    }

    private void askRecall(final Msg m) {
        Dialogs.confirm(nav.overlay(), "Thu hồi tin nhắn với tất cả?",
                "Tin nhắn sẽ bị gỡ nội dung với cả hai bên và không thể khôi phục.", "Thu hồi", true, new Runnable() {
                    public void run() {
                        final int tk = token;
                        final String dm = dmUser;
                        final Room rm = room;
                        if (m.id == playingId) {
                            player.stop();
                            setPlayingUi(-1, 0);
                        }
                        Async.run(new java.util.concurrent.Callable<Boolean>() {
                            public Boolean call() throws Exception {
                                if (dm != null)
                                    Session.backend.recallDm(m.id);
                                else
                                    Session.backend.recallRoom(rm.id, m.id);
                                return true;
                            }
                        }, new Async.Ok<Boolean>() {
                            public void accept(Boolean b) {
                                nav.overlay().toast("Đã thu hồi tin nhắn với tất cả", false);
                                if (tk == token)
                                    poll(false);
                                if (onChanged != null)
                                    onChanged.run();
                            }
                        }, new Async.Fail() {
                            public void accept(Exception e) {
                                nav.overlay().toast(e.getMessage() == null ? "Thu hồi thất bại" : e.getMessage(), true);
                            }
                        });
                    }
                });
    }

    /* ---------------- ghi âm ---------------- */
    private void toggleRecord() {
        if (!active())
            return;
        if (recorder != null && recorder.isRunning()) {
            finishRecording();
            return;
        }
        if (attached != null) {
            nav.overlay().toast("Hãy gửi hoặc bỏ tệp đang đính kèm trước", true);
            return;
        }
        final Audio.Recorder r = new Audio.Recorder();
        try {
            r.start();
        } catch (Exception e) {
            nav.overlay().toast(
                    "Không mở được micro: " + (e.getMessage() == null ? "không tìm thấy thiết bị" : e.getMessage()),
                    true);
            return;
        }
        recorder = r;
        mic.setIcon(Icons.STOP);
        mic.setForegroundColor(Theme.danger());
        chip.setVisible(true);
        chipName.setForeground(Theme.danger());
        recTimer = new javax.swing.Timer(250, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (recorder == null)
                    return;
                int s = recorder.seconds();
                chipName.setText("Đang ghi âm  " + (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60)
                        + "   (bấm nút mic để dừng)");
                chip.repaint();
                if (!recorder.isRunning())
                    finishRecording();
            }
        });
        recTimer.start();
        chipName.setText("Đang ghi âm  0:00   (bấm nút mic để dừng)");
        revalidate();
    }

    private void resetMic() {
        if (recTimer != null)
            recTimer.stop();
        recTimer = null;
        mic.setIcon(Icons.MIC);
        mic.setForegroundColor(null);
    }

    private void finishRecording() {
        if (recorder == null)
            return;
        Audio.Recorder r = recorder;
        int secs = r.seconds();
        recorder = null;
        resetMic();
        try {
            File f = r.stop();
            if (f == null) {
                chip.setVisible(false);
                nav.overlay().toast("Ghi âm quá ngắn", true);
                return;
            }
            setAttached(f);
            chipName.setText("Ghi âm " + (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60) + "  ("
                    + sizeText(f.length()) + ") - bấm gửi");
        } catch (Exception e) {
            chip.setVisible(false);
            nav.overlay().toast("Lưu ghi âm thất bại", true);
        }
    }

    private void cancelRecording() {
        if (recorder != null) {
            recorder.cancel();
            recorder = null;
        }
        resetMic();
    }

    /* ---------------- mở / phát tệp đính kèm ---------------- */
    private void setPlayingUi(long id, float p) {
        playingId = id;
        list.setPlaying(id, p);
    }

    private void openMedia(final Msg m) {
        if (m.isAudio() && playingId == m.id) {
            player.stop();
            setPlayingUi(-1, 0);
            return;
        }
        player.stop();
        setPlayingUi(-1, 0);
        byte[] cached = mediaCache.get(m.attUrl);
        if (cached != null) {
            playOrOpen(m, cached);
            return;
        }
        nav.overlay().toast(m.isVideo() ? "Đang tải video..." : "Đang tải âm thanh...", false);
        Async.run(new java.util.concurrent.Callable<byte[]>() {
            public byte[] call() throws Exception {
                return Session.backend.download(m.attUrl);
            }
        }, new Async.Ok<byte[]>() {
            public void accept(byte[] d) {
                mediaCache.put(m.attUrl, d);
                playOrOpen(m, d);
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                nav.overlay().toast(e.getMessage() == null ? "Không tải được tệp" : e.getMessage(), true);
            }
        });
    }

    private void playOrOpen(final Msg m, byte[] data) {
        if (m.isAudio()) {
            try {
                setPlayingUi(m.id, 0f);
                player.play(data, new Audio.Progress() {
                    public void update(float p, boolean done) {
                        if (done)
                            setPlayingUi(-1, 0);
                        else
                            setPlayingUi(m.id, p);
                    }
                });
                return;
            } catch (Exception e) {
                setPlayingUi(-1, 0);
            }
        }
        try {
            String ext = Models.extOf(m.attUrl);
            File f = File.createTempFile("chatwave-", ext.isEmpty() ? (m.isVideo() ? ".mp4" : ".wav") : ext);
            f.deleteOnExit();
            java.nio.file.Files.write(f.toPath(), data);
            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().open(f);
            else
                nav.overlay().toast("Đã lưu tại " + f.getAbsolutePath(), false);
        } catch (Exception e) {
            nav.overlay().toast("Không mở được tệp bằng trình phát của máy", true);
        }
    }

    private void doSend() {
        if (!active() || sending)
            return;
        final String text = input.getText().trim();
        final File file = attached;
        if (text.isEmpty() && file == null) {
            input.flagError();
            return;
        }
        sending = true;
        send.setLoading(true);
        final int tk = token;
        final String dm = dmUser;
        final Room rm = room;
        Async.run(new java.util.concurrent.Callable<Msg>() {
            public Msg call() throws Exception {
                String url = null, type = null;
                if (file != null) {
                    Upload u = Session.backend.upload(file);
                    url = u.url;
                    type = u.type;
                }
                return dm != null ? Session.backend.sendDm(dm, text, url, type)
                        : Session.backend.sendRoom(rm.id, text, url, type);
            }
        }, new Async.Ok<Msg>() {
            public void accept(Msg m) {
                sending = false;
                send.setLoading(false);
                if (tk != token)
                    return;
                input.setText("");
                setAttached(null);
                poll(true);
                if (onChanged != null)
                    onChanged.run();
            }
        }, new Async.Fail() {
            public void accept(Exception e) {
                sending = false;
                send.setLoading(false);
                nav.overlay().toast(e.getMessage() == null ? "Gửi thất bại" : e.getMessage(), true);
            }
        });
    }

    /* ---------------- Tiêu đề ---------------- */
    private final class Header extends JComponent {
        private String title = "", sub = "";
        private boolean group;
        private final FButton info;

        Header() {
            setLayout(null);
            setPreferredSize(new Dimension(100, 68));
            info = FButton.icon(Icons.INFO, "Thông tin", new Runnable() {
                public void run() {
                    drawer.toggle();
                }
            });
            add(info);
        }

        void set(String t, String s, boolean g) {
            title = t;
            sub = s;
            group = g;
            repaint();
        }

        @Override
        public void doLayout() {
            info.setBounds(getWidth() - 56, (getHeight() - 40) / 2, 40, 40);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.g2(g0);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.surface());
            g.fillRect(0, 0, w, h);
            g.setColor(Theme.border());
            g.fillRect(0, h - 1, w, 1);
            Avatar.paint(g, title, 18, 14, 40, false, group);
            g.setFont(Theme.font(Font.BOLD, 16f));
            g.setColor(Theme.text());
            g.drawString(Gfx.ellipsize(title, g.getFontMetrics(), w - 160), 70, 31);
            g.setFont(Theme.font(Font.PLAIN, 12.5f));
            g.setColor(Theme.muted());
            g.drawString(sub, 70, 50);
            g.dispose();
        }
    }

    /*
     * ---------------- Ngăn thông tin (trượt đè lên từ bên phải) ----------------
     */
    private final class Drawer extends JPanel {
        static final int W = 300;
        int width; // phần đang hiển thị (0..W)
        int settled; // phần vùng chat đã nhường chỗ (đổi 1 lần mỗi lần mở/đóng)
        private boolean open;
        private Anim.Tween tw;
        private final JPanel content = new JPanel();
        private final ImageGrid grid = new ImageGrid();
        private final JScrollPane sp;

        Drawer() {
            super(null);
            setOpaque(true);
            setVisible(false);
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));
            sp = ModernScroll.wrap(content);
            add(sp);
        }

        @Override
        public void doLayout() {
            sp.setBounds(0, 0, getWidth(), getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.side());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.border());
            g.fillRect(0, 0, 1, getHeight());
        }

        void toggle() {
            if (open)
                close();
            else
                openIt();
        }

        void close() {
            if (!open && width == 0)
                return;
            open = false;
            settled = 0; // vùng chat nở ra ngay, ngăn trượt đi để lộ dần phần bên dưới
            ChatView.this.revalidate();
            slide(0);
        }

        void openIt() {
            open = true;
            build();
            slide(W);
        }

        private void slide(final int target) {
            if (tw != null)
                tw.cancel();
            final int from = width;
            setVisible(true);
            tw = Anim.run(260, new Anim.Update() {
                public void apply(float t) {
                    width = from + Math.round((target - from) * Anim.easeOut(t));
                    ChatView.this.doLayout(); // chỉ dời drawer, không layout lại danh sách tin nhắn
                    ChatView.this.repaint();
                    if (t >= 1f) {
                        if (target > 0) {
                            settled = target;
                            ChatView.this.revalidate();
                        } else
                            setVisible(false);
                    }
                }
            });
        }

        void refreshImages(List<Msg> h) {
            List<Msg> imgs = new ArrayList<Msg>();
            for (Msg m : h)
                if (m.isImage())
                    imgs.add(m);
            grid.set(imgs);
        }

        private JComponent label(String t, Font f, boolean muted) {
            Dialogs.Label l = new Dialogs.Label(t, f, muted, true, 258);
            l.setAlignmentX(0f);
            return l;
        }

        private void build() {
            content.removeAll();
            if (dmUser != null) {
                final String u = dmUser;
                content.add(avatarBlock(u, false));
                content.add(Box.createVerticalStrut(12));
                content.add(label(u, Theme.font(Font.BOLD, 18f), false));
                final JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                info.setAlignmentX(0f);
                content.add(info);
                Async.run(new java.util.concurrent.Callable<Profile>() {
                    public Profile call() throws Exception {
                        return Session.backend.profile(u);
                    }
                }, new Async.Ok<Profile>() {
                    public void accept(Profile p) {
                        if (!u.equals(dmUser))
                            return;
                        info.removeAll();
                        if (!p.display().equals(u))
                            info.add(label(p.display(), Theme.font(Font.PLAIN, 14f), true));
                        if (p.email != null && !p.email.isEmpty()) {
                            info.add(Box.createVerticalStrut(4));
                            info.add(label(p.email, Theme.font(Font.PLAIN, 13f), true));
                        }
                        if (p.bio != null && !p.bio.isEmpty()) {
                            info.add(Box.createVerticalStrut(8));
                            info.add(label(p.bio, Theme.font(Font.PLAIN, 13.5f), false));
                        }
                        info.revalidate();
                        content.revalidate();
                        content.repaint();
                    }
                }, null);
            } else if (room != null) {
                content.add(avatarBlock(room.name, true));
                content.add(Box.createVerticalStrut(12));
                content.add(label(room.name, Theme.font(Font.BOLD, 18f), false));
                content.add(label("Tạo bởi " + room.createdBy, Theme.font(Font.PLAIN, 13f), true));
                content.add(Box.createVerticalStrut(18));
                final JPanel mem = new JPanel();
                mem.setOpaque(false);
                mem.setLayout(new BoxLayout(mem, BoxLayout.Y_AXIS));
                mem.setAlignmentX(0f);
                final long rid = room.id;
                content.add(sectionTitle("Thành viên"));
                content.add(mem);
                Async.run(new java.util.concurrent.Callable<List<String>>() {
                    public List<String> call() throws Exception {
                        return Session.backend.roomMembers(rid);
                    }
                }, new Async.Ok<List<String>>() {
                    public void accept(List<String> ms) {
                        mem.removeAll();
                        for (String m : ms)
                            mem.add(memberRow(m));
                        mem.revalidate();
                        content.revalidate();
                        content.repaint();
                    }
                }, null);
            }
            content.add(Box.createVerticalStrut(18));
            content.add(sectionTitle("Ảnh đã chia sẻ"));
            grid.setAlignmentX(0f);
            content.add(grid);
            if (dmUser != null) {
                content.add(Box.createVerticalStrut(22));
                final String u = dmUser;
                FButton un = new FButton("Hủy kết bạn", Icons.TRASH, FButton.Kind.DANGER, new Runnable() {
                    public void run() {
                        Dialogs.confirm(nav.overlay(), "Hủy kết bạn?",
                                "Bạn và " + u + " sẽ không còn nhắn tin được cho nhau.", "Hủy kết bạn", true,
                                new Runnable() {
                                    public void run() {
                                        Async.run(new java.util.concurrent.Callable<Boolean>() {
                                            public Boolean call() throws Exception {
                                                Session.backend.unfriend(u);
                                                return true;
                                            }
                                        }, new Async.Ok<Boolean>() {
                                            public void accept(Boolean b) {
                                                closeChat();
                                                if (onChanged != null)
                                                    onChanged.run();
                                                nav.overlay().toast("Đã hủy kết bạn", false);
                                            }
                                        }, new Async.Fail() {
                                            public void accept(Exception e) {
                                                nav.overlay().toast(e.getMessage(), true);
                                            }
                                        });
                                    }
                                });
                    }
                });
                un.setPreferredSize(new Dimension(258, 42));
                un.setMaximumSize(new Dimension(258, 42));
                un.setAlignmentX(0f);
                content.add(un);
            }
            content.revalidate();
            content.repaint();
        }

        private JComponent sectionTitle(String t) {
            JLabel l = new JLabel(t) {
                @Override
                public Color getForeground() {
                    return Theme.muted();
                }
            };
            l.setFont(Theme.font(Font.BOLD, 12.5f));
            l.setAlignmentX(0f);
            l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            p.add(l, BorderLayout.WEST);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            p.setAlignmentX(0f);
            return p;
        }

        private JComponent avatarBlock(final String name, final boolean g) {
            JComponent c = new JComponent() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D gg = Gfx.g2(g0);
                    Avatar.paint(gg, name, (getWidth() - 88) / 2f, 0, 88, false, g);
                    gg.dispose();
                }
            };
            c.setPreferredSize(new Dimension(258, 88));
            c.setMaximumSize(new Dimension(258, 88));
            c.setAlignmentX(0f);
            return c;
        }

        private JComponent memberRow(final String name) {
            JComponent c = new JComponent() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D gg = Gfx.g2(g0);
                    Avatar.paint(gg, name, 0, 4, 30, false, false);
                    gg.setFont(Theme.font(Font.PLAIN, 14f));
                    gg.setColor(Theme.text());
                    gg.drawString(name + (name.equals(Session.me()) ? " (bạn)" : ""), 42, 24);
                    gg.dispose();
                }
            };
            c.setPreferredSize(new Dimension(258, 38));
            c.setMaximumSize(new Dimension(258, 38));
            c.setAlignmentX(0f);
            return c;
        }
    }

    /* ---------------- Lưới ảnh đã chia sẻ ---------------- */
    private final class ImageGrid extends JComponent {
        private List<Msg> imgs = new ArrayList<Msg>();
        private int hover = -1;
        private static final int CELL = 82, GAP = 6;

        ImageGrid() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseMotionListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int i = at(e.getPoint());
                    if (i != hover) {
                        int old = hover;
                        hover = i;
                        repaintCell(old);
                        repaintCell(i);
                    }
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    int i = at(e.getPoint());
                    if (i >= 0) {
                        BufferedImage im = ImageCache.full(imgs.get(i).attUrl, null);
                        if (im != null)
                            Dialogs.image(nav.overlay(), im);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    int old = hover;
                    hover = -1;
                    repaintCell(old);
                }
            });
        }

        private void repaintCell(int i) {
            if (i < 0)
                return;
            repaint((i % 3) * (CELL + GAP), (i / 3) * (CELL + GAP), CELL, CELL);
        }

        void set(List<Msg> l) {
            boolean same = l.size() == imgs.size();
            for (int i = 0; same && i < l.size(); i++)
                same = l.get(i).id == imgs.get(i).id;
            if (same)
                return;
            imgs = l;
            revalidate();
            repaint();
        }

        private int at(Point p) {
            int col = p.x / (CELL + GAP), row = p.y / (CELL + GAP), i = row * 3 + col;
            return col < 3 && p.x % (CELL + GAP) < CELL && p.y % (CELL + GAP) < CELL && i < imgs.size() ? i : -1;
        }

        @Override
        public Dimension getPreferredSize() {
            if (imgs.isEmpty())
                return new Dimension(258, 50);
            int rows = (imgs.size() + 2) / 3;
            return new Dimension(258, rows * (CELL + GAP));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.g2(g0);
            Rectangle clipB = g.getClipBounds();
            if (imgs.isEmpty()) {
                g.setFont(Theme.font(Font.PLAIN, 13f));
                g.setColor(Theme.muted());
                g.drawString("Chưa có ảnh nào", 0, 20);
            }
            for (int i = 0; i < imgs.size(); i++) {
                int x = (i % 3) * (CELL + GAP), y = (i / 3) * (CELL + GAP);
                if (clipB != null && !clipB.intersects(x, y, CELL, CELL))
                    continue; // bỏ qua ô ngoài vùng cần vẽ
                Shape clip = Gfx.rr(x, y, CELL, CELL, 12);
                g.setColor(Theme.surface2());
                g.fill(clip);
                BufferedImage t = ImageCache.thumb(imgs.get(i).attUrl, CELL * 2, CELL * 2, new Runnable() {
                    public void run() {
                        repaint();
                    }
                });
                if (t != null) {
                    Shape old = g.getClip();
                    g.clip(clip);
                    double s = Math.max((double) CELL / t.getWidth(), (double) CELL / t.getHeight());
                    int w = (int) Math.round(t.getWidth() * s), h = (int) Math.round(t.getHeight() * s);
                    g.drawImage(t, x + (CELL - w) / 2, y + (CELL - h) / 2, w, h, null);
                    if (i == hover) {
                        g.setColor(new Color(255, 255, 255, 40));
                        g.fillRect(x, y, CELL, CELL);
                    }
                    g.setClip(old);
                }
            }
            g.dispose();
        }
    }
}