package chatwave.ui;

import chatwave.core.Async;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Kho dữ liệu emoji Twemoji (PNG 72x72) kèm tên tiếng Việt, phân nhóm và cache ảnh bất đồng bộ. */
public final class EmojiRegistry {
    public static final class Entry {
        public final String unicode;
        public final String hex;
        public final String name;
        public final int category;
        Entry(String unicode, String hex, String name, int category) {
            this.unicode = unicode;
            this.hex = hex;
            this.name = name;
            this.category = category;
        }
    }

    public static final String[] TAB_NAMES = {
            "Gần đây", "Mặt cười", "Cử chỉ", "Trái tim", "Động vật", "Đồ ăn", "Hoạt động", "Đồ vật"
    };
    public static final String[] TAB_HEX = {
            "23f0", "1f600", "1f44d", "2764", "1f436", "1f354", "26bd", "1f4a1"
    };

    private static final List<Entry> ALL = new ArrayList<Entry>();
    private static final Map<String, Entry> BY_HEX = new HashMap<String, Entry>();
    private static final Map<String, String> UNICODE_TO_HEX = new HashMap<String, String>();
    private static final Map<String, BufferedImage> RAW = new ConcurrentHashMap<String, BufferedImage>();
    private static final Map<String, BufferedImage> SCALED = new ConcurrentHashMap<String, BufferedImage>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static int maxLen = 1;

