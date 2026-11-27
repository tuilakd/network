package chatwave.core;

import chatwave.core.Models.*;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Dữ liệu giả chạy trong bộ nhớ, để xem giao diện khi chưa có backend.
 * Đăng nhập tên bất kỳ (mật khẩu bất kỳ). Quản trị viên: admin / admin123 (giống tài khoản backend tự tạo).
 */
public class DemoBackend implements Backend {
    private final String[] friendNames = {"minh_anh", "bao_tran", "hoang_nam", "linh_chi", "duc_thinh"};
    private final String[] full = {"Minh Anh", "Bảo Trân", "Hoàng Nam", "Linh Chi", "Đức Thịnh"};
    private final String[] bios = {"Thích cà phê và code Java", "Sinh viên CNTT năm 3", "Đam mê nhiếp ảnh", "Đang học Lập trình mạng", "Chơi bóng rổ cuối tuần"};
    private final Map<String, List<Msg>> dms = new HashMap<String, List<Msg>>();
    private final Map<Long, List<Msg>> roomMsgs = new HashMap<Long, List<Msg>>();
    private final List<String> friends = new ArrayList<String>();
    private final List<Req> incoming = new ArrayList<Req>();
    private final List<Room> rooms = new ArrayList<Room>();
    private final List<Long> myRooms = new ArrayList<Long>();
    private final Map<String, byte[]> files = new HashMap<String, byte[]>();
    private final List<AdminUser> users = new ArrayList<AdminUser>();
    private String me = "";
    private long ids = 100;
    private final Random rnd = new Random(7);

    public DemoBackend() {
        long now = System.currentTimeMillis();
        for (String f : friendNames) { friends.add(f); dms.put(f, new ArrayList<Msg>()); }
        String[][] chat = {
            {"minh_anh", "them", "Chào bạn, hôm nay nộp đồ án chưa?", "-190"},
            {"minh_anh", "me", "Mình đang làm nốt giao diện Swing nè", "-185"},
            {"minh_anh", "them", "Nhớ gửi mình xem thử nhé", "-5"},
            {"bao_tran", "them", "Tối nay họp nhóm lúc 8 giờ nha", "-60"},
            {"bao_tran", "me", "Ok bạn, mình có mặt", "-58"},
            {"hoang_nam", "them", "Bạn xem giúp mình đoạn code kết nối socket với", "-300"},
            {"hoang_nam", "them", "Nó báo lỗi Connection refused ở dòng 42", "-299"},
            {"linh_chi", "me", "Tài liệu báo cáo mình gửi rồi đó", "-1500"},
            {"duc_thinh", "them", "Cảm ơn bạn nhiều!", "-3000"}};
        for (String[] c : chat) {
            Msg m = new Msg();
            m.id = ids++;
            m.sender = c[1].equals("me") ? "@me" : c[0];
            m.content = c[2];
            m.at = now + Long.parseLong(c[3]) * 60000L;
            m.read = !(c[0].equals("minh_anh") && c[1].equals("them") && m.content.startsWith("Nhớ"))
                    && !(c[0].equals("hoang_nam"));
            dms.get(c[0]).add(m);
        }
        // một tin có ảnh
        Msg img = new Msg();
        img.id = ids++;
        img.sender = "minh_anh";
        img.attType = "IMAGE";
        img.attUrl = demoImage("sunset", new Color(0xFFB28A), new Color(0x7B6CF0));
        img.content = "Ảnh hôm qua mình chụp";
        img.at = now - 6 * 60000L;
        img.read = true;
        dms.get("minh_anh").add(dms.get("minh_anh").size() - 1, img);
        dms.get("minh_anh").get(dms.get("minh_anh").size() - 1).at = now - 5 * 60000L;

        Req r = new Req();
        r.id = ids++;
        r.requester = "quoc_viet";
        r.addressee = "@me";
        r.at = now - 3600000L;
        incoming.add(r);

        String[] rn = {"Lớp Lập trình mạng", "Nhóm đồ án chat", "Hội yêu Java"};
        for (int i = 0; i < rn.length; i++) {
            Room x = new Room();
            x.id = i + 1;
            x.name = rn[i];
            x.createdBy = i == 2 ? "hoang_nam" : "minh_anh";
            rooms.add(x);
            roomMsgs.put(x.id, new ArrayList<Msg>());
            if (i < 2) myRooms.add(x.id);
        }
        addRoom(1, "minh_anh", "Mọi người nhớ nộp báo cáo trước thứ Sáu nhé", -240);
        addRoom(1, "bao_tran", "Thầy bảo giao diện phải đẹp nữa đó", -200);
        addRoom(2, "hoang_nam", "Ai làm phần server rồi vậy?", -90);

        String[] un = {"admin", "minh_anh", "bao_tran", "hoang_nam", "linh_chi", "duc_thinh", "quoc_viet"};
        for (int i = 0; i < un.length; i++) {
            AdminUser u = new AdminUser();
            u.id = i + 1;
            u.username = un[i];
            u.email = un[i] + "@gmail.com";
            u.role = i == 0 ? "ADMIN" : "USER";
            users.add(u);
        }
    }

