package chatwave.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.SwingUtilities;

/** Chạy việc gọi mạng ở luồng nền rồi trả kết quả về luồng giao diện (EDT). */
public final class Async {
    private Async() {}

    public interface Ok<T> { void accept(T value); }
    public interface Fail { void accept(Exception e); }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "chatwave-net");
            t.setDaemon(true);
            return t;
        }
    });

    public static <T> void run(final Callable<T> job, final Ok<T> ok, final Fail fail) {
        POOL.submit(new Runnable() {
            public void run() {
                try {
                    final T v = job.call();
                    SwingUtilities.invokeLater(new Runnable() { public void run() { if (ok != null) ok.accept(v); } });
                } catch (final Exception e) {
                    SwingUtilities.invokeLater(new Runnable() { public void run() { if (fail != null) fail.accept(e); } });
                }
            }
        });
    }

    public static <T> void run(Callable<T> job, Ok<T> ok) { run(job, ok, null); }
}
