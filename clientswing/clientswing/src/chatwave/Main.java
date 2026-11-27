package chatwave;

import javax.swing.SwingUtilities;

/** Điểm khởi chạy: chọn chatwave.Main làm Main Class trong NetBeans. */
public class Main {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new chatwave.AppFrame().setVisible(true));
    }
}
