package chatwave.screens;

import chatwave.core.*;
import chatwave.ui.*;
import java.awt.*;
import javax.swing.*;

/** Hộp thoại quên mật khẩu: nhập email -> nhập mã + mật khẩu mới. */
public final class ForgotDialog {
    private ForgotDialog() {}

    public static void show(final Overlay ov, final Backend b, final boolean demo, final String presetEmail) {
        final Card card = new Card(26);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        step1(ov, b, demo, card, presetEmail);
        ov.show(card);
    }

    private static JComponent fix(JComponent c, int w, int h) {
        c.setPreferredSize(new Dimension(w, h));
        c.setMaximumSize(new Dimension(w, h));
        c.setAlignmentX(0f);
        return c;
    }

    private static void reset(Card card) { card.removeAll(); }

    private static void step1(final Overlay ov, final Backend b, final boolean demo, final Card card, String preset) {
        reset(card);
        Dialogs.Label t = new Dialogs.Label("Quên mật khẩu", Theme.font(Font.BOLD, 20f), false, false, 340);
        Dialogs.Label d = new Dialogs.Label("Nhập email đã đăng ký, chúng tôi sẽ gửi mã xác nhận 6 số để bạn đặt lại mật khẩu.", Theme.font(Font.PLAIN, 14f), true, false, 340);
        t.setAlignmentX(0f); d.setAlignmentX(0f);
        card.add(t); card.add(Box.createVerticalStrut(6)); card.add(d); card.add(Box.createVerticalStrut(18));
        final FField email = new FField("Email", Icons.MAIL, false);
        if (preset != null && preset.contains("@")) email.setText(preset);
        card.add(fix(email, 340, 46));
        card.add(Box.createVerticalStrut(8));
        final JLabel err = errLabel();
        card.add(err);
        card.add(Box.createVerticalStrut(8));
        final FButton send = new FButton("Gửi mã xác nhận", Icons.MAIL, FButton.Kind.PRIMARY, null);
        final Runnable go = new Runnable() {
            public void run() {
                final String em = email.getText().trim();
                if (!em.contains("@") || em.length() < 5) { email.flagError(); err.setText("Email chưa hợp lệ"); return; }
                err.setText(" ");
                send.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Boolean>() {
                    public Boolean call() throws Exception { b.forgotPassword(em); return true; }
                }, new Async.Ok<Boolean>() {
                    public void accept(Boolean ok) { send.setLoading(false); step2(ov, b, demo, card, em); }
                }, new Async.Fail() {
                    public void accept(Exception e) { send.setLoading(false); err.setText(msg(e)); card.revalidate(); }
                });
            }
        };
        send.setAction(go);
        email.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { go.run(); } });
        card.add(fix(send, 340, 44));
        card.add(Box.createVerticalStrut(8));
        card.add(fix(new FButton("Quay lại đăng nhập", FButton.Kind.GHOST, new Runnable() { public void run() { ov.close(); } }), 340, 40));
        refresh(card, email);
    }

    private static void step2(final Overlay ov, final Backend b, final boolean demo, final Card card, final String em) {
        reset(card);
        Dialogs.Label t = new Dialogs.Label("Nhập mã xác nhận", Theme.font(Font.BOLD, 20f), false, false, 340);
        Dialogs.Label d = new Dialogs.Label("Mã đã gửi tới " + em + ". Mã có hiệu lực trong thời gian ngắn." + (demo ? " (Chế độ demo: mã là 123456)" : ""), Theme.font(Font.PLAIN, 14f), true, false, 340);
        t.setAlignmentX(0f); d.setAlignmentX(0f);
        card.add(t); card.add(Box.createVerticalStrut(6)); card.add(d); card.add(Box.createVerticalStrut(18));
        final FField code = new FField("Mã 6 số", Icons.SHIELD, false);
        final FField p1 = new FField("Mật khẩu mới (tối thiểu 6 ký tự)", Icons.LOCK, true);
        final FField p2 = new FField("Nhập lại mật khẩu mới", Icons.LOCK, true);
        for (FField f : new FField[]{code, p1, p2}) { card.add(fix(f, 340, 46)); card.add(Box.createVerticalStrut(10)); }
        final JLabel err = errLabel();
        card.add(err);
        card.add(Box.createVerticalStrut(8));
        final FButton ok = new FButton("Đặt lại mật khẩu", Icons.CHECK, FButton.Kind.PRIMARY, null);
        final Runnable go = new Runnable() {
            public void run() {
                final String c = code.getText().trim(), a = p1.getText();
                if (c.isEmpty()) { code.flagError(); err.setText("Nhập mã xác nhận"); return; }
                if (a.length() < 6) { p1.flagError(); err.setText("Mật khẩu mới tối thiểu 6 ký tự"); return; }
                if (!a.equals(p2.getText())) { p2.flagError(); err.setText("Mật khẩu nhập lại không khớp"); return; }
                err.setText(" ");
                ok.setLoading(true);
                Async.run(new java.util.concurrent.Callable<Boolean>() {
                    public Boolean call() throws Exception { b.resetPassword(em, c, a); return true; }
                }, new Async.Ok<Boolean>() {
                    public void accept(Boolean r) { ok.setLoading(false); ov.close(); ov.toast("Đã đặt lại mật khẩu, hãy đăng nhập lại", false); }
                }, new Async.Fail() {
                    public void accept(Exception e) { ok.setLoading(false); err.setText(msg(e)); card.revalidate(); }
                });
            }
        };
        ok.setAction(go);
        p2.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { go.run(); } });
        card.add(fix(ok, 340, 44));
        card.add(Box.createVerticalStrut(8));
        card.add(fix(new FButton("Gửi lại mã / đổi email", FButton.Kind.GHOST, new Runnable() { public void run() { step1(ov, b, demo, card, em); } }), 340, 40));
        refresh(card, code);
    }

    private static JLabel errLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.font(Font.PLAIN, 13f));
        l.setForeground(Theme.danger());
        l.setAlignmentX(0f);
        return l;
    }

    private static String msg(Exception e) { return e.getMessage() == null ? "Có lỗi xảy ra" : e.getMessage(); }

    private static void refresh(final Card card, final FField focus) {
        card.revalidate();
        card.repaint();
        Container p = card.getParent();
        if (p != null) { p.revalidate(); p.repaint(); }
        SwingUtilities.invokeLater(new Runnable() { public void run() { focus.requestFocus(); } });
    }
}
