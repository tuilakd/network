package chatwave.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.sound.sampled.*;
import javax.swing.Timer;

/** Ghi âm (ra tệp WAV) và phát âm thanh bằng javax.sound, không cần thư viện ngoài. */
public final class Audio {
    private Audio() {}

    /** 16 kHz, 16 bit, mono: ~32 KB/giây, 4 phút rưỡi vẫn dưới 10 MB. */
    private static final AudioFormat FMT = new AudioFormat(16000f, 16, 1, true, false);
    public static final int MAX_SECONDS = 270;

    public static final class Recorder {
        private TargetDataLine line;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private volatile boolean running;
        private Thread thread;
        private long started;

        public void start() throws Exception {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FMT);
            if (!AudioSystem.isLineSupported(info)) throw new LineUnavailableException("Không tìm thấy micro");
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FMT);
            line.start();
            running = true;
            started = System.currentTimeMillis();
            thread = new Thread(new Runnable() {
                public void run() {
                    byte[] b = new byte[4096];
                    while (running) {
                        int n = line.read(b, 0, b.length);
                        if (n > 0) buf.write(b, 0, n);
                        if (buf.size() >= MAX_SECONDS * 32000) running = false;
                    }
                }
            }, "recorder");
            thread.setDaemon(true);
            thread.start();
        }

        public boolean isRunning() { return running; }
        public int seconds() { return (int) ((System.currentTimeMillis() - started) / 1000); }

        /** Dừng và lưu thành tệp WAV tạm. Trả về null nếu quá ngắn. */
        public File stop() throws Exception {
            running = false;
            if (thread != null) thread.join(500);
            if (line != null) { line.stop(); line.close(); }
            byte[] data = buf.toByteArray();
            if (data.length < 8000) return null; // dưới ~0,25 giây
            File f = File.createTempFile("ghiam-", ".wav");
            f.deleteOnExit();
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), FMT, data.length / FMT.getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, f);
            return f;
        }

        public void cancel() {
            running = false;
            if (line != null) { line.stop(); line.close(); }
        }
    }

    public interface Progress { void update(float p, boolean done); }

    public static final class Player {
        private Clip clip;
        private Timer timer;
        private Progress cb;

        /** Ném UnsupportedAudioFileException nếu định dạng không phát được (ví dụ mp3). */
        public void play(byte[] data, Progress progress) throws Exception {
            stop();
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            clip = AudioSystem.getClip();
            clip.open(ais);
            cb = progress;
            clip.start();
            timer = new Timer(60, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (clip == null) return;
                    long len = Math.max(1, clip.getMicrosecondLength());
                    float p = Math.min(1f, clip.getMicrosecondPosition() / (float) len);
                    boolean done = !clip.isRunning() && p >= 0.98f;
                    if (cb != null) cb.update(done ? 0f : p, done);
                    if (done) release();
                }
            });
            timer.start();
        }

        public void stop() {
            Progress c = cb;
            release();
            if (c != null) c.update(0f, true);
        }

        private void release() {
            if (timer != null) timer.stop();
            timer = null;
            if (clip != null) { clip.stop(); clip.close(); }
            clip = null;
            cb = null;
        }
    }
}