    static {
        // 1: Mặt cười
        add(1, "1f600", "Cười tươi"); add(1, "1f601", "Nhe răng"); add(1, "1f602", "Cười ra nước mắt"); add(1, "1f923", "Cười lăn lộn");
        add(1, "1f603", "Cười rạng rỡ"); add(1, "1f604", "Cười híp mắt"); add(1, "1f605", "Cười đổ mồ hôi"); add(1, "1f606", "Cười sảng khoái");
        add(1, "1f609", "Nháy mắt"); add(1, "1f60a", "Mỉm cười"); add(1, "1f607", "Thiên thần"); add(1, "1f970", "Yêu thương");
        add(1, "1f60d", "Mắt trái tim"); add(1, "1f929", "Mắt ngôi sao"); add(1, "1f618", "Hôn gió"); add(1, "1f617", "Hôn");
        add(1, "1f61a", "Hôn thẹn thùng"); add(1, "1f60b", "Ngon miệng"); add(1, "1f61b", "Lè lưỡi"); add(1, "1f61c", "Nháy mắt lè lưỡi");
        add(1, "1f92a", "Tinh nghịch"); add(1, "1f61d", "Lè lưỡi híp mắt"); add(1, "1f911", "Mê tiền"); add(1, "1f917", "Ôm");
        add(1, "1f92d", "Che miệng cười"); add(1, "1f92b", "Suỵt"); add(1, "1f914", "Suy nghĩ"); add(1, "1f910", "Khóa miệng");
        add(1, "1f928", "Nghi ngờ"); add(1, "1f610", "Bình thản"); add(1, "1f611", "Không cảm xúc"); add(1, "1f60f", "Cười khẩy");
        add(1, "1f612", "Chán nản"); add(1, "1f644", "Đảo mắt"); add(1, "1f62c", "Nhăn mặt"); add(1, "1f60c", "Nhẹ nhõm");
        add(1, "1f614", "Buồn bã"); add(1, "1f634", "Ngủ"); add(1, "1f637", "Đeo khẩu trang"); add(1, "1f912", "Ốm");
        add(1, "1f922", "Buồn nôn"); add(1, "1f975", "Nóng bức"); add(1, "1f976", "Lạnh cóng"); add(1, "1f92f", "Nổ não");
        add(1, "1f973", "Tiệc tùng"); add(1, "1f60e", "Ngầu"); add(1, "1f913", "Mọt sách"); add(1, "1f9d0", "Kính lúp");
        add(1, "1f62e", "Ngạc nhiên"); add(1, "1f632", "Kinh ngạc"); add(1, "1f633", "Đỏ mặt"); add(1, "1f97a", "Long lanh");
        add(1, "1f622", "Khóc"); add(1, "1f62d", "Khóc lớn"); add(1, "1f631", "Hét sợ hãi"); add(1, "1f621", "Giận dữ");
        add(1, "1f620", "Tức giận"); add(1, "1f92c", "Chửi thề"); add(1, "1f608", "Tiểu quỷ"); add(1, "1f480", "Đầu lâu");
        add(1, "1f4a9", "Cục phân"); add(1, "1f921", "Chú hề"); add(1, "1f47b", "Con ma"); add(1, "1f916", "Robot");

        // 2: Cử chỉ
        add(2, "1f44b", "Vẫy tay"); add(2, "1f91a", "Giơ tay"); add(2, "1f590", "Bàn tay xòe"); add(2, "270b", "Dừng lại");
        add(2, "1f44c", "OK"); add(2, "1f90f", "Một chút"); add(2, "270c", "Chiến thắng"); add(2, "1f91e", "Chúc may mắn");
        add(2, "1f91f", "Yêu bạn"); add(2, "1f918", "Rock"); add(2, "1f919", "Gọi nhé"); add(2, "1f448", "Chỉ trái");
        add(2, "1f449", "Chỉ phải"); add(2, "1f446", "Chỉ lên"); add(2, "1f447", "Chỉ xuống"); add(2, "261d", "Số một");
        add(2, "1f44d", "Thích"); add(2, "1f44e", "Không thích"); add(2, "270a", "Nắm đấm"); add(2, "1f44a", "Cụng tay");
        add(2, "1f44f", "Vỗ tay"); add(2, "1f64c", "Hoan hô"); add(2, "1f450", "Mở tay"); add(2, "1f932", "Xin");
        add(2, "1f91d", "Bắt tay"); add(2, "1f64f", "Cảm ơn"); add(2, "1f4aa", "Mạnh mẽ"); add(2, "1f9be", "Tay máy");

        // 3: Trái tim & Cảm xúc
        add(3, "2764", "Tim đỏ"); add(3, "1f9e1", "Tim cam"); add(3, "1f49b", "Tim vàng"); add(3, "1f49a", "Tim xanh lá");
        add(3, "1f499", "Tim xanh dương"); add(3, "1f49c", "Tim tím"); add(3, "1f5a4", "Tim đen"); add(3, "1f90d", "Tim trắng");
        add(3, "1f90e", "Tim nâu"); add(3, "1f494", "Tan vỡ"); add(3, "2763", "Tim chấm than"); add(3, "1f495", "Hai trái tim");
        add(3, "1f49e", "Tim xoay"); add(3, "1f493", "Tim đập"); add(3, "1f497", "Tim lớn dần"); add(3, "1f496", "Tim lấp lánh");
        add(3, "1f498", "Mũi tên tình yêu"); add(3, "1f49d", "Quà trái tim"); add(3, "1f48b", "Dấu hôn"); add(3, "1f4af", "100 điểm");
        add(3, "1f4a2", "Tức giận"); add(3, "1f4a5", "Bùng nổ"); add(3, "1f4a6", "Giọt mồ hôi"); add(3, "1f525", "Cháy");
        add(3, "2728", "Lấp lánh"); add(3, "1f31f", "Ngôi sao sáng"); add(3, "1f389", "Pháo giấy"); add(3, "1f38a", "Chúc mừng");

        // 4: Động vật
        add(4, "1f436", "Chó"); add(4, "1f431", "Mèo"); add(4, "1f42d", "Chuột"); add(4, "1f439", "Hamster");
        add(4, "1f430", "Thỏ"); add(4, "1f98a", "Cáo"); add(4, "1f43b", "Gấu"); add(4, "1f43c", "Gấu trúc");
        add(4, "1f428", "Koala"); add(4, "1f42f", "Hổ"); add(4, "1f981", "Sư tử"); add(4, "1f42e", "Bò");
        add(4, "1f437", "Heo"); add(4, "1f438", "Ếch"); add(4, "1f435", "Khỉ"); add(4, "1f648", "Khỉ che mắt");
        add(4, "1f649", "Khỉ bịt tai"); add(4, "1f64a", "Khỉ bịt miệng"); add(4, "1f414", "Gà"); add(4, "1f427", "Chim cánh cụt");
        add(4, "1f425", "Gà con"); add(4, "1f986", "Vịt"); add(4, "1f985", "Đại bàng"); add(4, "1f989", "Cú mèo");
        add(4, "1f43a", "Sói"); add(4, "1f434", "Ngựa"); add(4, "1f984", "Kỳ lân"); add(4, "1f426", "Chim");

        // 5: Đồ ăn & Thức uống
        add(5, "1f34e", "Táo đỏ"); add(5, "1f34a", "Cam"); add(5, "1f34b", "Chanh"); add(5, "1f34c", "Chuối");
        add(5, "1f349", "Dưa hấu"); add(5, "1f347", "Nho"); add(5, "1f353", "Dâu tây"); add(5, "1f352", "Anh đào");
        add(5, "1f351", "Đào"); add(5, "1f34d", "Dứa"); add(5, "1f96d", "Xoài"); add(5, "1f951", "Bơ");
        add(5, "1f354", "Burger"); add(5, "1f355", "Pizza"); add(5, "1f32d", "Hotdog"); add(5, "1f35f", "Khoai tây chiên");
        add(5, "1f35c", "Mì"); add(5, "1f363", "Sushi"); add(5, "1f370", "Bánh kem"); add(5, "1f382", "Bánh sinh nhật");
        add(5, "1f369", "Donut"); add(5, "1f36a", "Bánh quy"); add(5, "1f36b", "Sô cô la"); add(5, "1f366", "Kem");
        add(5, "2615", "Cà phê"); add(5, "1f9cb", "Trà sữa"); add(5, "1f37a", "Bia"); add(5, "1f37b", "Cụng ly");
        add(5, "1f942", "Sâm panh"); add(5, "1f377", "Rượu vang"); add(5, "1f379", "Cocktail"); add(5, "1f345", "Cà chua");

        // 6: Hoạt động
        add(6, "26bd", "Bóng đá"); add(6, "1f3c0", "Bóng rổ"); add(6, "1f3c8", "Bóng bầu dục"); add(6, "26be", "Bóng chày");
        add(6, "1f3be", "Tennis"); add(6, "1f3d0", "Bóng chuyền"); add(6, "1f3b1", "Bi-a"); add(6, "1f3d3", "Bóng bàn");
        add(6, "1f3f8", "Cầu lông"); add(6, "1f3ae", "Chơi game"); add(6, "1f3b2", "Xúc xắc"); add(6, "1f3af", "Mục tiêu");
        add(6, "1f3b5", "Nốt nhạc"); add(6, "1f3b6", "Âm nhạc"); add(6, "1f3a4", "Micro"); add(6, "1f3a7", "Tai nghe");
        add(6, "1f3b8", "Guitar"); add(6, "1f3ac", "Phim ảnh"); add(6, "1f3a8", "Hội họa"); add(6, "1f3c6", "Cúp");
        add(6, "1f947", "Huy chương vàng"); add(6, "1f948", "Huy chương bạc"); add(6, "1f949", "Huy chương đồng"); add(6, "1f381", "Quà tặng");
        add(6, "1f388", "Bóng bay"); add(6, "1f383", "Halloween"); add(6, "1f384", "Cây thông"); add(6, "1f386", "Pháo hoa");

        // 7: Đồ vật
        add(7, "1f4a1", "Ý tưởng"); add(7, "1f4f1", "Điện thoại"); add(7, "1f4bb", "Laptop"); add(7, "2328", "Bàn phím");
        add(7, "1f5a5", "Máy tính"); add(7, "1f4f7", "Máy ảnh"); add(7, "1f4fa", "Tivi"); add(7, "1f4da", "Sách");
        add(7, "1f4d6", "Sách mở"); add(7, "270f", "Bút chì"); add(7, "1f4dd", "Ghi chú"); add(7, "1f4cc", "Ghim");
        add(7, "1f4ce", "Kẹp giấy"); add(7, "1f511", "Chìa khóa"); add(7, "1f512", "Khóa"); add(7, "1f513", "Mở khóa");
        add(7, "1f514", "Chuông"); add(7, "23f0", "Đồng hồ báo thức"); add(7, "231a", "Đồng hồ"); add(7, "1f4b0", "Túi tiền");
        add(7, "1f4b8", "Tiền bay"); add(7, "1f48e", "Kim cương"); add(7, "1f680", "Tên lửa"); add(7, "2708", "Máy bay");
        add(7, "1f697", "Ô tô"); add(7, "1f6b2", "Xe đạp"); add(7, "1f308", "Cầu vồng"); add(7, "2b50", "Ngôi sao");
    }

