package chatwave.core;

import chatwave.core.Models.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gọi REST API Spring Boot của nhóm (JWT Bearer). Đường dẫn lấy từ các Controller trong repo. */
public class RemoteBackend implements Backend {
    private final String base;
    private String token;
    private String me = "";

    public RemoteBackend(String baseUrl) {
        String b = baseUrl.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        this.base = b;
    }

    // ---------------- HTTP ----------------
    private Object call(String method, String path, Object body) throws Exception {
        HttpURLConnection c = open(method, path);
        c.setRequestProperty("Accept", "application/json");
        if (body != null) {
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setDoOutput(true);
            byte[] bytes = (body instanceof String)
                    ? ((String) body).getBytes(StandardCharsets.UTF_8)
                    : Json.write(body).getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            OutputStream o = c.getOutputStream();
            o.write(bytes);
            o.close();
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(0);
            OutputStream o = c.getOutputStream();
            o.close();
        }
        return readJson(c);
    }

    private HttpURLConnection open(String method, String path) throws IOException {
        HttpURLConnection c;
        try {
            c = (HttpURLConnection) new URL(base + path).openConnection();
        } catch (IOException e) {
            throw new IOException("Địa chỉ máy chủ không hợp lệ");
        }
        c.setConnectTimeout(6000);
        c.setReadTimeout(15000);
        c.setRequestMethod(method);
        if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
        return c;
    }

