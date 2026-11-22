package chatwave;

import chatwave.core.Session;
import chatwave.screens.*;
import chatwave.ui.*;
import java.awt.*;
import java.util.prefs.Preferences;
import javax.swing.*;

/** Cửa sổ chính: giữ một màn hình tại một thời điểm, chuyển có hiệu ứng mờ dần. */
public class AppFrame extends JFrame implements Nav {
    private final Overlay overlay = new Overlay();
    private final JPanel host = new JPanel(new BorderLayout());
    private final Preferences prefs = Preferences.userNodeForPackage(AppFrame.class);
    private JComponent current;
    private MainPanel main;

    public AppFrame() {
        super("ChatWave");
        Theme.dark = prefs.getBoolean("dark", false);
        setIconImage(BrandLogo.image(64));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        host.setOpaque(false);
        setContentPane(new Bg(Bg.Kind.BG, new BorderLayout()));
        getContentPane().add(host, BorderLayout.CENTER);
        overlay.install(getRootPane());
        setMinimumSize(new Dimension(900, 600));
        setSize(1180, 740);
        setLocationRelativeTo(null);
        showAuth();
    }

    public Overlay overlay() { return overlay; }

    private void swap(JComponent c) {
        if (main != null) { main.stop(); main = null; }
        host.removeAll();
        FadePanel fp = new FadePanel(new BorderLayout());
        fp.add(c, BorderLayout.CENTER);
        host.add(fp, BorderLayout.CENTER);
        current = c;
        host.revalidate();
        host.repaint();
        fp.playIn(320, 14);
    }

    public void showAuth() {
        Session.account = null;
        AuthPanel a = new AuthPanel(this);
        swap(a);
        a.focusFirst();
    }

    public void showMain() {
        main = new MainPanel(this);
        JComponent c = main;
        host.removeAll();
        FadePanel fp = new FadePanel(new BorderLayout());
        fp.add(c, BorderLayout.CENTER);
        host.add(fp, BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
        fp.playIn(320, 14);
        main.start();
    }

    public void showAdmin() { swap(new AdminPanel(this)); }

    public void toggleTheme() {
        Theme.dark = !Theme.dark;
        prefs.putBoolean("dark", Theme.dark);
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }
}