    private EmojiRegistry() {}

    private static void add(int cat, String hex, String name) {
        String uni = hexToUnicode(hex);
        Entry e = new Entry(uni, hex, name, cat);
        ALL.add(e);
        BY_HEX.put(hex, e);
        UNICODE_TO_HEX.put(uni, hex);
        UNICODE_TO_HEX.put(uni + "\uFE0F", hex);
        maxLen = Math.max(maxLen, uni.length() + 1);
    }

    public static String hexToUnicode(String hex) {
        StringBuilder sb = new StringBuilder();
        for (String part : hex.split("-")) {
            sb.appendCodePoint(Integer.parseInt(part, 16));
        }
        return sb.toString();
    }

    public static List<Entry> byCategory(int cat) {
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : ALL) if (e.category == cat) out.add(e);
        return out;
    }

    public static Entry byHex(String hex) { return BY_HEX.get(hex); }

    public static String findHex(String unicode) { return UNICODE_TO_HEX.get(unicode); }

    public static int maxUnicodeLength() { return maxLen; }

    public static boolean hasHex(String hex) { return BY_HEX.containsKey(hex) || Arrays.asList(TAB_HEX).contains(hex); }

    /** Lấy ảnh emoji theo kích thước (null nếu đang nạp ở nền; tự gọi onLoaded khi xong). */
    public static BufferedImage image(final String hex, final int size, final Runnable onLoaded) {
        if (hex == null || hex.isEmpty() || MISSING.contains(hex)) return null;
        String key = hex + "@" + size;
        BufferedImage s = SCALED.get(key);
        if (s != null) return s;

        BufferedImage raw = RAW.get(hex);
        if (raw != null) {
            s = scale(raw, size);
            SCALED.put(key, s);
            return s;
        }

        if (LOADING.add(hex)) {
            Async.run(new Callable<BufferedImage>() {
                public BufferedImage call() throws Exception {
                    BufferedImage img = null;
                    InputStream is = EmojiRegistry.class.getResourceAsStream("/resources/emoji/" + hex + ".png");
                    if (is != null) {
                        try { img = ImageIO.read(is); } finally { is.close(); }
                    } else {
                        File f = new File("clientswing/clientswing/src/resources/emoji/" + hex + ".png");
                        if (f.exists()) img = ImageIO.read(f);
                    }
                    if (img == null) throw new Exception("Missing emoji " + hex);
                    return img;
                }
            }, new Async.Ok<BufferedImage>() {
                public void accept(BufferedImage v) {
                    LOADING.remove(hex);
                    RAW.put(hex, v);
                    SCALED.put(hex + "@" + size, scale(v, size));
                    if (onLoaded != null) onLoaded.run();
                }
            }, new Async.Fail() {
                public void accept(Exception e) {
                    LOADING.remove(hex);
                    MISSING.add(hex);
                }
            });
        }
        return null;
    }

    private static BufferedImage scale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }
}
