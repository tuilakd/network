package chatwave.ui;

import chatwave.core.Async;
import chatwave.core.Session;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;

/** Tải ảnh đính kèm (có kèm JWT) ở luồng nền và nhớ lại để không tải lại. */
public final class ImageCache {
    private ImageCache() {}

    private static final Map<String, BufferedImage> FULL = new HashMap<String, BufferedImage>();
    private static final Map<String, BufferedImage> THUMB = new HashMap<String, BufferedImage>();
    private static final Set<String> LOADING = new HashSet<String>();
    private static final Set<String> FAILED = new HashSet<String>();

    public static boolean failed(String url) { return FAILED.contains(url); }

    /** Trả về ảnh gốc nếu đã có; chưa có thì bắt đầu tải và gọi onLoaded khi xong. */
    public static BufferedImage full(final String url, final Runnable onLoaded) {
        BufferedImage im = FULL.get(url);
        if (im != null || FAILED.contains(url) || LOADING.contains(url)) return im;
        LOADING.add(url);
        Async.run(new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                byte[] b = Session.backend.download(url);
                BufferedImage r = ImageIO.read(new ByteArrayInputStream(b));
                if (r == null) throw new Exception("Không đọc được ảnh");
                return r;
            }
        }, new Async.Ok<BufferedImage>() {
            public void accept(BufferedImage v) { LOADING.remove(url); FULL.put(url, v); if (onLoaded != null) onLoaded.run(); }
        }, new Async.Fail() {
            public void accept(Exception e) { LOADING.remove(url); FAILED.add(url); if (onLoaded != null) onLoaded.run(); }
        });
        return null;
    }

    /** Ảnh thu nhỏ vừa khung maxW x maxH (null nếu chưa tải xong). */
    public static BufferedImage thumb(String url, int maxW, int maxH, Runnable onLoaded) {
        String k = url + "@" + maxW + "x" + maxH;
        BufferedImage t = THUMB.get(k);
        if (t != null) return t;
        BufferedImage f = full(url, onLoaded);
        if (f == null) return null;
        t = fit(f, maxW, maxH);
        THUMB.put(k, t);
        return t;
    }

    public static BufferedImage fit(BufferedImage src, int maxW, int maxH) {
        double s = Math.min(1.0, Math.min(maxW / (double) src.getWidth(), maxH / (double) src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * s)), h = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage cur = src;
        // thu nhỏ từng nửa để ảnh mịn
        while (cur.getWidth() / 2 > w && cur.getHeight() / 2 > h) cur = scale(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        return scale(cur, w, h);
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return o;
    }
}
