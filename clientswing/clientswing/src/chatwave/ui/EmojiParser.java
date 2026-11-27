package chatwave.ui;

import java.util.ArrayList;
import java.util.List;

/** Tách chuỗi tin nhắn thành các đoạn chữ thường và đoạn emoji (khớp dài nhất, hỗ trợ surrogate, ZWJ, FE0F, skin-tone). */
public final class EmojiParser {
    public static final class Segment {
        public final boolean emoji;
        public final String text;
        public final String hex;

        public Segment(boolean emoji, String text, String hex) {
            this.emoji = emoji;
            this.text = text;
            this.hex = hex;
        }
    }

    private EmojiParser() {}

    public static List<Segment> parse(String input) {
        List<Segment> out = new ArrayList<Segment>();
        if (input == null || input.isEmpty()) return out;

        StringBuilder buf = new StringBuilder();
        int i = 0;
        int len = input.length();
        while (i < len) {
            Match m = matchAt(input, i);
            if (m != null) {
                if (buf.length() > 0) {
                    out.add(new Segment(false, buf.toString(), null));
                    buf.setLength(0);
                }
                out.add(new Segment(true, m.unicode, m.hex));
                i = m.nextIndex;
            } else {
                int cp = input.codePointAt(i);
                buf.appendCodePoint(cp);
                i += Character.charCount(cp);
            }
        }
        if (buf.length() > 0) out.add(new Segment(false, buf.toString(), null));
        return out;
    }

    private static final class Match {
        final String unicode;
        final String hex;
        final int nextIndex;
        Match(String unicode, String hex, int nextIndex) {
            this.unicode = unicode; this.hex = hex; this.nextIndex = nextIndex;
        }
    }

    private static boolean isModifierOrSelector(int cp) {
        return cp == 0xFE0F || cp == 0xFE0E || (cp >= 0x1F3FB && cp <= 0x1F3FF);
    }

    private static Match matchAt(String s, int start) {
        int maxEnd = Math.min(s.length(), start + Math.max(8, EmojiRegistry.maxUnicodeLength() + 6));
        for (int end = maxEnd; end > start; ) {
            String sub = s.substring(start, end);
            String hex = EmojiRegistry.findHex(sub);
            if (hex != null) return new Match(sub, hex, end);

            // Thử bỏ FE0F / skin-tone modifiers nếu không khớp chính xác
            String stripped = stripModifiers(sub);
            if (!stripped.isEmpty() && !stripped.equals(sub)) {
                hex = EmojiRegistry.findHex(stripped);
                if (hex != null) return new Match(sub, hex, end);
            }
            int prevCp = s.codePointBefore(end);
            end -= Character.charCount(prevCp);
        }
        return null;
    }

    private static String stripModifiers(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (!isModifierOrSelector(cp) && cp != 0x200D) sb.appendCodePoint(cp);
        }
        return sb.toString();
    }
}
