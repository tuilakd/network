package chatwave.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Biểu tượng vẽ bằng Java2D (không cần ảnh, luôn nét ở mọi độ phân giải). */
public enum Icons {
    SEND, IMAGE, SEARCH, CLOSE, MOON, SUN, INFO, PLUS, CHECK, LOGOUT, BACK, USER, CHAT, GROUP, TRASH, SHIELD, MAIL, LOCK, EYE, SETTINGS, SHIELDUSER, MIC, PLAY, PAUSE, STOP, FILM, SMILE, USER_PLUS, USERS_PLUS, FORWARD, COPY;

    public void paint(Graphics2D g0, float x, float y, float s, Color col) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.scale(s / 24f, s / 24f);
        g.setColor(col);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (this) {
            case SEND: {
                Path2D p = new Path2D.Float();
                p.moveTo(3, 11); p.lineTo(21, 3); p.lineTo(13, 21); p.lineTo(11, 13); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Float(21, 3, 11, 13));
                break;
            }
            case IMAGE:
                g.draw(new RoundRectangle2D.Float(3, 4, 18, 16, 4, 4));
                g.fill(new Ellipse2D.Float(7, 8, 3.2f, 3.2f));
                Path2D m = new Path2D.Float();
                m.moveTo(4, 18); m.lineTo(10, 12); m.lineTo(14, 16); m.lineTo(17, 13); m.lineTo(21, 17);
                g.draw(m);
                break;
            case SEARCH:
                g.draw(new Ellipse2D.Float(4, 4, 12, 12));
                g.draw(new Line2D.Float(14, 14, 20, 20));
                break;
            case CLOSE:
                g.draw(new Line2D.Float(6, 6, 18, 18));
                g.draw(new Line2D.Float(18, 6, 6, 18));
                break;
            case MOON: {
                java.awt.geom.Area a = new java.awt.geom.Area(new Ellipse2D.Float(4, 3, 17, 17));
                a.subtract(new java.awt.geom.Area(new Ellipse2D.Float(10, 0, 14, 14)));
                g.fill(a);
                break;
            }
            case SUN:
                g.draw(new Ellipse2D.Float(7, 7, 10, 10));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(12 + Math.cos(a) * 7.5, 12 + Math.sin(a) * 7.5, 12 + Math.cos(a) * 10, 12 + Math.sin(a) * 10));
                }
                break;
            case INFO:
                g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                g.draw(new Line2D.Float(12, 11, 12, 17));
                g.fill(new Ellipse2D.Float(11, 6.3f, 2, 2));
                break;
            case PLUS:
                g.draw(new Line2D.Float(12, 5, 12, 19));
                g.draw(new Line2D.Float(5, 12, 19, 12));
                break;
            case CHECK: {
                Path2D p = new Path2D.Float();
                p.moveTo(5, 13); p.lineTo(10, 18); p.lineTo(19, 7);
                g.draw(p);
                break;
            }
            case LOGOUT: {
                Path2D p = new Path2D.Float();
                p.moveTo(10, 4); p.lineTo(5, 4); p.lineTo(5, 20); p.lineTo(10, 20);
                g.draw(p);
                g.draw(new Line2D.Float(10, 12, 20, 12));
                Path2D h = new Path2D.Float();
                h.moveTo(16, 8); h.lineTo(20, 12); h.lineTo(16, 16);
                g.draw(h);
                break;
            }
            case BACK: {
                Path2D p = new Path2D.Float();
                p.moveTo(14, 5); p.lineTo(7, 12); p.lineTo(14, 19);
                g.draw(p);
                break;
            }
            case USER:
                g.draw(new Ellipse2D.Float(8, 3.5f, 8, 8));
                g.draw(new Arc2D.Float(4, 14, 16, 14, 0, 180, Arc2D.OPEN));
                break;
            case SHIELDUSER:
            case SHIELD: {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 3); p.lineTo(20, 6); p.lineTo(20, 12);
                p.curveTo(20, 17, 16, 20, 12, 21);
                p.curveTo(8, 20, 4, 17, 4, 12);
                p.lineTo(4, 6); p.closePath();
                g.draw(p);
                if (this == SHIELD) {
                    Path2D c = new Path2D.Float();
                    c.moveTo(8.5f, 12); c.lineTo(11, 14.5f); c.lineTo(15.5f, 9.5f);
                    g.draw(c);
                }
                break;
            }
            case CHAT: {
                Path2D p = new Path2D.Float();
                p.moveTo(7, 4); p.lineTo(17, 4);
                p.curveTo(19.5f, 4, 21, 5.5f, 21, 8); p.lineTo(21, 13);
                p.curveTo(21, 15.5f, 19.5f, 17, 17, 17); p.lineTo(11, 17); p.lineTo(6.5f, 21); p.lineTo(7, 17);
                p.curveTo(4.5f, 17, 3, 15.5f, 3, 13); p.lineTo(3, 8);
                p.curveTo(3, 5.5f, 4.5f, 4, 7, 4); p.closePath();
                g.draw(p);
                g.fill(new Ellipse2D.Float(7.2f, 9.5f, 2, 2));
                g.fill(new Ellipse2D.Float(11, 9.5f, 2, 2));
                g.fill(new Ellipse2D.Float(14.8f, 9.5f, 2, 2));
                break;
            }
            case GROUP:
                g.draw(new Ellipse2D.Float(4, 6, 7, 7));
                g.draw(new Ellipse2D.Float(13, 6, 7, 7));
                g.draw(new Arc2D.Float(1, 15, 13, 10, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Float(10, 15, 13, 10, 0, 180, Arc2D.OPEN));
                break;
            case TRASH: {
                g.draw(new Line2D.Float(4, 7, 20, 7));
                g.draw(new Line2D.Float(9, 7, 9, 4)); g.draw(new Line2D.Float(9, 4, 15, 4)); g.draw(new Line2D.Float(15, 4, 15, 7));
                Path2D p = new Path2D.Float();
                p.moveTo(6, 7); p.lineTo(7, 20); p.lineTo(17, 20); p.lineTo(18, 7);
                g.draw(p);
                break;
            }
            case MAIL:
                g.draw(new RoundRectangle2D.Float(3, 5, 18, 14, 4, 4));
                Path2D p2 = new Path2D.Float();
                p2.moveTo(4, 7); p2.lineTo(12, 13); p2.lineTo(20, 7);
                g.draw(p2);
                break;
            case LOCK:
                g.draw(new RoundRectangle2D.Float(5, 11, 14, 9, 4, 4));
                g.draw(new Arc2D.Float(8, 3.5f, 8, 11, 0, 180, Arc2D.OPEN));
                break;
            case EYE: {
                Path2D p = new Path2D.Float();
                p.moveTo(2, 12); p.curveTo(6, 5, 18, 5, 22, 12); p.curveTo(18, 19, 6, 19, 2, 12);
                g.draw(p);
                g.draw(new Ellipse2D.Float(9, 9, 6, 6));
                break;
            }
            case SETTINGS:
                g.draw(new Ellipse2D.Float(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(12 + Math.cos(a) * 8, 12 + Math.sin(a) * 8, 12 + Math.cos(a) * 10, 12 + Math.sin(a) * 10));
                }
                break;
            case MIC:
                g.draw(new RoundRectangle2D.Float(9, 3, 6, 11, 6, 6));
                g.draw(new Arc2D.Float(5, 7, 14, 11, 180, 180, Arc2D.OPEN));
                g.draw(new Line2D.Float(12, 18, 12, 21));
                g.draw(new Line2D.Float(8.5f, 21, 15.5f, 21));
                break;
            case PLAY: {
                Path2D p = new Path2D.Float();
                p.moveTo(7, 4.5f); p.lineTo(19, 12); p.lineTo(7, 19.5f); p.closePath();
                g.fill(p);
                break;
            }
            case PAUSE:
                g.fill(new RoundRectangle2D.Float(6, 5, 4.2f, 14, 2, 2));
                g.fill(new RoundRectangle2D.Float(13.8f, 5, 4.2f, 14, 2, 2));
                break;
            case STOP:
                g.fill(new RoundRectangle2D.Float(6, 6, 12, 12, 3, 3));
                break;
            case FILM:
                g.draw(new RoundRectangle2D.Float(3, 5, 18, 14, 4, 4));
                Path2D f = new Path2D.Float();
                f.moveTo(10, 9); f.lineTo(15, 12); f.lineTo(10, 15); f.closePath();
                g.fill(f);
                break;
            case SMILE:
                g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                g.fill(new Ellipse2D.Float(8, 8.5f, 2.2f, 2.2f));
                g.fill(new Ellipse2D.Float(13.8f, 8.5f, 2.2f, 2.2f));
                g.draw(new Arc2D.Float(7, 9, 10, 8, 200, 140, Arc2D.OPEN));
                break;
            case USER_PLUS:
                g.draw(new Ellipse2D.Float(6, 4, 7, 7));
                g.draw(new Arc2D.Float(2.5f, 13.5f, 14, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Float(18.5f, 8, 18.5f, 14));
                g.draw(new Line2D.Float(15.5f, 11, 21.5f, 11));
                break;
            case USERS_PLUS:
                g.draw(new Ellipse2D.Float(5, 4.5f, 6.5f, 6.5f));
                g.draw(new Arc2D.Float(2, 13.5f, 12.5f, 11, 0, 180, Arc2D.OPEN));
                g.draw(new Ellipse2D.Float(13, 5.5f, 5, 5));
                g.draw(new Line2D.Float(19, 12.5f, 19, 18.5f));
                g.draw(new Line2D.Float(16, 15.5f, 22, 15.5f));
                break;
            case FORWARD: {
                Path2D p = new Path2D.Float();
                p.moveTo(14, 5); p.lineTo(20, 10.5f); p.lineTo(14, 16);
                g.draw(p);
                g.draw(new Arc2D.Float(4, 10.5f, 16, 13, 180, -90, Arc2D.OPEN));
                break;
            }
            case COPY:
                g.draw(new RoundRectangle2D.Float(8, 8, 12, 12, 3, 3));
                g.draw(new RoundRectangle2D.Float(4, 4, 12, 12, 3, 3));
                break;
            default:
        }
        g.dispose();
    }
}