    private Object readJson(HttpURLConnection c) throws Exception {
        int code;
        try {
            code = c.getResponseCode();
        } catch (IOException e) {
            throw new Models.ApiException(0, "Không kết nối được máy chủ. Hãy kiểm tra backend đã chạy chưa (" + base + ")");
        }
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = in == null ? "" : new String(readAll(in), StandardCharsets.UTF_8);
        if (code >= 400) {
            String msg = "";
            try {
                Object o = Json.parse(text);
                if (o instanceof Map) {
                    Map<String, Object> m = Json.obj(o);
                    msg = Json.str(m, "message");
                    if (msg.isEmpty()) msg = Json.str(m, "error");
                    Map<String, Object> d = Json.obj(m.get("details"));
                    if (!d.isEmpty()) msg = String.valueOf(d.values().iterator().next());
                }
            } catch (Exception ignore) { /* không phải JSON */ }
            if (msg.isEmpty()) {
                msg = code == 401 ? "Phiên đăng nhập hết hạn hoặc sai thông tin"
                        : code == 403 ? "Bạn không có quyền thực hiện thao tác này"
                        : code == 404 ? "Không tìm thấy dữ liệu" : "Lỗi máy chủ (" + code + ")";
            }
            throw new Models.ApiException(code, msg);
        }
        try {
            return Json.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); } catch (Exception e) { return s; }
    }

    static long time(Object v) {
        if (!(v instanceof String)) return 0L;
        try {
            return LocalDateTime.parse((String) v).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Msg msg(Map<String, Object> m) {
        Msg x = new Msg();
        x.id = Json.num(m, "id");
        x.sender = Json.str(m, "sender");
        x.content = Json.str(m, "content");
        x.attUrl = Json.str(m, "attachmentUrl");
        x.attType = Json.str(m, "attachmentType");
        x.read = Json.bool(m, "isRead") || Json.bool(m, "read");
        x.recalled = Json.bool(m, "recalled") || Json.bool(m, "isRecalled") || Json.bool(m, "deleted");
        x.forwarded = Json.bool(m, "forwarded") || Json.bool(m, "isForwarded");
        if (x.recalled) { x.content = ""; x.attUrl = ""; x.attType = ""; }
        x.at = time(m.get("sentAt"));
        return x;
    }

    private static List<Msg> msgs(Object o) {
        List<Msg> l = new ArrayList<Msg>();
        for (Object e : Json.arr(o)) l.add(msg(Json.obj(e)));
        Collections.sort(l, new Comparator<Msg>() {
            public int compare(Msg a, Msg b) { return a.at != b.at ? Long.compare(a.at, b.at) : Long.compare(a.id, b.id); }
        });
        return l;
    }

    // ---------------- tài khoản ----------------
    public Account login(String u, String p) throws Exception {
        Map<String, Object> r = Json.obj(call("POST", "/api/auth/login", Json.map("username", u, "password", p)));
        Account a = new Account();
        a.token = Json.str(r, "token");
        a.username = Json.str(r, "username").isEmpty() ? u : Json.str(r, "username");
        a.role = Json.str(r, "role").replace("ROLE_", "");
        if (a.token.isEmpty()) throw new Models.ApiException(0, "Máy chủ không trả về token");
        token = a.token;
        me = a.username;
        return a;
    }

    public void register(String u, String email, String p) throws Exception {
        call("POST", "/api/auth/register", Json.map("username", u, "email", email, "password", p));
    }

    public void logout() { token = null; }

    public void recallDm(long id) throws Exception {
        try { call("PUT", "/api/direct-messages/" + id + "/recall", "{}"); }
        catch (Models.ApiException e) { throw recallMissing(e); }
    }

    public void recallRoom(long roomId, long id) throws Exception {
        try { call("PUT", "/api/rooms/" + roomId + "/messages/" + id + "/recall", "{}"); }
        catch (Models.ApiException e) { throw recallMissing(e); }
    }

    private static Models.ApiException recallMissing(Models.ApiException e) {
        if (e.status == 404 || e.status == 405) return new Models.ApiException(e.status, "Không thu hồi được (tin không tồn tại hoặc backend chưa hỗ trợ thu hồi tin nhắn)");
        return e;
    }

    /** Cần backend có POST /api/auth/forgot-password {email} và /api/auth/reset-password {email, code, newPassword}. */
    public void forgotPassword(String email) throws Exception {
        try { call("POST", "/api/auth/forgot-password", Json.map("email", email)); }
        catch (Models.ApiException e) { throw missing(e); }
    }

    public void resetPassword(String email, String code, String newPassword) throws Exception {
        try { call("POST", "/api/auth/reset-password", Json.map("email", email, "code", code, "newPassword", newPassword)); }
        catch (Models.ApiException e) { throw missing(e); }
    }

    public void changePassword(String oldPassword, String newPassword) throws Exception {
        call("POST", "/api/auth/change-password", Json.map("oldPassword", oldPassword, "newPassword", newPassword));
    }

    private static Models.ApiException missing(Models.ApiException e) {
        if (e.status == 404 || e.status == 405) return new Models.ApiException(e.status, "Máy chủ chưa hỗ trợ chức năng này (backend cần thêm API quên mật khẩu)");
        return e;
    }

    // ---------------- bạn bè ----------------
    public List<Friend> friends() throws Exception {
        Map<String, Friend> map = new HashMap<String, Friend>();
        List<Friend> out = new ArrayList<Friend>();
        for (Object o : Json.arr(call("GET", "/api/friends", null))) {
            Friend f = new Friend();
            f.username = Json.str(Json.obj(o), "friendUsername");
            if (f.username.isEmpty()) continue;
            map.put(f.username, f);
            out.add(f);
        }
        for (Object o : Json.arr(call("GET", "/api/direct-messages/conversations", null))) {
            Map<String, Object> m = Json.obj(o);
            Friend f = map.get(Json.str(m, "friendUsername"));
            if (f == null) continue;
            f.lastMessage = Json.str(m, "lastMessage");
            f.lastSender = Json.str(m, "lastSender");
            f.lastAt = time(m.get("lastSentAt"));
            f.unread = (int) Json.num(m, "unreadCount");
        }
        return out;
    }

    public List<Req> pendingRequests() throws Exception {
        List<Req> out = new ArrayList<Req>();
        for (Object o : Json.arr(call("GET", "/api/friends/requests/pending", null))) {
            Map<String, Object> m = Json.obj(o);
            Req r = new Req();
            r.id = Json.num(m, "id");
            r.requester = Json.str(m, "requester");
            r.addressee = Json.str(m, "addressee");
            r.at = time(m.get("createdAt"));
            out.add(r);
        }
        return out;
    }

    public void sendRequest(String t) throws Exception { call("POST", "/api/friends/request", Json.map("targetUsername", t)); }
    public void accept(long id) throws Exception { call("POST", "/api/friends/requests/" + id + "/accept", null); }
    public void decline(long id) throws Exception { call("POST", "/api/friends/requests/" + id + "/decline", null); }
    public void unfriend(String u) throws Exception { call("DELETE", "/api/friends/" + enc(u), null); }

    // ---------------- tin nhắn riêng ----------------
    public List<Msg> dmHistory(String friend) throws Exception {
        return msgs(call("GET", "/api/direct-messages/" + enc(friend) + "?page=0&size=100", null));
    }

    public Msg sendDm(String to, String content, String url, String type) throws Exception {
        Map<String, Object> b = Json.map("recipient", to, "content", content);
        if (url != null && !url.isEmpty()) { b.put("attachmentUrl", url); b.put("attachmentType", type); }
        return msg(Json.obj(call("POST", "/api/direct-messages", b)));
    }

    public void markRead(long id) throws Exception { call("PUT", "/api/direct-messages/" + id + "/read", null); }

    // ---------------- phòng chat ----------------
    private static List<Room> rooms(Object o) {
        List<Room> l = new ArrayList<Room>();
        for (Object e : Json.arr(o)) {
            Map<String, Object> m = Json.obj(e);
            Room r = new Room();
            r.id = Json.num(m, "id");
            r.name = Json.str(m, "name");
            r.createdBy = Json.str(m, "createdBy");
            l.add(r);
        }
        return l;
    }

    public List<Room> myRooms() throws Exception { return rooms(call("GET", "/api/rooms/mine", null)); }
    public List<Room> allRooms() throws Exception { return rooms(call("GET", "/api/rooms", null)); }

    public Room createRoom(String name, List<String> initialMembers) throws Exception {
        Map<String, Object> body = Json.map("name", name);
        if (initialMembers != null && !initialMembers.isEmpty()) body.put("initialMembers", initialMembers);
        Map<String, Object> m = Json.obj(call("POST", "/api/rooms", body));
        Room r = new Room();
        r.id = Json.num(m, "id");
        r.name = Json.str(m, "name");
        r.createdBy = Json.str(m, "createdBy");
        return r;
    }

    public void joinRoom(long id) throws Exception { call("POST", "/api/rooms/" + id + "/join", null); }

    public List<String> roomMembers(long id) throws Exception {
        List<String> l = new ArrayList<String>();
        for (Object o : Json.arr(call("GET", "/api/rooms/" + id + "/members", null))) l.add(String.valueOf(o));
        return l;
    }

    public List<Msg> roomHistory(long id) throws Exception {
        return msgs(call("GET", "/api/rooms/" + id + "/messages?page=0&size=100", null));
    }

    public Msg sendRoom(long id, String content, String url, String type) throws Exception {
        Map<String, Object> b = Json.map("content", content);
        if (url != null && !url.isEmpty()) { b.put("attachmentUrl", url); b.put("attachmentType", type); }
        return msg(Json.obj(call("POST", "/api/rooms/" + id + "/messages", b)));
    }

    public ForwardResult forwardMessage(long messageId, String sourceType, List<String> friendUsernames, List<Long> roomIds) throws Exception {
        Map<String, Object> body = Json.map("sourceType", sourceType, "friendUsernames", friendUsernames, "roomIds", roomIds);
        Map<String, Object> res = Json.obj(call("POST", "/api/messages/" + messageId + "/forward", body));
        ForwardResult out = new ForwardResult();
        out.totalRequested = (int) Json.num(res, "totalRequested");
        out.successCount = (int) Json.num(res, "successCount");
        out.failedCount = (int) Json.num(res, "failedCount");
        Object arr = res.get("results");
        if (arr != null) {
            for (Object o : Json.arr(arr)) {
                Map<String, Object> r = Json.obj(o);
                if (!Json.bool(r, "success")) {
                    out.failedDetails.add(Json.str(r, "target") + " (" + Json.str(r, "error") + ")");
                }
            }
        }
        return out;
    }

    // ---------------- tệp ----------------
    public Upload upload(File f) throws Exception {
        String boundary = "----ChatWave" + System.nanoTime();
        HttpURLConnection c = open("POST", "/api/files/upload");
        c.setDoOutput(true);
        c.setChunkedStreamingMode(16384);
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream o = c.getOutputStream();
        String ext = Models.extOf(f.getName());
        String mime = ext.equals(".png") ? "image/png" : ext.equals(".gif") ? "image/gif" : ext.equals(".webp") ? "image/webp"
                : ext.equals(".mp3") ? "audio/mpeg" : ext.equals(".wav") ? "audio/wav" : ext.equals(".ogg") ? "audio/ogg"
                : ext.equals(".m4a") ? "audio/x-m4a" : ext.equals(".webm") ? "audio/webm" : ext.equals(".mp4") ? "video/mp4"
                : ext.equals(".mov") ? "video/quicktime" : ext.equals(".jpg") || ext.equals(".jpeg") ? "image/jpeg" : "application/octet-stream";
        String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + f.getName().replace("\"", "") + "\"\r\nContent-Type: " + mime + "\r\n\r\n";
        o.write(head.getBytes(StandardCharsets.UTF_8));
        FileInputStream in = new FileInputStream(f);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        in.close();
        o.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        o.close();
        Map<String, Object> m = Json.obj(readJson(c));
        Upload u = new Upload();
        u.url = Json.str(m, "fileUrl");
        String kind = Models.kindOf(f.getName());
        u.type = !Json.str(m, "fileType").isEmpty() ? Json.str(m, "fileType") : kind.isEmpty() ? "IMAGE" : kind;
        u.name = Json.str(m, "originalName");
        return u;
    }

    public byte[] download(String url) throws Exception {
        String path = url == null ? "" : url.trim();
        if (path.startsWith(base)) {
            path = path.substring(base.length());
        } else if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(path);
                path = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
            } catch (Exception ignore) {}
        }
        HttpURLConnection c = open("GET", path.startsWith("/") ? path : "/" + path);
        int code = c.getResponseCode();
        if (code >= 400) throw new Models.ApiException(code, "Không tải được tệp (" + code + ")");
        return readAll(c.getInputStream());
    }

    // ---------------- hồ sơ ----------------
    private static Profile profile(Map<String, Object> m) {
        Profile p = new Profile();
        p.username = Json.str(m, "username");
        p.email = Json.str(m, "email");
        p.fullName = Json.str(m, "fullName");
        p.bio = Json.str(m, "bio");
        p.avatarUrl = Json.str(m, "avatarUrl");
        return p;
    }

    public Profile profile(String u) throws Exception {
        return profile(Json.obj(call("GET", u.equals(me) ? "/api/profile/me" : "/api/profile/" + enc(u), null)));
    }

    public Profile updateMyProfile(String fullName, String bio, String avatarUrl) throws Exception {
        Map<String, Object> body = Json.map("fullName", fullName, "bio", bio);
        if (avatarUrl != null && !avatarUrl.isEmpty()) body.put("avatarUrl", avatarUrl);
        return profile(Json.obj(call("PUT", "/api/profile/me", body)));
    }

    // ---------------- quản trị ----------------
    public List<AdminUser> adminUsers() throws Exception {
        List<AdminUser> l = new ArrayList<AdminUser>();
        for (Object o : Json.arr(call("GET", "/api/admin/users", null))) {
            Map<String, Object> m = Json.obj(o);
            AdminUser u = new AdminUser();
            u.id = Json.num(m, "id");
            u.username = Json.str(m, "username");
            u.email = Json.str(m, "email");
            u.role = Json.str(m, "role");
            l.add(u);
        }
        return l;
    }

    public void adminSetRole(long id, String role) throws Exception { call("PUT", "/api/admin/users/" + id + "/role", Json.map("role", role)); }
    public void adminDeleteUser(long id) throws Exception { call("DELETE", "/api/admin/users/" + id, null); }

    public List<ReqLog> requestLogs(int size) throws Exception {
        List<ReqLog> l = new ArrayList<ReqLog>();
        for (Object o : Json.arr(call("GET", "/api/logs?size=" + size, null))) {
            Map<String, Object> m = Json.obj(o);
            ReqLog r = new ReqLog();
            r.at = time(m.get("createdAt"));
            r.method = Json.str(m, "method");
            r.url = Json.str(m, "url");
            r.ip = Json.str(m, "ip");
            r.username = Json.str(m, "username");
            r.status = (int) Json.num(m, "status");
            r.ms = Json.num(m, "durationMs");
            l.add(r);
        }
        return l;
    }

    public List<ErrLog> errorLogs(int size) throws Exception {
        List<ErrLog> l = new ArrayList<ErrLog>();
        Object o = call("GET", "/api/admin/error-logs?page=0&size=" + size, null);
        List<Object> items = o instanceof List ? Json.arr(o) : Json.arr(Json.obj(o).get("content"));
        for (Object e : items) {
            Map<String, Object> m = Json.obj(e);
            ErrLog r = new ErrLog();
            r.at = time(m.get("time"));
            r.status = (int) Json.num(m, "status");
            r.type = Json.str(m, "exceptionType");
            r.message = Json.str(m, "message");
            r.path = Json.str(m, "path");
            r.method = Json.str(m, "method");
            r.user = Json.str(m, "userId");
            l.add(r);
        }
        return l;
    }
}
