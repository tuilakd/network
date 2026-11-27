package chatwave.core;

/** Các lớp dữ liệu mà giao diện dùng (khớp với JSON của backend Spring Boot). */
public final class Models {
    /** Đoán loại tệp theo đuôi: IMAGE / AUDIO / VIDEO / "" (không hỗ trợ). Khớp quy ước của backend. */
    public static String kindOf(String name) {
        String n = name == null ? "" : name.toLowerCase();
        int i = n.lastIndexOf('.');
        String e = i < 0 ? "" : n.substring(i + 1);
        if (e.equals("jpg") || e.equals("jpeg") || e.equals("png") || e.equals("gif") || e.equals("webp")) return "IMAGE";
        if (e.equals("mp3") || e.equals("wav") || e.equals("ogg") || e.equals("m4a") || e.equals("webm")) return "AUDIO";
        if (e.equals("mp4") || e.equals("mov") || e.equals("avi") || e.equals("mkv")) return "VIDEO";
        return "";
    }

    public static String extOf(String name) {
        String n = name == null ? "" : name;
        int i = n.lastIndexOf('.');
        return i < 0 || i < n.length() - 6 ? "" : n.substring(i).toLowerCase();
    }

    private Models() {}

    public static class Account {
        public String username, role, token;
        public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
    }

    /** Bạn bè kèm tin nhắn cuối và số tin chưa đọc. */
    public static class Friend {
        public String username = "", lastMessage = "", lastSender = "";
        public long lastAt;
        public int unread;
    }

    /** Lời mời kết bạn. */
    public static class Req {
        public long id;
        public String requester = "", addressee = "";
        public long at;
    }

    public static class Room {
        public long id;
        public String name = "", createdBy = "";
    }

    /** Tin nhắn riêng hoặc tin trong phòng. */
    public static class Msg {
        public long id;
        public String sender = "", content = "", attUrl = "", attType = "";
        public boolean read, recalled, forwarded;
        public long at;
        public boolean isImage() { return !recalled && attUrl != null && !attUrl.isEmpty() && "IMAGE".equalsIgnoreCase(attType); }
        public boolean isAudio() { return !recalled && attUrl != null && !attUrl.isEmpty() && "AUDIO".equalsIgnoreCase(attType); }
        public boolean isVideo() { return !recalled && attUrl != null && !attUrl.isEmpty() && "VIDEO".equalsIgnoreCase(attType); }
        public boolean isMedia() { return isAudio() || isVideo(); }
    }

    public static class Profile {
        public String username = "", email = "", fullName = "", bio = "", avatarUrl = "";
        public String display() { return fullName != null && !fullName.trim().isEmpty() ? fullName : username; }
    }

    public static class Upload {
        public String url = "", type = "IMAGE", name = "";
    }

    public static class AdminUser {
        public long id;
        public String username = "", email = "", role = "USER";
    }

    public static class ReqLog {
        public long at;
        public String method = "", url = "", ip = "", username = "";
        public int status;
        public long ms;
    }

    public static class ErrLog {
        public long at;
        public int status;
        public String type = "", message = "", path = "", method = "", user = "";
    }

    public static class ForwardResult {
        public int totalRequested, successCount, failedCount;
        public java.util.List<String> failedDetails = new java.util.ArrayList<String>();
    }

    public static class ApiException extends Exception {
        public final int status;
        public ApiException(int status, String msg) { super(msg); this.status = status; }
    }
}
