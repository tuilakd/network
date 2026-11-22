package chatwave.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;

/** Màu sắc + font kiểu Fluent (WPF): nền mica dịu, bo góc, màu nhấn xanh ngọc. Có chế độ tối. */
public final class Theme {
    private Theme() {}

    public static boolean dark = false;

    private static Color c(int rgb) { return new Color(rgb); }

    // Màu tạo sẵn một lần (không new Color mỗi lần vẽ)
    private static final Color L_BG = c(0xEDF3F4),     D_BG = c(0x182024);
    private static final Color L_SIDE = c(0xF6FAFA),   D_SIDE = c(0x1E282C);
    private static final Color L_SURFACE = c(0xFFFFFF), D_SURFACE = c(0x222D32);
    private static final Color L_SURFACE2 = c(0xEEF3F4), D_SURFACE2 = c(0x2B383E);
    private static final Color L_HOVER = c(0xE6EEF0),  D_HOVER = c(0x2A373D);
    private static final Color L_BORDER = c(0xDAE4E7), D_BORDER = c(0x35444B);
    private static final Color L_TEXT = c(0x1E2B31),   D_TEXT = c(0xE5EDEF);
    private static final Color L_MUTED = c(0x667A83),  D_MUTED = c(0x93A5AC);
    private static final Color L_ACCENT = c(0x2E9C95), D_ACCENT = c(0x4DB8B0);
    private static final Color L_ACCENT_DARK = c(0x25837D), D_ACCENT_DARK = c(0x3FA39C);
    private static final Color L_ACCENT_SOFT = c(0xD9F0EE), D_ACCENT_SOFT = c(0x274844);
    private static final Color L_ON_ACCENT = Color.WHITE, D_ON_ACCENT = c(0x0F2523);
    private static final Color L_DANGER = c(0xC9566B), D_DANGER = c(0xE07A8D);
    private static final Color L_OK = c(0x3FA66B),     D_OK = c(0x62C48A);
    private static final Color L_WARN = c(0xC98A1B),   D_WARN = c(0xE3B25C);
    private static final Color L_SCRIM = new Color(15, 25, 28, 110), D_SCRIM = new Color(15, 25, 28, 170);
    private static final Color L_SHADOW = new Color(0, 0, 0, 28),    D_SHADOW = new Color(0, 0, 0, 70);

    public static Color bg()         { return dark ? D_BG : L_BG; }
    public static Color side()       { return dark ? D_SIDE : L_SIDE; }
    public static Color surface()    { return dark ? D_SURFACE : L_SURFACE; }
    public static Color surface2()   { return dark ? D_SURFACE2 : L_SURFACE2; }
    public static Color hover()      { return dark ? D_HOVER : L_HOVER; }
    public static Color border()     { return dark ? D_BORDER : L_BORDER; }
    public static Color text()       { return dark ? D_TEXT : L_TEXT; }
    public static Color muted()      { return dark ? D_MUTED : L_MUTED; }
    public static Color accent()     { return dark ? D_ACCENT : L_ACCENT; }
    public static Color accentDark() { return dark ? D_ACCENT_DARK : L_ACCENT_DARK; }
    public static Color accentSoft() { return dark ? D_ACCENT_SOFT : L_ACCENT_SOFT; }
    public static Color onAccent()   { return dark ? D_ON_ACCENT : L_ON_ACCENT; }
    public static Color danger()     { return dark ? D_DANGER : L_DANGER; }
    public static Color ok()         { return dark ? D_OK : L_OK; }
    public static Color warn()       { return dark ? D_WARN : L_WARN; }
    public static Color scrim()      { return dark ? D_SCRIM : L_SCRIM; }
    public static Color shadow()     { return dark ? D_SHADOW : L_SHADOW; }

    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))); }

    private static String family;
    private static final Map<String, Font> CACHE = new HashMap<String, Font>();

    private static String family() {
        if (family == null) {
            family = Font.SANS_SERIF;
            String[] want = {"Segoe UI", "Segoe UI Variable", "Inter", "Noto Sans", "DejaVu Sans"};
            String[] have = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            outer:
            for (String w : want) for (String h : have) if (h.equalsIgnoreCase(w)) { family = h; break outer; }
        }
        return family;
    }

    public static Font font(int style, float size) {
        String k = style + ":" + size;
        Font f = CACHE.get(k);
        if (f == null) { f = new Font(family(), style, 12).deriveFont(size); CACHE.put(k, f); }
        return f;
    }

    public static Font body()  { return font(Font.PLAIN, 14f); }
    public static Font small() { return font(Font.PLAIN, 12f); }
    public static Font bold()  { return font(Font.BOLD, 14f); }

    private static final Color[] AVATAR = {
            c(0x4DB6AC), c(0x7986CB), c(0xF48FB1), c(0xFFB74D), c(0x81C784), c(0x64B5F6), c(0xBA68C8), c(0xE57373)
    };

    /** Màu avatar ổn định theo tên. */
    public static Color avatarColor(String name) {
        int h = 0;
        for (int i = 0; i < name.length(); i++) h = h * 31 + name.charAt(i);
        return AVATAR[Math.floorMod(h, AVATAR.length)];
    }
}