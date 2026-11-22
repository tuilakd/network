package chatwave.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

/** Bộ hiệu ứng: một Timer dùng chung chạy các "tween" (đổi giá trị mượt theo thời gian). */
public final class Anim {
    private Anim() {}

    public interface Update { void apply(float t); }

    public static final class Tween {
        final long start = System.nanoTime();
        final int ms;
        final Update update;
        final Runnable done;
        final boolean ease;
        boolean cancelled;
        Tween(int ms, Update u, Runnable d, boolean ease) { this.ms = ms; this.update = u; this.done = d; this.ease = ease; }
        public void cancel() { cancelled = true; }
    }

    private static final List<Tween> ACTIVE = new ArrayList<Tween>();
    private static Timer timer;

    public static float easeOut(float t) { float u = 1 - t; return 1 - u * u * u; }
    public static float easeInOut(float t) { return t < .5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }

    public static Tween run(int ms, Update u) { return run(ms, u, null); }

    public static Tween run(int ms, Update u, Runnable done) {
        Tween t = new Tween(ms, u, done, true);
        ACTIVE.add(t);
        ensure();
        u.apply(0f);
        return t;
    }

    private static void ensure() {
        if (timer != null) return;
        timer = new Timer(15, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { tick(); }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    private static void tick() {
        long now = System.nanoTime();
        List<Tween> copy = new ArrayList<Tween>(ACTIVE);
        for (Tween t : copy) {
            if (t.cancelled) { ACTIVE.remove(t); continue; }
            float p = Math.min(1f, (now - t.start) / (t.ms * 1_000_000f));
            t.update.apply(t.ease ? easeOut(p) : p);
            if (p >= 1f) {
                ACTIVE.remove(t);
                if (t.done != null) t.done.run();
            }
        }
        if (ACTIVE.isEmpty()) { timer.stop(); timer = null; }
    }

    /** Giá trị 0..1 chuyển mượt tới đích (dùng cho hover, focus...). */
    public static final class Fader {
        public float v;
        private Tween cur;
        private final Component owner;
        public Fader(Component owner) { this.owner = owner; }

        public void to(final float target, int ms) {
            if (cur != null) cur.cancel();
            final float from = v;
            if (from == target) return;
            cur = run(ms, new Update() {
                public void apply(float t) {
                    v = from + (target - from) * t;
                    if (owner != null) owner.repaint();
                }
            });
        }
    }
}