    private void addRoom(long room, String who, String text, int minAgo) {
        Msg m = new Msg();
        m.id = ids++;
        m.sender = who;
        m.content = text;
        m.at = System.currentTimeMillis() + minAgo * 60000L;
        roomMsgs.get(room).add(m);
    }

    private String demoImage(String name, Color a, Color b) {
        try {
            BufferedImage im = new BufferedImage(640, 420, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, a, 640, 420, b));
            g.fillRect(0, 0, 640, 420);
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval(380, 60, 190, 190);
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(-120, 260, 560, 300);
            g.fillOval(300, 300, 520, 260);
            g.dispose();
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            ImageIO.write(im, "png", o);
            String url = "demo://" + name;
            files.put(url, o.toByteArray());
            return url;
        } catch (Exception e) {
            return "";
        }
    }

    private String key(String sender) { return sender.equals("@me") ? me : sender; }

    private List<Msg> view(List<Msg> src) {
        List<Msg> out = new ArrayList<Msg>();
        for (Msg m : src) {
            Msg c = new Msg();
            c.id = m.id; c.sender = key(m.sender); c.content = m.content; c.attUrl = m.attUrl; c.attType = m.attType;
            c.read = m.read; c.at = m.at; c.recalled = m.recalled;
            if (c.recalled) { c.content = ""; c.attUrl = ""; c.attType = ""; }
            out.add(c);
        }
        return out;
    }

    private void lag() { try { Thread.sleep(120); } catch (InterruptedException e) { /* bỏ qua */ } }

    public Account login(String u, String p) throws Exception {
        lag();
        if (u.trim().isEmpty()) throw new ApiException(401, "Sai tên đăng nhập hoặc mật khẩu");
        me = u.trim();
        if (me.contains("@")) me = me.substring(0, me.indexOf('@')); // đăng nhập bằng email
        if (me.equalsIgnoreCase("admin") && !"admin123".equals(p)) { me = ""; throw new ApiException(401, "Sai tên đăng nhập hoặc mật khẩu"); }
        Account a = new Account();
        a.username = me;
        a.role = me.equalsIgnoreCase("admin") ? "ADMIN" : "USER";
        a.token = "demo";
        return a;
    }

    public void register(String u, String email, String p) throws Exception {
        lag();
        if (u.equalsIgnoreCase("admin")) throw new ApiException(409, "Tên đăng nhập đã tồn tại");
    }

    public void logout() { me = ""; }

    private String sentCode = "";

    public void forgotPassword(String email) throws Exception {
        lag();
        if (email == null || !email.contains("@")) throw new ApiException(400, "Email không hợp lệ");
        sentCode = "123456";
    }

    public void resetPassword(String email, String code, String newPassword) throws Exception {
        lag();
        if (sentCode.isEmpty() || !sentCode.equals(code)) throw new ApiException(400, "Mã xác nhận không đúng hoặc đã hết hạn");
        if (newPassword == null || newPassword.length() < 6) throw new ApiException(400, "Mật khẩu mới tối thiểu 6 ký tự");
        sentCode = "";
    }

    public void changePassword(String oldPassword, String newPassword) throws Exception {
        lag();
        if (oldPassword == null || oldPassword.isEmpty()) throw new ApiException(400, "Mật khẩu hiện tại không được để trống");
        if (newPassword == null || newPassword.length() < 6) throw new ApiException(400, "Mật khẩu mới tối thiểu 6 ký tự");
    }

    public List<Friend> friends() {
        List<Friend> out = new ArrayList<Friend>();
        for (String f : friends) {
            Friend x = new Friend();
            x.username = f;
            List<Msg> l = dms.get(f);
            if (l != null && !l.isEmpty()) {
                Msg last = l.get(l.size() - 1);
                x.lastMessage = last.recalled ? "Tin nhắn đã được thu hồi" : last.attUrl.isEmpty() ? last.content : last.isAudio() ? "[Ghi âm]" : last.isVideo() ? "[Video]" : "[Hình ảnh]";
                x.lastSender = key(last.sender);
                x.lastAt = last.at;
                for (Msg m : l) if (!m.sender.equals("@me") && !m.read) x.unread++;
            }
            out.add(x);
        }
        return out;
    }

    public List<Req> pendingRequests() {
        List<Req> out = new ArrayList<Req>();
        for (Req r : incoming) {
            Req c = new Req();
            c.id = r.id; c.requester = r.requester; c.addressee = me; c.at = r.at;
            out.add(c);
        }
        return out;
    }

    public void sendRequest(String t) throws Exception {
        lag();
        if (t.equalsIgnoreCase(me)) throw new ApiException(400, "Không thể kết bạn với chính mình");
        if (friends.contains(t)) throw new ApiException(409, "Hai bạn đã là bạn bè");
        boolean known = false;
        for (AdminUser u : users) if (u.username.equalsIgnoreCase(t)) known = true;
        if (!known) throw new ApiException(404, "Không tìm thấy người dùng '" + t + "'");
    }

    public void accept(long id) {
        for (int i = 0; i < incoming.size(); i++) {
            if (incoming.get(i).id == id) {
                String n = incoming.remove(i).requester;
                friends.add(n);
                dms.put(n, new ArrayList<Msg>());
                return;
            }
        }
    }

    public void decline(long id) {
        for (int i = 0; i < incoming.size(); i++) if (incoming.get(i).id == id) { incoming.remove(i); return; }
    }

    public void unfriend(String u) { friends.remove(u); }

    public List<Msg> dmHistory(String f) throws Exception {
        List<Msg> l = dms.get(f);
        if (l == null) throw new ApiException(403, "Hai bạn chưa là bạn bè");
        return view(l);
    }

    public Msg sendDm(final String to, String content, String url, String type) throws Exception {
        lag();
        List<Msg> l = dms.get(to);
        if (l == null) throw new ApiException(403, "Hai bạn chưa là bạn bè nên chưa thể nhắn tin");
        Msg m = new Msg();
        m.id = ids++;
        m.sender = "@me";
        m.content = content;
        m.attUrl = url == null ? "" : url;
        m.attType = type == null ? "" : type;
        m.at = System.currentTimeMillis();
        l.add(m);
        final long mid = m.id;
        // bạn "xem" tin sau 1,2 giây rồi trả lời sau 2,5 giây
        schedule(1200, new Runnable() { public void run() { setRead(to, mid); } });
        schedule(2800, new Runnable() {
            public void run() {
                Msg r = new Msg();
                r.id = ids++;
                r.sender = to;
                String[] ans = {"Ok bạn", "Hay quá!", "Để mình xem rồi báo lại nha", "Haha đúng rồi 😂", "Cảm ơn bạn nhé"};
                r.content = ans[rnd.nextInt(ans.length)];
                r.at = System.currentTimeMillis();
                List<Msg> list = dms.get(to);
                if (list != null) list.add(r);
            }
        });
        Msg c = new Msg();
        c.id = m.id; c.sender = me; c.content = m.content; c.attUrl = m.attUrl; c.attType = m.attType; c.at = m.at;
        return c;
    }

    private void schedule(final int ms, final Runnable r) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(ms); } catch (InterruptedException e) { return; }
                r.run();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void setRead(String f, long id) {
        List<Msg> l = dms.get(f);
        if (l != null) for (Msg m : l) if (m.id == id) m.read = true;
    }

    public void recallDm(long id) throws ApiException {
        lag();
        for (List<Msg> l : dms.values()) for (Msg m : l) if (m.id == id) {
            if (!"@me".equals(m.sender)) throw new ApiException(403, "Bạn chỉ thu hồi được tin nhắn của mình");
            m.recalled = true; m.content = ""; m.attUrl = ""; m.attType = "";
            return;
        }
        throw new ApiException(404, "Không tìm thấy tin nhắn");
    }

    public ForwardResult forwardMessage(long messageId, String sourceType, List<String> friendUsernames, List<Long> roomIds) throws Exception {
        lag();
        ForwardResult fr = new ForwardResult();
        int f = friendUsernames == null ? 0 : friendUsernames.size();
        int r = roomIds == null ? 0 : roomIds.size();
        fr.totalRequested = f + r;
        fr.successCount = fr.totalRequested;
        return fr;
    }

    public void recallRoom(long roomId, long id) throws ApiException {
        lag();
        List<Msg> l = roomMsgs.get(roomId);
        if (l != null) for (Msg m : l) if (m.id == id) {
            if (!"@me".equals(m.sender)) throw new ApiException(403, "Bạn chỉ thu hồi được tin nhắn của mình");
            m.recalled = true; m.content = ""; m.attUrl = ""; m.attType = "";
            return;
        }
        throw new ApiException(404, "Không tìm thấy tin nhắn");
    }

    public void markRead(long id) {
        for (List<Msg> l : dms.values()) for (Msg m : l) if (m.id == id) m.read = true;
    }

    public List<Room> myRooms() {
        List<Room> out = new ArrayList<Room>();
        for (Room r : rooms) if (myRooms.contains(r.id)) out.add(r);
        return out;
    }

    public List<Room> allRooms() { return new ArrayList<Room>(rooms); }

    public Room createRoom(String name, List<String> initialMembers) throws Exception {
        lag();
        Room r = new Room();
        r.id = rooms.size() + 1;
        r.name = name;
        r.createdBy = me;
        rooms.add(r);
        myRooms.add(r.id);
        roomMsgs.put(r.id, new ArrayList<Msg>());
        return r;
    }

    public void joinRoom(long id) { if (!myRooms.contains(id)) myRooms.add(id); }

    public List<String> roomMembers(long id) {
        List<String> l = new ArrayList<String>();
        l.add(me);
        l.add("minh_anh");
        l.add("bao_tran");
        l.add("hoang_nam");
        return l;
    }

    public List<Msg> roomHistory(long id) { return view(roomMsgs.get(id)); }

    public Msg sendRoom(long id, String content, String url, String type) throws Exception {
        lag();
        Msg m = new Msg();
        m.id = ids++;
        m.sender = "@me";
        m.content = content;
        m.attUrl = url == null ? "" : url;
        m.attType = type == null ? "" : type;
        m.at = System.currentTimeMillis();
        roomMsgs.get(id).add(m);
        Msg c = new Msg();
        c.id = m.id; c.sender = me; c.content = content; c.attUrl = m.attUrl; c.attType = m.attType; c.at = m.at;
        return c;
    }

    public Upload upload(File f) throws Exception {
        lag();
        byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
        Upload u = new Upload();
        u.url = "demo://up" + (ids++) + Models.extOf(f.getName());
        u.name = f.getName();
        u.type = Models.kindOf(f.getName()).isEmpty() ? "IMAGE" : Models.kindOf(f.getName());
        files.put(u.url, data);
        return u;
    }

    public byte[] download(String url) throws Exception {
        byte[] b = files.get(url);
        if (b == null) throw new ApiException(404, "Không tìm thấy tệp");
        return b;
    }

    public Profile profile(String u) {
        Profile p = new Profile();
        p.username = u;
        p.email = u + "@gmail.com";
        for (int i = 0; i < friendNames.length; i++) if (friendNames[i].equals(u)) { p.fullName = full[i]; p.bio = bios[i]; }
        if (u.equals(me)) { p.fullName = ""; p.bio = "Xin chào, mình dùng ChatWave"; }
        return p;
    }

    public Profile updateMyProfile(String fullName, String bio, String avatarUrl) {
        Profile p = profile(me);
        p.fullName = fullName;
        p.bio = bio;
        if (avatarUrl != null && !avatarUrl.isEmpty()) p.avatarUrl = avatarUrl;
        return p;
    }

    public List<AdminUser> adminUsers() { return new ArrayList<AdminUser>(users); }

    public void adminSetRole(long id, String role) { for (AdminUser u : users) if (u.id == id) u.role = role; }

    public void adminDeleteUser(long id) {
        for (int i = 0; i < users.size(); i++) if (users.get(i).id == id) { users.remove(i); return; }
    }

    public List<ReqLog> requestLogs(int size) {
        String[] urls = {"/api/auth/login", "/api/friends", "/api/direct-messages/minh_anh", "/api/rooms/mine", "/api/direct-messages/conversations", "/api/profile/me", "/api/files/upload", "/api/admin/users"};
        String[] meth = {"POST", "GET", "GET", "GET", "GET", "GET", "POST", "GET"};
        int[] st = {200, 200, 200, 200, 200, 200, 201, 200};
        List<ReqLog> l = new ArrayList<ReqLog>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < Math.min(size, 40); i++) {
            ReqLog r = new ReqLog();
            int k = rnd.nextInt(urls.length);
            r.at = now - i * 7000L;
            r.method = meth[k]; r.url = urls[k]; r.status = i % 11 == 5 ? 403 : i % 17 == 9 ? 500 : st[k];
            r.ip = "127.0.0.1"; r.username = friendNames[rnd.nextInt(friendNames.length)]; r.ms = 4 + rnd.nextInt(90);
            l.add(r);
        }
        return l;
    }

    public List<ErrLog> errorLogs(int size) {
        List<ErrLog> l = new ArrayList<ErrLog>();
        String[][] d = {{"ForbiddenException", "Hai bạn chưa là bạn bè", "/api/direct-messages", "POST", "403"},
            {"NotFoundException", "Không tìm thấy người dùng", "/api/friends/request", "POST", "404"},
            {"BadCredentialsException", "Sai tên đăng nhập hoặc mật khẩu", "/api/auth/login", "POST", "401"},
            {"NullPointerException", "Cannot invoke \"String.length()\"", "/api/profile/me", "PUT", "500"}};
        for (int i = 0; i < d.length; i++) {
            ErrLog e = new ErrLog();
            e.at = System.currentTimeMillis() - i * 600000L;
            e.type = d[i][0]; e.message = d[i][1]; e.path = d[i][2]; e.method = d[i][3]; e.status = Integer.parseInt(d[i][4]);
            e.user = friendNames[i];
            l.add(e);
        }
        return l;
    }
}
